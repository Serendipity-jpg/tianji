package com.tianji.promotion.discount;

import com.tianji.promotion.enums.DiscountType;

import java.util.EnumMap;

/**
 * 工厂模式
 */
public class DiscountStrategy {

    private final static EnumMap<DiscountType, Discount> strategies;

    static {
        strategies = new EnumMap<>(DiscountType.class); // 枚举map
        strategies.put(DiscountType.NO_THRESHOLD, new NoThresholdDiscount());   // key为优惠券类型，value为对应优惠券折扣计算类
        strategies.put(DiscountType.PER_PRICE_DISCOUNT, new PerPriceDiscount());
        strategies.put(DiscountType.RATE_DISCOUNT, new RateDiscount());
        strategies.put(DiscountType.PRICE_DISCOUNT, new PriceDiscount());
    }

    /**
     * 根据优惠券类型生成对应优惠券折扣计算类
     * @param type  优惠券类型
     * @return 对应优惠券折扣计算类（实现了Discount接口）
     */
    public static Discount getDiscount(DiscountType type) {
        return strategies.get(type);
    }
}
