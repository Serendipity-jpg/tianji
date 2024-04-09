package com.tianji.promotion.discount;

import com.tianji.common.utils.NumberUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.promotion.domain.po.Coupon;
import lombok.RequiredArgsConstructor;

/**
 * 打折券折扣计算
 */
@RequiredArgsConstructor
public class RateDiscount implements Discount {

    private static final String RULE_TEMPLATE = "满{}打{}折，上限{}元";

    /**
     * 判断当前价格是否满足优惠券使用限制（是否满足门槛）
     * @param totalAmount 订单总价
     * @param coupon 优惠券信息
     * @return
     */
    @Override
    public boolean canUse(int totalAmount, Coupon coupon) {
        // 判断订单总金额是否大于等于使用门槛金额
        return totalAmount >= coupon.getThresholdAmount();
    }

    /**
     * 计算折扣金额
     * @param totalAmount 总金额
     * @param coupon 优惠券信息
     * @return 折扣金额
     */
    @Override
    public int calculateDiscount(int totalAmount,  Coupon coupon) {
        // 计算折扣，扩大100倍计算，向下取整，单位是分，折扣也是百分比数字记录
        return Math.min(coupon.getMaxDiscountAmount(), totalAmount * (100 - coupon.getDiscountValue()) / 100);
    }

    /**
     * 根据优惠券规则拼接优化描述信息描述
     * @return 规则描述信息
     */
    @Override
    public String getRule( Coupon coupon) {
        return StringUtils.format(
                RULE_TEMPLATE,
                // scaleToStr数据处理，db存的金额单位是分，折扣：90表示9折
                NumberUtils.scaleToStr(coupon.getThresholdAmount(), 2),
                NumberUtils.scaleToStr(coupon.getDiscountValue(), 1),
                NumberUtils.scaleToStr(coupon.getMaxDiscountAmount(), 2)
        );
    }
}
