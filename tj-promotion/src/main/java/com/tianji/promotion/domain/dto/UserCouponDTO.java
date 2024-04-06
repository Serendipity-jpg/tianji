package com.tianji.promotion.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserCouponDTO {
    /**
     * 用户id
     */
    private Long userId;

    /**
     * 优惠券id
     */
    private Long couponId;

    /**
     * 兑换码主键自增id
     */
    private Long serialNum;
}
