package com.tianji.promotion.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tianji.common.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExchangeCodeType implements BaseEnum {
    COUPON(1, "优惠券"),
    ;
    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ExchangeCodeType of(Integer value) {
        if (value == null) {
            return null;
        }
        for (ExchangeCodeType status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }

    public static String desc(Integer value) {
        ExchangeCodeType status = of(value);
        return status == null ? "" : status.desc;
    }
}
