package com.tianji.promotion.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.course.CategoryClient;
import com.tianji.api.dto.course.CategoryBasicDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponScopeVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.enums.CouponScopeType;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.enums.ObtainType;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.ICouponService;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 优惠券的规则信息 服务实现类
 * </p>
 *
 * @author Sakura
 */
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {

    private final ICouponScopeService couponScopeService;
    private final CategoryClient categoryClient;
    private final IExchangeCodeService exchangeCodeService;
    private final IUserCouponService userCouponService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 新增优惠券
     *
     * @param dto 相关参数
     */
    @Override
    @Transactional
    public void saveCoupon(CouponFormDTO dto) {
        // dtop转po，保存优惠券
        Coupon coupon = BeanUtil.toBean(dto, Coupon.class);
        this.save(coupon);
        // 判断是否限定了范围
        if (!coupon.getSpecific()) { // 未限定范围
            return;
        }
        // 限定范围，需要校验dto.scopes
        List<Long> scopes = dto.getScopes();
        if (CollUtil.isEmpty(scopes)) {
            throw new BadRequestException("分类id不能为空");
        }
        // 保存优惠券限定范围到 coupon_scope表
        List<CouponScope> couponScopeList = scopes.stream().map(scope ->
                CouponScope.builder()
                        .couponId(coupon.getId())
                        .bizId(scope)
                        .type(CouponScopeType.CATEGORY)    // 范围限定类型：1-分类，2-课程
                        .build()).collect(Collectors.toList());
        // 批量保存优惠券
        couponScopeService.saveBatch(couponScopeList);
    }

    /**
     * 管理端分页查询优惠券
     *
     * @param query 分页查询参数
     */
    @Override
    public PageDTO<CouponPageVO> couponPage(CouponQuery query) {
        Page<Coupon> page = this.lambdaQuery()
                .eq(query.getType() != null, Coupon::getType, query.getType())
                .eq(query.getStatus() != null, Coupon::getStatus, query.getStatus())
                .like(StrUtil.isNotBlank(query.getName()), Coupon::getName, query.getName())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<Coupon> couponList = page.getRecords();
        if (CollUtil.isEmpty(couponList)) {  // 判空
            return PageDTO.empty(page);
        }
        // 封装到VO返回
        List<CouponPageVO> couponPageVOS = couponList.stream().map(coupon -> BeanUtil.toBean(coupon, CouponPageVO.class))
                .collect(Collectors.toList());
        return PageDTO.of(page, couponPageVOS);
    }

    /**
     * 发放优惠券
     *
     * @param id  优惠券id
     * @param dto 发放优惠券的日期参数
     */
    @Override
    public void issueCoupon(Long id, CouponIssueFormDTO dto) {
        // 校验id
        if (id == null || !id.equals(dto.getId())) {
            throw new BadRequestException("非法参数");
        }
        // 校验id是否有效
        Coupon coupon = this.getById(id);
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在");
        }
        // 校验当前优惠券状态，只有待发放和暂停状态才能执行发放操作
        if (coupon.getStatus() != CouponStatus.DRAFT && coupon.getStatus() != CouponStatus.PAUSE) {
            throw new BizIllegalException("只有待发放和暂停状态才能发放");
        }
        LocalDateTime now = LocalDateTime.now();    // 获取当前时间
        // 判断是否立即发放，issueBeginTime为空或者小于等于当前时间
        boolean instantIssue = dto.getIssueBeginTime() == null || !dto.getIssueBeginTime().isAfter(now);
        // 更新当前优惠券状态、领取开始和结束时间、使用开始和结束时间
        Coupon couponDB = BeanUtil.toBean(dto, Coupon.class);
        if (instantIssue) { // 立即发放
            // 领取开始时间前端不一定传，但领取结束时间前端必传
            couponDB.setStatus(CouponStatus.ISSUING)    // 更新当前优惠券状态为发放中
                    .setIssueBeginTime(now);
        } else {
            couponDB.setStatus(CouponStatus.UN_ISSUE);  // 更新当前优惠券状态为未开始
        }
        // 更新优惠券信息
        this.updateById(couponDB);
        // 如果优惠券是立刻发放，将部分字段（优惠券id、开始领取时间、结束领取时间、已发放数量、发放总数量、限领数量）存入redis用于实现异步领取优惠券
        if (instantIssue) {  // 立即发放
            String key = PromotionConstants.COUPON_CACHE_KEY_PREFIX + coupon.getId();   // 拼接key
            // 开始领取时间，转成Long方便比较
            redisTemplate.opsForHash().put(key, "issueBeginTime", String.valueOf(DateUtils.toEpochMilli(now)));
            // 结束领取时间
            redisTemplate.opsForHash().put(key, "issueEndTime", String.valueOf(DateUtils.toEpochMilli(dto.getIssueEndTime())));
            // 发放总数量
            redisTemplate.opsForHash().put(key, "totalNum", String.valueOf(coupon.getTotalNum()));
            // 已发放发放数量
            redisTemplate.opsForHash().put(key, "issueNum", String.valueOf(coupon.getIssueNum()));
            // 单个用户限领的优惠券数量
            redisTemplate.opsForHash().put(key, "userLimit", String.valueOf(coupon.getUserLimit()));
        }
        // 如果优惠券领取方式为指定发放，且之前状态为待发放（防止重复生成兑换码），需要生成兑换码
        if (coupon.getObtainWay().equals(ObtainType.ISSUE)
                && coupon.getStatus().equals(CouponStatus.DRAFT)) {
            // 兑换码兑换的截止时间是优惠券领取的的截止时间（前端传递）
            coupon.setIssueEndTime(couponDB.getIssueEndTime());
            // 异步生成兑换码
            exchangeCodeService.asyncGenerateExchangeCode(coupon);
        }
    }

    /**
     * 修改优惠券
     *
     * @param dto 相关参数
     * @param id  优惠券id
     */
    @Override
    @Transactional
    public void updateCoupon(CouponFormDTO dto, Long id) {
        // 校验优惠券id
        if (id == null || !id.equals(dto.getId())) {
            throw new BadRequestException("非法参数");
        }
        // 校验优惠券是否有效
        Coupon coupon = this.getById(id);
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在");
        }
        // 更新优惠券，只有待发放状态的优惠券才可以被修改
        if (!coupon.getStatus().equals(CouponStatus.DRAFT)) {
            throw new BizIllegalException("只有待发放状态才能修改");
        }
        // 拷贝属性
        Coupon couponDB = BeanUtil.toBean(dto, Coupon.class);
        this.updateById(couponDB);
        // 清空原有的优惠券id关联的CouponScope记录
        deleteScoperByCouponId(couponDB.getId());
        // 判断是否限定了范围
        if (!dto.getSpecific()) { // 未限定范围
            return;
        }
        // 限定范围，需要校验dto.scopes
        List<Long> scopes = dto.getScopes();
        if (CollUtil.isEmpty(scopes)) {
            throw new BadRequestException("分类id不能为空");
        }
        // 保存优惠券限定范围到 coupon_scope表
        List<CouponScope> couponScopeList = scopes.stream().map(scope ->
                CouponScope.builder()
                        .couponId(couponDB.getId())
                        .bizId(scope)
                        .type(CouponScopeType.CATEGORY)    // 范围限定类型：1-分类，2-课程
                        .build()).collect(Collectors.toList());
        couponScopeService.saveBatch(couponScopeList);
    }

    /**
     * 查询优惠券详情，需要封装关联的优惠券范围信息
     *
     * @param id 优惠券id
     */
    @Override
    public CouponDetailVO queryCouponById(Long id) {
        // 校验优惠券id
        Coupon coupon = this.getById(id);
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在");
        }
        // 封装到VO
        CouponDetailVO couponDetailVO = BeanUtil.toBean(coupon, CouponDetailVO.class);

        // 查询关联的优惠券范围信息
        List<CouponScope> couponScopes = couponScopeService.lambdaQuery()
                .eq(CouponScope::getCouponId, id)
                .list();
        if (CollUtil.isEmpty(couponScopes)) {   // 判空
            couponDetailVO.setScopes(Collections.emptyList());
            return couponDetailVO;
        }
        // 查询分类信息
        List<CategoryBasicDTO> categoryBasicDTOS = categoryClient.getAllOfOneLevel();
        if (CollUtil.isEmpty(categoryBasicDTOS)) {   // 判空
            throw new BizIllegalException("查询分类信息失败");
        }
        // 封装为<id，name>的map方便查找
        Map<Long, String> categoryMap = categoryBasicDTOS.stream().collect(Collectors.toMap(CategoryBasicDTO::getId,
                categoryBasicDTO -> categoryBasicDTO.getName()));
        List<CouponScopeVO> scopeVOList = couponScopes.stream().map(couponScope ->
                CouponScopeVO.builder()
                        .id(couponScope.getBizId())
                        .name(categoryMap.getOrDefault(couponScope.getBizId(), "")) // 分类名称赋值
                        .build()
        ).collect(Collectors.toList());
        couponDetailVO.setScopes(scopeVOList);
        return couponDetailVO;
    }

    /**
     * 删除优惠券，需要删除关联的优惠券范围信息
     *
     * @param id 优惠券id
     */
    @Override
    @Transactional
    public void deleteCoupon(Long id) {
        if (id == null) {
            throw new BadRequestException("参数异常");
        }
        // 删除关联的优惠券范围信息
        this.deleteScoperByCouponId(id);
        // 删除优惠券
        this.removeById(id);
    }

    /**
     * 暂停发放优惠券
     *
     * @param id 优惠券id
     */
    @Override
    public void pauseIssueCoupon(Long id) {
        // 校验优惠券id
        Coupon coupon = this.getById(id);
        if (coupon == null) {   // 判空
            throw new BadRequestException("优惠券不存在");
        }
        // 当前券状态必须是未开始或进行中
        CouponStatus status = coupon.getStatus();
        if (status != CouponStatus.UN_ISSUE && status != CouponStatus.ISSUING) {
            // 状态错误，直接结束
            return;
        }

        // 更新状态为暂停发放
        boolean success = lambdaUpdate()
                .set(Coupon::getStatus, CouponStatus.PAUSE)
                .eq(Coupon::getId, id)
                .in(Coupon::getStatus, CouponStatus.UN_ISSUE, CouponStatus.ISSUING)
                .update();
        if (!success) {
            // 可能是重复更新，结束
            log.error("重复暂停发放优惠券");
        }

        // 删除缓存
        redisTemplate.delete(PromotionConstants.COUPON_CACHE_KEY_PREFIX + id);
    }

    /**
     * 查询发放中的优惠券列表 - 用户端
     *
     * @return 发放中的优惠券列表
     */
    @Override
    public List<CouponVO> queryIssuingCouponList() {
        // 查询db,条件：状态为发放中、领取方式为手动领取
        List<Coupon> coupons = this.lambdaQuery()
                .eq(Coupon::getStatus, CouponStatus.ISSUING)    // 状态为发放中
                .eq(Coupon::getObtainWay, ObtainType.PUBLIC)     // 领取方式为手动领取
                .list();
        if (CollUtil.isEmpty(coupons)) { // 列表判空
            return Collections.emptyList();
        }
        // 获取当前登录用户
        Long userId = UserContext.getUser();
        // 获取发放中的优惠券id列表
        Set<Long> couponIds = coupons.stream().map(Coupon::getId).collect(Collectors.toSet());
        // 查询user_coupon表，条件：当前用户user_id、发放中的优惠券id列表
        List<UserCoupon> userCoupons = userCouponService.lambdaQuery().eq(UserCoupon::getUserId, userId)
                .in(UserCoupon::getCouponId, couponIds)
                .list();
        // 统计当前用户针对每个券的已领取数量，即<couponId,allCount>
        Map<Long, Long> issueMap = userCoupons.stream()
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        // 统计当前用户针对每个券的已领取且未使用数量,即<couponId,unUsedCount>
        Map<Long, Long> unUsedMap = userCoupons.stream()
                .filter(userCoupon -> userCoupon.getStatus().equals(UserCouponStatus.UNUSED))
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        List<CouponVO> couponVOS = new ArrayList<>();
        // po转vo
        for (Coupon coupon : coupons) {
            CouponVO couponVO = BeanUtil.toBean(coupon, CouponVO.class);
            // 优惠券还有剩余(已发放数量issueNum < 总发放数量totalNum)且用户领取数量(统计user_coupon表)小于该优惠券领取上限user_limit
            boolean available = coupon.getIssueNum() < coupon.getTotalNum()
                    && issueMap.getOrDefault(coupon.getId(), 0L) < coupon.getUserLimit();
            couponVO.setAvailable(available);    // 是否可以领取
            // 统计user_coupon表，查询当前用户已经领取且未使用的券数量是否大于0
            boolean received = unUsedMap.getOrDefault(coupon.getId(), 0L) > 0;
            couponVO.setReceived(received); // 是否可以使用
            couponVOS.add(couponVO);
        }
        return couponVOS;
    }

    /**
     * 清空优惠券id关联的CouponScope记录
     */
    @Transactional
    void deleteScoperByCouponId(Long couponId) {
        LambdaQueryWrapper<CouponScope> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponScope::getCouponId, couponId);
        couponScopeService.remove(wrapper);
    }
}
