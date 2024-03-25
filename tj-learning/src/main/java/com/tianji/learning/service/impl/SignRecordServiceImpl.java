package com.tianji.learning.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.tianji.api.constants.RedisConstants;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.mq.SignInMessage;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 *
 * @author Sakura
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl implements ISignRecordService {

    private final StringRedisTemplate redisTemplate;
    private final RabbitMqHelper rabbitMqHelper;

    /**
     * 学生签到
     */
    @Override
    public SignResultVO addSignRecords() {
        // 获取当前登录用户
        Long userId = UserContext.getUser();
        // 拼接key
        LocalDate now = LocalDate.now();
        int dayOfMonth = now.getDayOfMonth();   // 获取当前日是这个月的多少号
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
        String yearMonth = formatter.format(now);   // 获取年月，如202403
        String key = RedisConstants.SIGN_RECORD_KEY_PREFIX + userId.toString() + ":" + yearMonth;
        // 利用bitset，将签到记录保存到redis的bitmap结构中，需要校验是否已签到
        int offset = dayOfMonth - 1;    // 偏移量，因为下标从0开始所以减1
        // 校验是否已签到
        Boolean exists = redisTemplate.opsForValue().setBit(key, offset, true);
        if (exists) {
            throw new BadRequestException("不能重复签到");
        }
        // 计算连续签到的天数，以此计算是否有连续签到7天的额外奖励积分
        int signDays = countSignDays(key, dayOfMonth);
        // 计算连续签到的奖励积分，规则是连续签到7天加10积分，连续签到14天加20积分，连续签到28天加40积分
        int rewardPoints = 0;
        switch (signDays) {
            case 7:
                rewardPoints = 10;
                break;
            case 14:
                rewardPoints = 20;
                break;
            case 28:
                rewardPoints = 40;
                break;
        }

        // 发送RabbitMQ消息，保存积分
        rabbitMqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.SIGN_IN,
                // 积分数量 = 签到积分1+额外签到奖励积分
                SignInMessage.of(userId, rewardPoints + 1));
        // 封装结果并返回
        SignResultVO resultVO = SignResultVO.builder()
                .signDays(signDays)
                .signPoints(1)   // 签到分数，默认为1，不需要重复赋值
                .rewardPoints(rewardPoints)
                .build();

        return resultVO;
    }


    /**
     * 计算本月已连续签到的天数
     *
     * @param key        redis相关的key
     * @param dayOfMonth 本月当前天数
     * @return 本月已连续签到的天数
     */
    private int countSignDays(String key, int dayOfMonth) {
        // 获取本月直到当前天的签到数据，结果为十进制，返回结果为list，在第0个元素
        // 相当于 bitfield key get u本月当前天数 0
        List<Long> signList = redisTemplate.opsForValue().bitField(key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                        .valueAt(0));
        // 判空
        if (CollUtil.isEmpty(signList)) {
            return 0;
        }
        int signDays = 0; // 计数器，统计连续签到天数
        Long num = signList.get(0);
        log.debug("num {}", num);
        // num转二进制，累加统计连续签到天数
        // 通过与1进行&运算，获取当前签到数据最后一位
        while ((num & 1) == 1) {
            signDays++;
            // 注：>>和>>>分别表示带符号右移和无符号右移，这里去的是无符号数，所以用>>>
            num = num >>> 1;    // 右移1位，更新num，
        }
        return signDays;
    }


    /**
     * 查询用户本月签到记录
     *
     * @return 用户本月签到记录
     */
    @Override
    public List<Byte> selectMonthSignRecords() {
        // 获取当前用户
        Long userId = UserContext.getUser();
        // 拼接key
        LocalDateTime now = LocalDateTime.now();
        int dayOfMonth = now.getDayOfMonth();   // 当前为本月第几天
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(":yyyyMM");
        String yearMonth = formatter.format(now);    // 年月
        String key = RedisConstants.SIGN_RECORD_KEY_PREFIX + userId.toString() + yearMonth;
        // 从redis的bitMap中取出本月到当前天的记录
        List<Long> field = redisTemplate.opsForValue().bitField(key,
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));
        if (CollUtil.isEmpty(field)){   // 判空
            throw new BizIllegalException("查询异常");
        }
        // 返回结果
        List<Byte> res = new ArrayList<>();
        Long num = field.get(0);
        while (num >= 0) {
            if (res.size() == dayOfMonth){
                break;
            }
            res.add(0, (byte) (num & 1));   // 尾插法解决顺序问题
            num = num>>>1;  // num为无符号整数，所以用无符号右移
        }
        return res;
    }


}
