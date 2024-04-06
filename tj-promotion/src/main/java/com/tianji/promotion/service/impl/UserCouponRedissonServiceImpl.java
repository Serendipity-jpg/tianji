// package com.tianji.promotion.service.impl;
//
// import cn.hutool.core.bean.BeanUtil;
// import cn.hutool.core.collection.CollUtil;
// import cn.hutool.core.util.StrUtil;
// import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
// import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
// import com.tianji.common.domain.dto.PageDTO;
// import com.tianji.common.exceptions.BadRequestException;
// import com.tianji.common.exceptions.BizIllegalException;
// import com.tianji.common.utils.UserContext;
// import com.tianji.promotion.constants.PromotionConstants;
// import com.tianji.promotion.domain.po.Coupon;
// import com.tianji.promotion.domain.po.ExchangeCode;
// import com.tianji.promotion.domain.po.UserCoupon;
// import com.tianji.promotion.domain.query.UserCouponQuery;
// import com.tianji.promotion.domain.vo.CouponVO;
// import com.tianji.promotion.enums.CouponStatus;
// import com.tianji.promotion.enums.ExchangeCodeStatus;
// import com.tianji.promotion.enums.UserCouponStatus;
// import com.tianji.promotion.mapper.CouponMapper;
// import com.tianji.promotion.mapper.UserCouponMapper;
// import com.tianji.promotion.service.IExchangeCodeService;
// import com.tianji.promotion.service.IUserCouponService;
// import com.tianji.promotion.utils.CodeUtil;
// import lombok.RequiredArgsConstructor;
// import org.aspectj.lang.annotation.Around;
// import org.redisson.api.RLock;
// import org.redisson.api.RedissonClient;
// import org.springframework.aop.framework.AopContext;
// import org.springframework.data.redis.core.StringRedisTemplate;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
//
// import java.time.LocalDateTime;
// import java.util.Collections;
// import java.util.List;
// import java.util.stream.Collectors;
//
// /**
//  * <p>
//  * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
//  * </p>
//  *
//  * @author Sakura
//  */
// @Service
// @RequiredArgsConstructor
// public class UserCouponRedissonServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {
//
//     private final CouponMapper couponMapper;
//     private final StringRedisTemplate redisTemplate;
//     private final IExchangeCodeService exchangeCodeService;
//     private final RedissonClient redissonClient;
//
//     /**
//      * 查询用户领取的优惠券列表-用户端
//      *
//      * @param query 分页查询参数
//      * @return 用户领取的优惠券列表
//      */
//     @Override
//     public PageDTO<CouponVO> queryUserCouponList(UserCouponQuery query) {
//         // 分页查询，根据user_id进行匹配
//         Page<UserCoupon> page = this.lambdaQuery().eq(UserCoupon::getUserId, UserContext.getUser())
//                 .eq(query.getStatus() != null, UserCoupon::getStatus, UserCouponStatus.of(query.getStatus()))
//                 .page(query.toMpPageDefaultSortByCreateTimeDesc());
//         if (CollUtil.isEmpty(page.getRecords())) {   // 判空
//             return PageDTO.of(page, Collections.emptyList());
//         }
//         // 收集用户领取的优惠券id，批量查优惠券信息
//         List<Long> couponIds = page.getRecords().stream().map(UserCoupon::getCouponId).collect(Collectors.toList());
//         List<Coupon> coupons = couponMapper.selectBatchIds(couponIds);
//         // 封装vo并返回
//         List<CouponVO> couponVOS = coupons.stream().map(userCoupon -> BeanUtil.toBean(userCoupon, CouponVO.class))
//                 .collect(Collectors.toList());
//         return PageDTO.of(page, couponVOS);
//     }
//
//     /**
//      * 用户凭兑换码兑换优惠券
//      *
//      * @param code 兑换码
//      */
//     @Override
//     // @Transactional
//     public void exchangeCoupon(String code) {
//         // 判断兑换码code是否为空
//         if (StrUtil.isBlank(code)) {
//             throw new BadRequestException("非法参数");
//         }
//         // 解析兑换码拿到自增id
//         Long serialNum = CodeUtil.parseCode(code);
//         // 判断兑换码是否已兑换，使用redis的bitmap结构:setbit key offset 1
//         boolean hasExchanged = exchangeCodeService.hasExchanged(serialNum, true);
//         if (hasExchanged) {  // 兑换码已使用
//             throw new BizIllegalException("兑换码已被使用");
//         }
//         try {
//             // 判断兑换码是否存在，根据自增id查询
//             ExchangeCode exchangeCode = exchangeCodeService.getById(serialNum);
//             if (exchangeCode == null) {
//                 throw new BizIllegalException("兑换码不存在");
//             }
//             // 判断兑换码是否已过期
//             LocalDateTime now = LocalDateTime.now();
//             if (now.isAfter(exchangeCode.getExpiredTime())) {
//                 throw new BizIllegalException("兑换码已过期");
//             }
//             // 查询优惠券信息
//             Coupon coupon = couponMapper.selectById(exchangeCode.getExchangeTargetId());
//             if (coupon == null) {
//                 throw new BizIllegalException("兑换码没有关联的优惠券");
//             }
//             // 校验领取上限、更新已发放优惠券+1并生成用户券
//             Long userId = UserContext.getUser();
//             // synchronized (userId.toString().intern()){  // 加锁
//             //     IUserCouponService userCouponService = (IUserCouponService) AopContext.currentProxy();
//             //     userCouponService.checkAndCreateUserCoupon(userId, coupon, serialNum.intValue());
//             // }
//             String key = PromotionConstants.COUPON_LOCK_KEY + userId.toString();
//             // 创建锁对象
//             RLock lock = redissonClient.getLock(key);
//             boolean isLock = lock.tryLock();    // 尝试获取锁，无参数，看门狗机制生效，默认失效时间30s
//             if (!isLock) {  // 如果获取锁失败
//                 throw new BizIllegalException("请求太频繁，请稍后重试");
//             } else {    // 获取锁成功，执行业务逻辑
//                 IUserCouponService userCouponService = (IUserCouponService) AopContext.currentProxy();
//                 userCouponService.checkAndCreateUserCoupon(userId, coupon, serialNum.intValue());
//             }
//         } catch (Exception e) {
//             // 重置redis的bitmap结构存储的兑换码状态，setbit key offset 1
//             exchangeCodeService.hasExchanged(serialNum, false);
//             throw e;
//         }
//     }
//
//     /**
//      * 领取优惠券
//      *
//      * @param id 优惠券id
//      */
//     @Override
//     // @Transactional
//     public void receiveCoupon(Long id) {
//         // 根据id查询优惠券信息，并进行相关校验
//         Coupon coupon = couponMapper.selectById(id);
//         if (coupon == null) {
//             throw new BadRequestException("优惠券不存在");
//         }
//         // 校验优惠券状态是否为发放中
//         if (!coupon.getStatus().equals(CouponStatus.ISSUING)) {
//             throw new BadRequestException("该优惠券未开始发放");
//         }
//         // 校验时间是否在领取开始时间 issue_begin_time 和 领取结束时间 issue_end_time区间内
//         LocalDateTime now = LocalDateTime.now();
//         if (now.isBefore(coupon.getIssueBeginTime()) || now.isAfter(coupon.getIssueEndTime())) {
//             throw new BadRequestException("该优惠券未开始发放或已过期");
//         }
//         // 判断优惠券余量
//         if (coupon.getTotalNum() <= 0 || coupon.getIssueNum() >= coupon.getTotalNum()) {
//             throw new BadRequestException("该优惠券库存不足");
//         }
//         // 校验当前用户是否已达到该优惠券的领取上限
//         Long userId = UserContext.getUser();
//         // 校验领取上限、更新已发放优惠券+1并生成用户券
//         // synchronized (userId.toString().intern()) { // 先加锁，在开启事务，避免所失效
//         //     IUserCouponService userCouponService = (IUserCouponService) AopContext.currentProxy();
//         //     userCouponService.checkAndCreateUserCoupon(userId, coupon, null);
//         // }
//         String key = PromotionConstants.COUPON_LOCK_KEY + userId.toString();
//         // 创建锁对象
//         RLock lock = redissonClient.getLock(key);
//         boolean isLock = lock.tryLock();    // 尝试获取锁，无参数，看门狗机制生效，默认失效时间30s
//         if (!isLock) {  // 如果获取锁失败
//             throw new BizIllegalException("请求太频繁，请稍后重试");
//         } else {    // 获取锁成功，执行业务逻辑
//             IUserCouponService userCouponService = (IUserCouponService) AopContext.currentProxy();
//             userCouponService.checkAndCreateUserCoupon(userId, coupon, null);
//         }
//     }
//
//     /**
//      * 校验当前用户是否已达到该优惠券的领取上限
//      * 优惠券已发放数量+1
//      * 保存用户券
//      */
//     @Transactional
//     public void checkAndCreateUserCoupon(Long userId, Coupon coupon, Integer serialNum) {
//         // synchronized (userId.toString().intern()) {  // intern会从常量池取
//         // 查询当前用户已领取的优惠券数量
//         Integer count = this.lambdaQuery().eq(UserCoupon::getUserId, userId)
//                 .eq(UserCoupon::getCouponId, coupon.getId())
//                 .count();
//         if (count != null && count >= coupon.getUserLimit()) {
//             throw new BadRequestException("该优惠券已达到领取上限");
//         }
//         // 优惠券已发放数量+
//         int rows = couponMapper.increaseIssueNum(coupon.getId());
//         if (rows == 0) { // rows == 0即更新失败，需要抛出异常，触发回滚
//             throw new BizIllegalException("优惠券库存不足");
//         }
//         // 保存用户券
//         saveUserCoupon(userId, coupon);
//         if (serialNum != null) {
//             // 更新兑换码状态和兑换人
//             exchangeCodeService.lambdaUpdate()
//                     .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)  // 更新状态为已兑换
//                     .set(ExchangeCode::getUserId, userId)   // 更新兑换人
//                     .eq(ExchangeCode::getId, serialNum)   // 兑换码自增id
//                     .update();
//         }
//         // }
//     }
//
//     /**
//      * 保存用户券
//      *
//      * @param userId 用户id
//      * @param coupon 优惠券信息
//      */
//     private void saveUserCoupon(Long userId, Coupon coupon) {
//         UserCoupon userCoupon = new UserCoupon();
//         userCoupon.setCouponId(coupon.getId());
//         userCoupon.setUserId(userId);
//         LocalDateTime termBeginTime = coupon.getTermBeginTime();    //  优惠券领取开始时间
//         LocalDateTime termEndTime = coupon.getTermEndTime();    // 优惠券领取结束时间
//         // 优惠券领取开始时间和结束时间需要校验下
//         if (termBeginTime == null && termEndTime == null) {  // 前端传参决定
//             termBeginTime = LocalDateTime.now();
//             termEndTime = termBeginTime.plusDays(coupon.getTermDays());
//         }
//         userCoupon.setTermBeginTime(termBeginTime);
//         userCoupon.setTermEndTime(termEndTime);
//         this.save(userCoupon);
//     }
// }
