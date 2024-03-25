package com.tianji.remark.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.api.constants.RedisConstants;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <p>
 * 点赞记录表 服务实现类
 * </p>
 *
 * @author Sakura
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {


    private final RabbitMqHelper rabbitMqHelper;

    private final StringRedisTemplate redisTemplate;

    /**
     * 点赞或者取消赞
     */
    @Override
    public void addLikeRecrod(LikeRecordFormDTO dto) {
        // 获取登录用户
        Long userId = UserContext.getUser();
        // 判断是否点赞、取消赞是否成功，根据liked属性判断是点赞还是取消赞
        boolean flag = dto.getLiked() ? liked(dto, userId) : unliked(dto, userId);
        if (!flag) { // 如果点赞或者取消赞失败
            return;
        }
        // 统计该业务id的总点赞数 - 基于redis实现
        String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + dto.getBizId();
        Long likedTimes = redisTemplate.opsForSet().size(key);
        if (likedTimes == null) {
            return;
        }
        String bizTypeKey = RedisConstants.LIKE_COUNT_KEY_PREFIX + dto.getBizType();
        // 采用zset结构缓存点赞总数，三个参数依次为：key，value(业务id)，score（点赞数量）
        redisTemplate.opsForZSet().add(bizTypeKey, dto.getBizId().toString(), likedTimes);
    }

    /**
     * 取消赞 - 基于Redis实现
     *
     * @param dto    表单信息
     * @param userId 用户id
     * @return 取消是否成功
     */
    private boolean unliked(LikeRecordFormDTO dto, Long userId) {
        // 拼接key
        String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + dto.getBizId();
        // 判断用户是否点赞，就是判断存在且唯一，而set能够实现自动去重
        // redisTemplate.boundSetOps(key).remove(userId);
        Long result = redisTemplate.opsForSet().remove(key, userId);
        return result != null && result > 0;
    }

    /**
     * 点赞 - 基于Redis实现
     *
     * @param dto    表单信息
     * @param userId 用户id
     * @return 点赞是否成功
     */
    private boolean liked(LikeRecordFormDTO dto, Long userId) {
        // 拼接key
        String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + dto.getBizId();
        // 判断用户是否点赞，就是判断存在且唯一，而set能够实现自动去重
        // 通过redisTemplate往set添加数据，返回结果为操作成功的记录数
        // 等效写法：redisTemplate.boundSetOps(key).add(userId);
        Long result = redisTemplate.opsForSet().add(key, userId.toString());
        return result != null && result > 0;
    }


    // /**
    //  * 取消赞
    //  *
    //  * @param dto    表单信息
    //  * @param userId 用户id
    //  * @return 取消是否成功
    //  */
    // private boolean unliked(LikeRecordFormDTO dto, Long userId) {
    //     // 根据userId和bizId查询点赞记录是否已存在
    //     LikedRecord likedRecord = this.lambdaQuery().eq(LikedRecord::getUserId, userId)
    //             .eq(LikedRecord::getBizId, dto.getBizId())
    //             .one();
    //     if (likedRecord == null) { // 没点过赞
    //         return false;
    //     }
    //     // 删除点赞记录
    //     return this.removeById(likedRecord.getId());
    // }
    //
    // /**
    //  * 点赞
    //  *
    //  * @param dto    表单信息
    //  * @param userId 用户id
    //  * @return 点赞是否成功
    //  */
    // private boolean liked(LikeRecordFormDTO dto, Long userId) {
    //     // 根据userId和bizId查询点赞记录是否已存在
    //     LikedRecord likedRecord = this.lambdaQuery().eq(LikedRecord::getUserId, userId)
    //             .eq(LikedRecord::getBizId, dto.getBizId())
    //             .one();
    //     if (likedRecord != null) { // 点过赞
    //         return false;
    //     }
    //     // 保存点赞记录
    //     LikedRecord newRecord = BeanUtil.toBean(dto, LikedRecord.class);
    //     newRecord.setUserId(userId);
    //     return this.save(newRecord);
    // }

    /**
     * 批量查询点赞状态 - Redis + 循环遍历实现
     * 使用set返回可以自动去重
     *
     * @param bizIds 业务id列表
     */
    // @Override
    // public Set<Long> getLikesStatusByBizIds(List<Long> bizIds) {
    //     // 传入的业务id列表为空，直接返回空集
    //     if (CollUtil.isEmpty(bizIds)) {
    //         return Collections.emptySet();
    //     }
    //     // 获取当前登录用户
    //     Long userId = UserContext.getUser();
    //     Set<Long> res = new HashSet<>(bizIds.size());
    //     // 循环读取redis中的业务id的set，判断是否当前用户点赞过
    //     for (Long bizId : bizIds) {
    //         Boolean member = redisTemplate.opsForSet().isMember(RedisConstants.LIKE_BIZ_KEY_PREFIX + bizId, userId.toString());
    //         if (member) {    // 在set中存在，表明用户点赞过
    //             res.add(bizId);
    //         }
    //     }
    //     // 当前用户点赞过的业务id列表
    //     return res;
    // }

    /**
     * 从redis取指定类型点赞数量并发送消息到RabbitMQ
     *
     * @param bizType    业务类型
     * @param maxBizSize 每次任务取出的业务score标准
     */
    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {
        // 拼接key
        String key = RedisConstants.LIKE_COUNT_KEY_PREFIX + bizType;
        // 读取redis，从zset(按score排序)中取出点赞信息，弹出score小于maxBizSize
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet().popMin(key, maxBizSize);
        if (CollUtil.isNotEmpty(typedTuples)) {
            List<LikedTimesDTO> likedTimesDTOS = new ArrayList<>(typedTuples.size());
            for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
                Double likedTimes = tuple.getScore();   // 获取点赞数量
                String bizId = tuple.getValue();    // 获取业务id
                // 校验是否为空
                if (StringUtils.isBlank(bizId) || likedTimes == null) {
                    continue;
                }
                // 封装LikedTimeDTO
                LikedTimesDTO likedTimesDTO = LikedTimesDTO.builder()
                        .bizId(Long.valueOf(bizId))
                        .likedTimes(likedTimes.intValue())
                        .build();
                likedTimesDTOS.add(likedTimesDTO);
            }
            // 发送RabbitMQ消息
            if (CollUtil.isNotEmpty(likedTimesDTOS)) {
                log.info("发送点赞消息，消息内容：{}", likedTimesDTOS);
                rabbitMqHelper.send(MqConstants.Exchange.LIKE_RECORD_EXCHANGE,
                        StringUtils.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, bizType),
                        likedTimesDTOS);
            }
        }
    }


    /**
     * 批量查询点赞状态 - Redis + 循环遍历实现
     * 使用set返回可以自动去重
     *
     * @param bizIds 业务id列表
     */
    @Override
    public Set<Long> getLikesStatusByBizIds(List<Long> bizIds) {
        // 传入的业务id列表为空，直接返回空集
        if (CollUtil.isEmpty(bizIds)) {
            return Collections.emptySet();
        }
        // 获取当前登录用户
        Long userId = UserContext.getUser();
        // 使用redis管道，提高性能，objects实际上是Boolean类型列表
        List<Object> objects = redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                StringRedisConnection stringRedisConnection = (StringRedisConnection) connection;
                for (Long bizId : bizIds) {
                    String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + bizId;    // 拼接key
                    // 真正的返回值类型由调用的方法决定
                    stringRedisConnection.sIsMember(key, userId.toString());    // 查询当前userId是否在对应业务id的set中
                }
                // executePipelined方法会忽略 doInRedis 方法的返回值，而只关心执行管道操作后得到的结果列表。
                return null;
            }
        });
        // 根据管道操作的结果列表 objects，筛选出当前用户点赞过的业务id列表
        return IntStream.range(0, objects.size())  // 遍历结果列表的索引范围
                .filter(i -> (Boolean) objects.get(i))  // 过滤出返回值为 true 的索引
                .mapToObj(bizIds::get)  // 将索引映射为对应的业务id
                .collect(Collectors.toSet());  // 将业务id 收集为 Set，并返回
    }

}
