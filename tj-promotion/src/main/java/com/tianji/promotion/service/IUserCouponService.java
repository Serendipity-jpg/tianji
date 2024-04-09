package com.tianji.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCouponDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.UserCouponQuery;
import com.tianji.promotion.domain.vo.CouponVO;

import java.util.List;

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

    /**
     * 查询可用用户券方案 - 提供给trade-service远程调用
     * @param dtoList  订单课程信息列表
     * @return  折扣方案集合
     */
    List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> dtoList);

    /**
     * 根据券方案计算订单优惠明细
     */
    CouponDiscountDTO queryDiscountDetailByOrder(OrderCouponDTO orderCouponDTO);

    /**
     * 分页查询我的优惠券接口
     * @param userCouponIds 用户券id列表
     */
    List<String> queryDiscountRules(List<Long> userCouponIds);


}
