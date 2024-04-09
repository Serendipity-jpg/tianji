package com.tianji.promotion.mapper;

import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.UserCoupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.promotion.enums.UserCouponStatus;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 Mapper 接口
 * </p>
 *
 * @author Sakura
 */
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     *
     * @param userId    当前用户id
     * @return  查询当前用户可用优惠券id
     */
    @Select("SELECT  c.id,c.discount_type,c.`specific`,c.threshold_amount,c.discount_value,c.max_discount_amount,uc.id as user_coupon_id "+
            "FROM coupon c " +
            "INNER JOIN user_coupon uc ON c.id = uc.coupon_id " +
            "WHERE uc.user_id = #{userId} AND uc.`status` = 1;")
    List<Coupon> queryMyCouponList(@Param("userId")Long userId);

    /***
     * 查询用户优惠券
     */
    List<Coupon> queryCouponByUserCouponIds(
            @Param("couponsIds") List<Long> couponsIds,
            @Param("status")  UserCouponStatus status);
}
