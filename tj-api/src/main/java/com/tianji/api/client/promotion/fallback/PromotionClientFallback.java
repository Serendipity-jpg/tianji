package com.tianji.api.client.promotion.fallback;

import com.tianji.api.client.promotion.PromotionClient;
import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCouponDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collections;
import java.util.List;

/**
 * @author: hong.jian
 * @date 2024-04-08 10:24
 */
@Slf4j
public class PromotionClientFallback implements FallbackFactory<PromotionClient> {

    /**
     * @param cause
     * @return
     */
    @Override
    public PromotionClient create(Throwable cause) {
        log.error("远程调用promotion服务失败",cause);
        return new PromotionClient() {
            @Override
            public List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> dtoList) {
                return Collections.emptyList();
            }

            @Override
            public CouponDiscountDTO queryDiscountDetailByOrder(OrderCouponDTO orderCouponDTO) {
                log.error("queryDiscountDetailByOrder error...");
                return null;
            }


            @Override
            public List<String> queryDiscountRules(List<Long> userCouponIds) {
                return Collections.emptyList();
            }
        };
    }
}
