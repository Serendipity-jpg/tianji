package com.tianji.promotion.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tianji.common.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CouponScopeType implements BaseEnum {
    CATEGORY(1, "分类"),
    COURSE(2, "课程");

    @JsonValue
    @EnumValue
    private final int value;
    private final String desc;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CouponScopeType of(Integer value) {
        if (value == null) {
            return null;
        }
        for (CouponScopeType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return null;
    }

    public static String desc(Integer value) {
        CouponScopeType type = of(value);
        return type == null ? "" : type.desc;
    }
}
