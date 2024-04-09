package com.tianji.api.client.promotion;

import com.tianji.api.client.promotion.fallback.PromotionClientFallback;
import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCouponDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 促销服务
 * @author: hong.jian
 * @date 2024-04-08 10:22
 */
@FeignClient(value = "promotion-service",fallbackFactory = PromotionClientFallback.class)
public interface PromotionClient {
    /**
     * 查询可用用户券方案 - 提供给trade-service远程调用
     * @param dtoList  订单课程信息列表
     * @return  折扣方案集合
     */
    @ApiOperation("查询可用用户券方案")
    @PostMapping("/user-coupons/available")
    List<CouponDiscountDTO> findDiscountSolution(@RequestBody List<OrderCourseDTO> dtoList);


    @ApiOperation("根据券方案计算订单优惠明细")
    @PostMapping("/user-coupons/discount")
    CouponDiscountDTO queryDiscountDetailByOrder(@RequestBody OrderCouponDTO orderCouponDTO);

    @ApiOperation("分页查询我的优惠券接口")
    @GetMapping("/user-coupons/rules")
    List<String> queryDiscountRules(@ApiParam("用户优惠券id集合") @RequestParam("couponIds") List<Long> userCouponIds);
}
