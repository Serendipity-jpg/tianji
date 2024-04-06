package com.tianji.promotion.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.annotation.MyLock;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.UserCouponQuery;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import com.tianji.promotion.utils.CodeUtil;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
 * </p>
 *
 * @author Sakura
 */
@EqualsAndHashCode
@Service
@RequiredArgsConstructor
public class UserCouponRedissonMQServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {

    private final CouponMapper couponMapper;
    private final StringRedisTemplate redisTemplate;
    private final IExchangeCodeService exchangeCodeService;
    private final RedissonClient redissonClient;
    private final RabbitMqHelper rabbitMqHelper;

    /**
     * 查询用户领取的优惠券列表-用户端
     *
     * @param query 分页查询参数
     * @return 用户领取的优惠券列表
     */
    @Override
    public PageDTO<CouponVO> queryUserCouponList(UserCouponQuery query) {
        // 分页查询，根据user_id进行匹配
        Page<UserCoupon> page = this.lambdaQuery().eq(UserCoupon::getUserId, UserContext.getUser())
                .eq(query.getStatus() != null, UserCoupon::getStatus, UserCouponStatus.of(query.getStatus()))
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        if (CollUtil.isEmpty(page.getRecords())) {   // 判空
            return PageDTO.of(page, Collections.emptyList());
        }
        // 收集用户领取的优惠券id，批量查优惠券信息
        List<Long> couponIds = page.getRecords().stream().map(UserCoupon::getCouponId).collect(Collectors.toList());
        List<Coupon> coupons = couponMapper.selectBatchIds(couponIds);
        // 封装vo并返回
        List<CouponVO> couponVOS = coupons.stream().map(userCoupon -> BeanUtil.toBean(userCoupon, CouponVO.class))
                .collect(Collectors.toList());
        return PageDTO.of(page, couponVOS);
    }

    /**
     * 用户凭兑换码兑换优惠券
     *
     * @param code 兑换码
     */
    @Override
    @MyLock(name = PromotionConstants.COUPON_LOCK_KEY + "#{T(com.tianji.common.utils.UserContext).getUser()}")
    public void exchangeCoupon(String code) {
        // 判断兑换码code是否为空
        if (StrUtil.isBlank(code)) {
            throw new BadRequestException("非法参数");
        }
        // 解析兑换码拿到自增id
        Long serialNum = CodeUtil.parseCode(code);
        // 判断兑换码是否已兑换，使用redis的bitmap结构:setbit key offset 1
        boolean hasExchanged = exchangeCodeService.hasExchanged(serialNum, true);
        if (hasExchanged) {  // 兑换码已使用
            throw new BizIllegalException("兑换码已被使用");
        }
        try {
            // 根据兑换码id查询对应优惠券id
            Long couponId = exchangeCodeService.getExchangeTargetId(serialNum);
            // 查询优惠券信息
            Coupon coupon = queryCouponFromRedis(couponId);
            if (coupon == null) {
                throw new BizIllegalException("兑换码没有关联的优惠券");
            }
            // 校验时间是否在领取开始时间 issue_begin_time 和 领取结束时间 issue_end_time区间内
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(coupon.getIssueBeginTime()) || now.isAfter(coupon.getIssueEndTime())) {
                throw new BadRequestException("该优惠券未开始发放或已过期");
            }
            // 判断优惠券余量
            if (coupon.getTotalNum() <= 0 || coupon.getIssueNum() >= coupon.getTotalNum()) {
                throw new BadRequestException("该优惠券库存不足");
            }
            // 校验当前用户是否已达到该优惠券的领取上限
            Long userId = UserContext.getUser();
            String key = PromotionConstants.USER_COUPON_CACHE_KEY_PREFIX + couponId;
            // 获取用户已领取数量+1，再判断是否超过上限，delta表示增量
            Long increment = redisTemplate.opsForHash().increment(key, userId.toString(), 1);
            if (increment > coupon.getUserLimit()) {   // 超出限领数量
                // redis用户已领取优惠券数量减1
                redisTemplate.opsForHash().increment(key, userId.toString(), -1);
                throw new BizIllegalException("超过限领数量");
            }
            // 更新优惠券的已发放数量+1
            redisTemplate.opsForHash().increment(PromotionConstants.COUPON_CACHE_KEY_PREFIX + couponId,
                    "issueNum", 1);

            // 发送MQ消息
            rabbitMqHelper.send(
                    MqConstants.Exchange.PROMOTION_EXCHANGE,
                    MqConstants.Key.COUPON_RECEIVE,
                    UserCouponDTO.builder()
                            .couponId(couponId)    // 优惠券id
                            .serialNum(serialNum)  // 兑换码序列id（注解id）
                            .userId(userId) // 用户id
                            .build()
            );

        } catch (Exception e) {
            // 重置redis的bitmap结构存储的兑换码状态，setbit key offset 1
            exchangeCodeService.hasExchanged(serialNum, false);
            throw e;
        }
    }

    /**
     * 领取优惠券
     *
     * @param id 优惠券id
     */
    @Override
    @MyLock(name = PromotionConstants.COUPON_LOCK_KEY + "#{id}")
    public void receiveCoupon(Long id) {
        // 从db根据id查询优惠券信息，并进行相关校验
        // Coupon coupon = couponMapper.selectById(id);
        // 从redis中获取优惠券信息
        Coupon coupon = queryCouponFromRedis(id);
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在");
        }
        // 校验时间是否在领取开始时间 issue_begin_time 和 领取结束时间 issue_end_time区间内
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getIssueBeginTime()) || now.isAfter(coupon.getIssueEndTime())) {
            throw new BadRequestException("该优惠券未开始发放或已过期");
        }
        // 判断优惠券余量
        if (coupon.getTotalNum() <= 0 || coupon.getIssueNum() >= coupon.getTotalNum()) {
            throw new BadRequestException("该优惠券库存不足");
        }
        // 校验当前用户是否已达到该优惠券的领取上限
        Long userId = UserContext.getUser();
        String key = PromotionConstants.USER_COUPON_CACHE_KEY_PREFIX + id;
        // 获取用户已领取数量+1，再判断是否超过上限，delta表示增量
        Long increment = redisTemplate.opsForHash().increment(key, userId.toString(), 1);
        if (increment > coupon.getUserLimit()) {   // 超出限领数量
            // redis用户已领取优惠券数量减1
            redisTemplate.opsForHash().increment(key, userId.toString(), -1);
            throw new BizIllegalException("超过限领数量");
        }
        // 更新优惠券的已发放数量+1
        redisTemplate.opsForHash().increment(PromotionConstants.COUPON_CACHE_KEY_PREFIX + id,
                "issueNum", 1);
        // 发送RabbitMQ消息，userId，couponId，即哪个用户需要新增用户券
        UserCouponDTO userCouponDTO = UserCouponDTO.builder()
                .couponId(id)
                .userId(userId)
                .build();
        rabbitMqHelper.send(MqConstants.Exchange.PROMOTION_EXCHANGE,
                MqConstants.Key.COUPON_RECEIVE,
                userCouponDTO);
    }

    /**
     * 从redis中获取优惠券信息
     *
     * @param couponId 优惠券id
     * @return 优惠券信息（开始领取时间、结束领取时间、发放总数量、限领数量）
     */
    private Coupon queryCouponFromRedis(Long couponId) {
        String key = PromotionConstants.COUPON_CACHE_KEY_PREFIX + couponId;   // 拼接key
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);  // 获取所有键值对，底层是hgetall命令
        if (entries.isEmpty()) {    // 查询不到，返回null
            return null;
        }
        // 反序列返回实现map转po
        return BeanUtils.mapToBean(entries, Coupon.class, false, CopyOptions.create());
    }

    /**
     * 校验当前用户是否已达到该优惠券的领取上限
     * 优惠券已发放数量+1
     * 保存用户券
     * #{userId}：使用SPEL表达式，对应着形参名称userId
     * 更新，移除锁
     * 更新，如果是兑换码领券，需要更新兑换码状态
     */
    @Transactional
    @Override
    public void checkAndCreateUserCoupon(UserCouponDTO userCouponDTO) {
        // 1.查询优惠券(因为Redis的优惠券信息较少)
        Coupon coupon = couponMapper.selectById(userCouponDTO.getCouponId());
        if (coupon == null) {
            throw new BizIllegalException("优惠券不存在！");
        }
        // 优惠券已发放数量+1
        int rows = couponMapper.increaseIssueNum(coupon.getId());
        if (rows == 0) { // rows 为 0即更新失败，需要抛出异常，触发回滚
            throw new BizIllegalException("优惠券库存不足");
        }
        Long userId = userCouponDTO.getUserId();
        // 保存用户券
        saveUserCoupon(userId, coupon);
        // 如果是兑换码方式，需要更新兑换码状态
        if (userCouponDTO.getSerialNum() != null) {
            // 更新兑换码状态和兑换人
            exchangeCodeService.lambdaUpdate()
                    .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)  // 更新状态为已兑换
                    .set(ExchangeCode::getUserId, userId)   // 更新兑换人
                    .eq(ExchangeCode::getId, userCouponDTO.getSerialNum())   // 兑换码自增id
                    .update();
        }
    }

    /**
     * 保存用户券
     *
     * @param userId 用户id
     * @param coupon 优惠券信息
     */
    private void saveUserCoupon(Long userId, Coupon coupon) {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setCouponId(coupon.getId());
        userCoupon.setUserId(userId);
        LocalDateTime termBeginTime = coupon.getTermBeginTime();    //  优惠券领取开始时间
        LocalDateTime termEndTime = coupon.getTermEndTime();    // 优惠券领取结束时间
        // 优惠券领取开始时间和结束时间需要校验下
        if (termBeginTime == null && termEndTime == null) {  // 前端传参决定
            termBeginTime = LocalDateTime.now();
            termEndTime = termBeginTime.plusDays(coupon.getTermDays());
        }
        userCoupon.setTermBeginTime(termBeginTime);
        userCoupon.setTermEndTime(termEndTime);
        this.save(userCoupon);
    }


    /**
     * 从redis中获取兑换码信息
     *
     * @param exchangeCodeId 兑换码id
     * @return 兑换码信息（优惠券id，兑换码过期时间）
     */
    // private ExchangeCode queryExchangeCodeFromRedis(Long exchangeCodeId) {
    //     String key = PromotionConstants.EXCHANGE_CODE_CACHE_KEY_PREFIX + exchangeCodeId;   // 拼接key
    //     Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);  // 获取所有键值对，底层是hgetall命令
    //     if (entries.isEmpty()) {    // 查询不到，返回null
    //         return null;
    //     }
    //     // 反序列返回实现map转po
    //     return BeanUtils.mapToBean(entries, ExchangeCode.class, false, CopyOptions.create());
    // }
}
