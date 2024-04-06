package com.tianji.promotion.constants;

public interface PromotionConstants {
    // 兑换码全局自增id对应键
    String COUPON_CODE_SERIAL_KEY = "coupon:code:serial";
    // 标记全局兑换码是否已兑换的bitmap的键
    String COUPON_CODE_MAP_KEY = "coupon:code:map";
    // redis存储优惠券信息，结构为hash，key为优惠券id，value为开始领取时间、结束领取时间、已发放数量、发放总数量、限领数量
    String COUPON_CACHE_KEY_PREFIX = "prs:coupon:";
    // redis存储兑换码信息，结构为hash，key为兑换码id，value为目标业务id，兑换码过期时间
    String EXCHANGE_CODE_CACHE_KEY_PREFIX = "prs:code:";
    // 用于redis存储优惠券每个用户已领取数量，结构为hash，key为userId，value为已领取数量
    String USER_COUPON_CACHE_KEY_PREFIX = "prs:user:coupon:";
    // 用于记录优惠券对应的最大序列号，采用zset结构，member：couponId，score：兑换码的最大序列号（serialNum）
    String COUPON_RANGE_KEY = "coupon:code:range";

    String[] RECEIVE_COUPON_ERROR_MSG = {
            "活动未开始",
            "库存不足",
            "活动已经结束",
            "领取次数过多",
    };


    String[] EXCHANGE_COUPON_ERROR_MSG = {
            "兑换码已兑换",
            "无效兑换码",
            "活动未开始",
            "活动已经结束",
            "领取次数过多",
    };

    // redisson优惠券锁key前缀，后面拼接userId，并发优化后不再使用
    String COUPON_LOCK_KEY = "lock:coupon:uid:";

    // redisson兑换码锁key前缀，后面拼接兑换码，并发优化后不再使用
    String EXCHANGE_CODE_LOCK_KEY = "lock:code:";
}