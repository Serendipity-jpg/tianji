package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.enums.ExchangeCodeType;
import com.tianji.promotion.mapper.ExchangeCodeMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.utils.CodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.tianji.promotion.constants.PromotionConstants.COUPON_RANGE_KEY;

/**
 * <p>
 * 兑换码 服务实现类
 * </p>
 *
 * @author Sakura
 */
@Service
@RequiredArgsConstructor
public class ExchangeCodeServiceImpl extends ServiceImpl<ExchangeCodeMapper, ExchangeCode> implements IExchangeCodeService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 根据兑换码id查询优惠券id
     *
     * @param serialNum 兑换码id
     * @return 优惠券id
     */
    @Override
    public Long getExchangeTargetId(Long serialNum) {
        // 查询score值比当前序列号大的第一个优惠券
        Set<String> results = redisTemplate.opsForZSet().rangeByScore(
                COUPON_RANGE_KEY, serialNum, serialNum + 5000, 0L, 1L);
        if (CollUtils.isEmpty(results)) {
            return null;
        }
        // 数据转换
        String next = results.iterator().next();
        return Long.parseLong(next);
    }

    /**
     * 判断兑换是否已兑换，并根据自增id 进行更新
     *
     * @param serialNum 自增id，偏移量
     * @param flag      更新值
     */
    @Override
    public boolean hasExchanged(long serialNum, boolean flag) {
        String key = PromotionConstants.COUPON_CODE_MAP_KEY;
        // bitmap结构，返回值为更新前的值，被Boolean接收
        Boolean res = redisTemplate.opsForValue().setBit(key, serialNum, flag);
        return res != null && res;
    }

    /**
     * 异步生成兑换码
     * 使用Async注解实现并注明线程池id
     *
     * @param coupon 优惠券信息
     */
    @Override
    @Async("generateExchangeCodeExecutor")
    public void asyncGenerateExchangeCode(Coupon coupon) {
        Integer totalNum = coupon.getTotalNum();    // 生成的兑换码的数量
        // 生成自增id，借助于redis incr命令
        Long increment = redisTemplate.opsForValue().increment(PromotionConstants.COUPON_CODE_SERIAL_KEY, totalNum);
        if (increment == null) {
            return;
        }
        int endSerialNum = increment.intValue();    // 本地自增id的结束值
        int startSerialNum = endSerialNum - totalNum + 1;   // // 本地自增id的开始值

        List<ExchangeCode> codeList = new ArrayList<>();
        // 调用工具类，循环生成兑换码
        for (int i = startSerialNum; i <= endSerialNum; i++) {
            // 生成兑换码
            String code = CodeUtil.generateCode(i, coupon.getId());
            ExchangeCode exchangeCode = ExchangeCode.builder()
                    .id(i)  // 主键id，因此po类的主键生成策略需要修改为INPUT
                    .code(code)     // 兑换码
                    .exchangeTargetId(coupon.getId())   // 目标业务id，这里填优惠券id
                    .expiredTime(coupon.getIssueEndTime())  // 兑换有效时间是优惠券领取结束时间
                    .status(ExchangeCodeStatus.UNUSED)  // 状态为未兑换
                    .type(ExchangeCodeType.COUPON)  // 兑换码类别为优惠券
                    .build();
            codeList.add(exchangeCode);
        }
        // 将兑换码保存到exchange_code表，批量保存
        this.saveBatch(codeList);
        // 写入Redis缓存，member：couponId，score：兑换码的最大序列号
        redisTemplate.opsForZSet().add(COUPON_RANGE_KEY, coupon.getId().toString(), endSerialNum);
    }
}
