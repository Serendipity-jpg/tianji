package com.tianji;

import com.tianji.promotion.PromotionApplication;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.service.ICouponService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: hong.jian
 * @date 2024-03-30 21:28
 */
@SpringBootTest(classes = PromotionApplication.class)
@Slf4j
public class CouponIssueTest {
    @Autowired
    private ICouponService couponService;

    @Test
    public void test() {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> list = couponService.lambdaQuery()
                .eq(Coupon::getStatus, CouponStatus.UN_ISSUE)   // 查询未开始状态的
                .le(Coupon::getIssueBeginTime, now)     // 发放开始时间早于当前时间
                .list();
        for (Coupon coupon : list) {
            log.info(coupon.toString());
        }
    }

    @Test
    public void test2() {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> list = couponService.lambdaQuery()
                .eq(Coupon::getStatus, CouponStatus.ISSUING)   // 查询发放中状态的
                .le(Coupon::getIssueEndTime, now)     // 发放结束时间早于当前时间
                .list();
        for (Coupon coupon : list) {
            log.info(coupon.toString());
        }
    }

    @Test
    public void test3() {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> list = couponService.lambdaQuery().list();
        ArrayList<Coupon> coupons = new ArrayList<>(list);
        for (int i = 1; i <= 100; i++) {
            for (Coupon coupon : coupons) {
                coupon.setName(coupon.getName() + i);
                coupon.setId(null);
            }
            couponService.saveBatch(list);
        }
    }
}
