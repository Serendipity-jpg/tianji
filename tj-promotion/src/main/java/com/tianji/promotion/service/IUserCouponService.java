package com.tianji.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.UserCouponQuery;
import com.tianji.promotion.domain.vo.CouponVO;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务类
 * </p>
 *
 * @author Sakura
 */
public interface IUserCouponService extends IService<UserCoupon> {

    /**
     * 领取优惠券
     *
     * @param id 优惠券id
     */
    void receiveCoupon(Long id);

    /**
     * 用户凭兑换码兑换优惠券
     *
     * @param code 兑换码
     */
    void exchangeCoupon(String code);

    /**
     * 查询用户领取的优惠券列表-用户端
     *
     * @param query 分页查询参数
     * @return 用户领取的优惠券列表
     */
    PageDTO<CouponVO> queryUserCouponList(UserCouponQuery query);

    /**
     * 更新优惠券已发放数量，新增用户券
     * @param userCouponDTO 需要使用用户id，优惠券id
     */
    // void checkAndCreateUserCoupon(Long userId, Coupon coupon, Integer serialNum);
    void checkAndCreateUserCoupon(UserCouponDTO userCouponDTO);
}
