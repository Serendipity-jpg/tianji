package com.tianji.promotion.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;

/**
 * <p>
 * 优惠券的规则信息 服务类
 * </p>
 *
 * @author Sakura
 */
public interface ICouponService extends IService<Coupon> {

    /**
     * 新增优惠券
     * @param dto   相关参数
     */
    void saveCoupon(CouponFormDTO dto);

    /**
     * 管理端分页查询优惠券
     * @param query   分页查询参数
     */
    PageDTO<CouponPageVO> couponPage(CouponQuery query);

    /**
     * 发放优惠券
     *
     * @param dto 发放优惠券的日期参数
     * @param id    优惠券id
     */
    void issueCoupon(Long id, CouponIssueFormDTO dto);

    /**
     * 修改优惠券
     *
     * @param dto 相关参数
     * @param id 优惠券id
     */
    void updateCoupon(CouponFormDTO dto, Long id);

    /**
     * 查询优惠券详情，需要封装关联的优惠券范围信息
     * @param id 优惠券id
     */
    CouponDetailVO queryCouponById(Long id);

    /**
     * 删除优惠券，需要删除关联的优惠券范围信息
     * @param id 优惠券id
     */
    void deleteCoupon(Long id);

    /**
     * 暂停发放优惠券
     * @param id 优惠券id
     */
    void pauseIssueCoupon(Long id);
}
