package com.tianji.promotion.mapper;

import com.tianji.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 优惠券的规则信息 Mapper 接口
 * </p>
 *
 * @author Sakura
 */
public interface CouponMapper extends BaseMapper<Coupon> {

    /***
     * 优惠券已领取数量 + 1
     */
    @Update("update coupon set issue_num = issue_num + 1 where id = #{id} AND issue_num < total_num ")
    int increaseIssueNum(@Param("id") Long id);
}
