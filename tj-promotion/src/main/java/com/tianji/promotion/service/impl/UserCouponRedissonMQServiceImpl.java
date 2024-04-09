package com.tianji.promotion.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCouponDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.annotation.MyLock;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.discount.Discount;
import com.tianji.promotion.discount.DiscountStrategy;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.UserCouponQuery;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import com.tianji.promotion.utils.CodeUtil;
import com.tianji.promotion.utils.PermuteUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
 * </p>
 *
 * @author Sakura
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCouponRedissonMQServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {

    private final CouponMapper couponMapper;
    private final StringRedisTemplate redisTemplate;
    private final IExchangeCodeService exchangeCodeService;
    private final RedissonClient redissonClient;
    private final RabbitMqHelper rabbitMqHelper;
    private final ICouponScopeService couponScopeService;
    private final Executor calSolutionExecutor;
    private final UserCouponMapper userCouponMapper;

    /**
     * 查询用户领取的优惠券列表-用户端
     *
     * @param query 分页查询参数
     * @return 用户领取的优惠券列表
     */
    @Override
    public PageDTO<CouponVO> queryUserCouponList(UserCouponQuery query) {
        // 分页查询，根据user_id进行匹配
        Page<UserCoupon> page = this.lambdaQuery().eq(UserCoupon::getUserId, UserContext.getUser()).eq(query.getStatus() != null, UserCoupon::getStatus, UserCouponStatus.of(query.getStatus())).page(query.toMpPageDefaultSortByCreateTimeDesc());
        if (CollUtil.isEmpty(page.getRecords())) {   // 判空
            return PageDTO.of(page, Collections.emptyList());
        }
        // 收集用户领取的优惠券id，批量查优惠券信息
        List<Long> couponIds = page.getRecords().stream().map(UserCoupon::getCouponId).collect(Collectors.toList());
        List<Coupon> coupons = couponMapper.selectBatchIds(couponIds);
        // 封装vo并返回
        List<CouponVO> couponVOS = coupons.stream().map(userCoupon -> BeanUtil.toBean(userCoupon, CouponVO.class)).collect(Collectors.toList());
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
            redisTemplate.opsForHash().increment(PromotionConstants.COUPON_CACHE_KEY_PREFIX + couponId, "issueNum", 1);

            // 发送MQ消息
            rabbitMqHelper.send(MqConstants.Exchange.PROMOTION_EXCHANGE, MqConstants.Key.COUPON_RECEIVE, UserCouponDTO.builder().couponId(couponId)    // 优惠券id
                    .serialNum(serialNum)  // 兑换码序列id（注解id）
                    .userId(userId) // 用户id
                    .build());

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
        redisTemplate.opsForHash().increment(PromotionConstants.COUPON_CACHE_KEY_PREFIX + id, "issueNum", 1);
        // 发送RabbitMQ消息，userId，couponId，即哪个用户需要新增用户券
        UserCouponDTO userCouponDTO = UserCouponDTO.builder().couponId(id).userId(userId).build();
        rabbitMqHelper.send(MqConstants.Exchange.PROMOTION_EXCHANGE, MqConstants.Key.COUPON_RECEIVE, userCouponDTO);
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
            exchangeCodeService.lambdaUpdate().set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)  // 更新状态为已兑换
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
     * 查询可用用户券方案 - 提供给trade-service远程调用
     *
     * @param courseDTOS 订单课程信息列表
     * @return 折扣方案集合
     */
    @Override
    public List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> courseDTOS) {
        // 从user_coupon表中查询当前用户可用优惠券，关联coupon表，
        // 条件：user_id，status=1，字段：优惠券规则、优惠券id、用户券id
        List<Coupon> coupons = getBaseMapper().queryMyCouponList(UserContext.getUser());
        if (CollUtil.isEmpty(coupons)) { // 判空
            return Collections.emptyList();
        }
        // 初步筛选出订单总金额达到使用门槛的优惠券
        int totalAmount = courseDTOS.stream().mapToInt(OrderCourseDTO::getPrice).sum();    // 计算订单总金额
        List<Coupon> availableCoupons = coupons.stream().filter(coupon -> DiscountStrategy.getDiscount(coupon.getDiscountType()).canUse(totalAmount, coupon)).collect(Collectors.toList()); // 筛选出可用的优惠券
        if (CollUtil.isEmpty(availableCoupons)) { // 判空
            return Collections.emptyList();
        }
        log.debug("初筛后，可用优惠券数量：{}", availableCoupons.size());
        // 细筛（需要考虑优惠券的限定范围）及排列所有优惠方案
        // availableMap的key为优惠券po，value为该优惠券能使用的课程列表（即优惠券限定范围内存在的各个课程价格累加能到达使用门槛）
        Map<Coupon, List<OrderCourseDTO>> availableMap = findAvailableCoupons(availableCoupons, courseDTOS);
        if (availableMap.isEmpty()) { // 判空
            return Collections.emptyList();
        }
        log.debug("细筛后，可用的优惠券信息:{}", availableMap.keySet());
        availableCoupons = new ArrayList<>(availableMap.keySet());   // 可用的优惠券集合（过滤掉了限定范围但不包含订单课程信息的优惠券）
        List<List<Coupon>> solutions = PermuteUtil.permute(availableCoupons);   // 全排列方式枚举所有优惠券组合
        // 全排列中只包含券组合方案，但是页面渲染的时候需要展示单张券供用户选择
        for (Coupon coupon : availableCoupons) {
            solutions.add(List.of(coupon)); // 补充单券方案
        }
        // 计算每种方案的优惠明细
        // List<CouponDiscountDTO> couponDiscountDTOS = new ArrayList<>(); // 保存最终结果
        // for (List<Coupon> solution : solutions) {
        //     // 计算单个方案
        //     CouponDiscountDTO discountDTO = calSolutionDiscount(availableMap, solution, courseDTOS);
        //     log.debug("方案最终优惠金额:{},使用优惠券集合：{}，优惠券规则集合：{}"
        //             , discountDTO.getDiscountAmount(), discountDTO.getIds(), discountDTO.getRules());
        //     couponDiscountDTOS.add(discountDTO);
        // }
        // 使用 CompletableFuture+CountDownLatch 实现并发优化，并行计算每种方案的优惠明细并汇总
        List<CouponDiscountDTO> couponDiscountDTOS = calAllSolutionsByComptableFuture(availableMap, solutions, courseDTOS);
        // 选出所有方案中的最优解
        return findBestSolutions(couponDiscountDTOS);
    }

    /**
     * 选出所有优惠方案中的最优解
     * - 用券相同时，优惠金额最高的方案
     * - 优惠金额相同时，用券最少的方案
     *
     * @param solutions 优惠券方案
     * @return 最优解
     */
    private List<CouponDiscountDTO> findBestSolutions(List<CouponDiscountDTO> solutions) {
        // 创建两个map，分别记录用券相同金额最高；金额相同，用券最少
        Map<String, CouponDiscountDTO> moreDiscountMap = new HashMap<>();
        Map<Integer, CouponDiscountDTO> lessCouponMap = new HashMap<>();
        // 遍历方案，更新map
        for (CouponDiscountDTO solution : solutions) {
            // 优惠券id升序排序，转为逗号拼接的字符串
            String ids = solution.getIds().stream().sorted(Comparator.comparing(Long::longValue)).map(String::valueOf)   // 转为字符串
                    .collect(Collectors.joining(","));// 逗号拼接
            // 对于moreDiscountMap，如果当前方案优惠金额 小于 已有的方案优惠金额，需要忽略
            CouponDiscountDTO oldDTO = moreDiscountMap.get(ids);
            if (oldDTO != null && solution.getDiscountAmount() < oldDTO.getDiscountAmount()) {
                continue;
            }
            // 更新 moreDiscountMap
            moreDiscountMap.put(ids, solution);
            // 对于lessCouponMap，如果当前方案用券数量 大于 已有的方案用券数量，需要忽略
            oldDTO = lessCouponMap.get(ids);
            if (oldDTO != null) {   // 判空，避免NPE
                int currentSize = solution.getIds().size(); // 当前方案用券数量
                int oldSize = oldDTO.getIds().size();   // 已有的方案用券数量
                if (oldSize > 1 && currentSize > oldSize) { // size>1 表示不比较单券方案
                    continue;
                }
            }
            // 更新 lessCouponMap
            lessCouponMap.put(solution.getDiscountAmount(), solution);
        }
        // 求两map的交集
        List<CouponDiscountDTO> bestSolutions = (List<CouponDiscountDTO>) CollUtil.intersection(moreDiscountMap.values(), lessCouponMap.values());
        // 对最终的方案，按优惠金额倒序排序
        bestSolutions.sort(Comparator.comparing(CouponDiscountDTO::getDiscountAmount).reversed());
        return bestSolutions;
    }

    /**
     * 用 CompletableFuture+CountDownLatch 实现并发优化，并行计算所有方案的优惠明细
     *
     * @param availableMap 优惠券和可用课程的映射
     * @param solutions    所有的优惠券组合方案
     * @param courseDTOS   订单中所有的课程
     * @return 方案的优惠明细
     */
    @SneakyThrows   // lombok模板处理异常
    private List<CouponDiscountDTO> calAllSolutionsByComptableFuture(Map<Coupon, List<OrderCourseDTO>> availableMap, List<List<Coupon>> solutions, List<OrderCourseDTO> courseDTOS) {
        int count = solutions.size();   // 计数器初值
        CountDownLatch downLatch = new CountDownLatch(count);   // 用于并发控制的计数器
        // 保存返回结果，考虑到是多线程的情况，需要使用线程安全的集合
        List<CouponDiscountDTO> couponDiscountDTOS = Collections.synchronizedList(new ArrayList<>(count));
        for (List<Coupon> solution : solutions) {
            CompletableFuture.supplyAsync(() -> calSolutionDiscount(availableMap, solution, courseDTOS), calSolutionExecutor)    // 计算方案的优惠明细
                    .thenAccept(couponDiscountDTO -> {
                        log.debug("线程名：{},方案最终优惠金额:{},使用优惠券集合：{}，优惠券规则集合：{}", Thread.currentThread().getName(), couponDiscountDTO.getDiscountAmount(), couponDiscountDTO.getIds(), couponDiscountDTO.getRules());
                        downLatch.countDown();  // 计数器减1
                        if (CollUtil.isNotEmpty(couponDiscountDTO.getIds()) && CollUtil.isNotEmpty(couponDiscountDTO.getRules())){
                            couponDiscountDTOS.add(couponDiscountDTO);  // 合并结果
                        }

                    });
        }
        // 阻塞主线程，直到所有计算线程都完成后才被唤醒，为防止计算线程阻塞死等，设置过期时间
        downLatch.await(3, TimeUnit.SECONDS);
        return couponDiscountDTOS;
    }

    /**
     * 计算单个方案的优惠明细
     *
     * @param availableMap 优惠券和可用课程的映射
     * @param solution     优惠券组合方案
     * @param courseDTOS   订单中所有的课程
     * @return 方案的优惠明细
     */
    private CouponDiscountDTO calSolutionDiscount(Map<Coupon, List<OrderCourseDTO>> availableMap, List<Coupon> solution, List<OrderCourseDTO> courseDTOS) {
        CouponDiscountDTO discountDTO = new CouponDiscountDTO();
        // 初始化课程id和课程折扣明细的映射，初始折扣明细置为0
        Map<Long, Integer> detailMap = courseDTOS.stream().collect(Collectors.toMap(OrderCourseDTO::getId, orderCourseDTO -> 0));
        // 循环方案中的优惠券组合,计算该方案的折扣明细
        for (Coupon coupon : solution) {
            // 获取该优惠券对应的的可用课程
            List<OrderCourseDTO> availableCourses = availableMap.get(coupon);
            // 计算可用课程的总金额（所有可用课程 课程价格-该课程已有折扣明细 累加）
            int totalAmount = availableCourses.stream()
                    // 可用课程的总金额 = 课程价格-该课程已有折扣明细
                    .mapToInt(value -> value.getPrice() - detailMap.get(value.getId())).sum();
            // 判断该优惠券是否可用
            Discount discount = DiscountStrategy.getDiscount(coupon.getDiscountType());
            boolean canUse = discount.canUse(totalAmount, coupon);
            if (!canUse) {   // 该优惠券不可用，跳过本次循环
                continue;
            }
            // 计算该优惠券使用后的折扣值
            int discountAmount = discount.calculateDiscount(totalAmount, coupon);
            // 更新课程的折扣明细（修改detailMap中的课程id映射的课程明细）
            calDetailDiscount(detailMap, availableCourses, totalAmount, discountAmount);
            // 累加当前优惠券的优惠金额，也意味着该优惠生效了
            discountDTO.getIds().add(coupon.getId());   // 新增优惠券id
            discountDTO.getRules().add(discount.getRule(coupon));   // 新增优惠券规则
            discountDTO.setDiscountAmount(discountAmount + discountDTO.getDiscountAmount()); // 累加优惠金额
        }
        // 设置优惠明细：课程id和课程折扣明细的映射
        discountDTO.setDiscountDetail(detailMap);
        return discountDTO;
    }

    /**
     * 更新课程的折扣明细（修改detailMap中的课程id映射的课程明细）
     *
     * @param detailMap        存储课程id和课程折扣明细的映射
     * @param availableCourses 当前优惠券的可用课程列表
     * @param totalAmount      可用课程的总金额（课程价格-该课程已有折扣明细）
     * @param discountAmount   当前优惠券的优惠总金额
     */
    private void calDetailDiscount(Map<Long, Integer> detailMap, List<OrderCourseDTO> availableCourses, int totalAmount, int discountAmount) {
        // 在优惠券使用，计算每个课程的折扣明细
        // 规则：前面的课程折扣明细按比例计算，最后一个课程的 = 总的优惠券-前面课程（n-1个）优惠金额之和
        int i = 0;  // 计数器
        int remainDiscount = 0;    // 记录剩余的优惠金额
        for (OrderCourseDTO courseDTO : availableCourses) {
            i++;
            int detailAmount = 0;
            if (i == availableCourses.size()) {  // 最后一个课程
                detailAmount = remainDiscount;   // 最后一个课程的折扣明细是剩余的优惠金额
            } else { // 前面的课程（n-1）
                detailAmount = courseDTO.getPrice() * discountAmount / totalAmount;  // 比例计算，先乘后除
                remainDiscount -= detailAmount;    // 更新剩余的优惠金额
            }
            // 更新detailMap，即更新当前课程的折扣明细
            detailMap.put(courseDTO.getId(), detailMap.get(courseDTO.getId()) + detailAmount);
        }
    }

    /**
     * 细筛，查询每个优惠券对应的可用课程
     * 设定：每个优惠券使用时都有对应的课程分类（coupon_scope表biz_id字段），每个课程也有对应的课程分类
     *
     * @param availableCoupons 初筛过的优惠券集合
     * @param orderCourseDTOS  订单中的课程集合
     * @return 类型为map，key为优惠券po，value为该优惠券能使用的课程列表（即优惠券限定范围内各个课程价格累加能到达使用门槛）
     */
    private Map<Coupon, List<OrderCourseDTO>> findAvailableCoupons(List<Coupon> availableCoupons, List<OrderCourseDTO> orderCourseDTOS) {
        Map<Coupon, List<OrderCourseDTO>> availableMap = new HashMap<>();
        // 循环遍历初筛后的优惠券集合
        for (Coupon coupon : availableCoupons) {
            List<OrderCourseDTO> availableCourseDTOs = orderCourseDTOS;
            // 判断优惠券是否限定范围，是则找出每个优惠券的可用课程
            if (coupon.getSpecific()) {  // 该优惠券限定范围
                // 查询该优惠券限定的课程分类范围
                List<CouponScope> scopeList = couponScopeService.lambdaQuery().eq(CouponScope::getCouponId, coupon.getId())   // 优惠券id
                        .list();
                // 获取该优惠券限定的课程分类id列表
                List<Long> scopeIds = scopeList.stream().map(CouponScope::getBizId).collect(Collectors.toList());
                // 筛选该范围内的课程
                availableCourseDTOs = orderCourseDTOS.stream().filter(orderCourseDTO -> scopeIds.contains(orderCourseDTO.getCateId())).collect(Collectors.toList());
            }
            if (CollUtil.isEmpty(availableCourseDTOs)) { // 当前优惠券没有任何可用课程，跳过本次循环
                continue;
            }
            // 计算优惠券的可用课程的总金额，以判断订单总和是否达到优惠券门槛
            int totalAmount = orderCourseDTOS.stream().mapToInt(OrderCourseDTO::getPrice).sum();
            // 判断该优惠券是否可用，可以的话，封装到map
            Discount discount = DiscountStrategy.getDiscount(coupon.getDiscountType());
            if (discount.canUse(totalAmount, coupon)) {  // 该优惠券可用
                availableMap.put(coupon, availableCourseDTOs);
            }
        }
        return availableMap;
    }

    /**
     * 分页查询我的优惠券接口
     *
     * @param couponsIds 优惠券id列表
     */
    @Override
    public List<String> queryDiscountRules(List<Long> couponsIds) {
        // 1.查询优惠券信息
        List<Coupon> coupons = userCouponMapper.queryCouponByUserCouponIds(couponsIds, UserCouponStatus.USED);
        if (CollUtils.isEmpty(coupons)) {
            return CollUtils.emptyList();
        }
        // 2.转换规则
        return coupons.stream()
                .map(c -> DiscountStrategy.getDiscount(c.getDiscountType()).getRule(c))
                .collect(Collectors.toList());
    }



    /**
     * 根据券方案计算订单优惠明细
     */
    @Override
    public CouponDiscountDTO queryDiscountDetailByOrder(OrderCouponDTO orderCouponDTO) {
        // 1.查询用户优惠券
        List<Long> userCouponIds = orderCouponDTO.getUserCouponIds();
        List<Coupon> coupons = userCouponMapper.queryCouponByUserCouponIds(userCouponIds, UserCouponStatus.UNUSED);
        if (CollUtils.isEmpty(coupons)) {
            return null;
        }
        // 2.查询优惠券对应课程
        Map<Coupon, List<OrderCourseDTO>> availableCouponMap = findAvailableCoupons(coupons, orderCouponDTO.getCourseList());
        if (CollUtils.isEmpty(availableCouponMap)) {
            return null;
        }
        // 3.查询优惠券规则
        return  calSolutionDiscount(availableCouponMap, coupons, orderCouponDTO.getCourseList());
    }

}
