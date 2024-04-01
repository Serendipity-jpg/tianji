package com.tianji.promotion.task;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.service.ICouponService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: hong.jian
 * @date 2024-03-30 20:46
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class CouponIssueChangeHandler {

    private final ICouponService couponService;

    /**
     * 优惠券定时处理任务
     */
    @XxlJob("couponIssueJobHandler")
    public void changeCouponIssue() {
        LocalDateTime now = LocalDateTime.now();    // 获取当前时间
        int shardTotal = XxlJobHelper.getShardTotal();  // 总分片数
        int shardIndex = XxlJobHelper.getShardIndex();  // 当前分片索引
        // 定时结束发放优惠券
        finishIssueCoupon(now, shardTotal, shardIndex);
        // 定时开始发放优惠券
        issueCoupon(now, shardTotal, shardIndex);
    }

    /**
     * 定时开始发放优惠券
     * 利用XXL-JOB的数据分片功能实现
     */
    private void issueCoupon(LocalDateTime now, int shardTotal, int shardIndex) {
        log.info("开始执行定时开始发放优惠券任务...");
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPageNo(shardIndex + 1);    // 页码（分片索引从0开始，页码从1开始）
        pageQuery.setPageSize(20);   // 页面大小，根据数据量动态调整
        while (true) {
            // 分页查询所有未开始的，发放开始时间早于当前时间的优惠券
            Page<Coupon> couponPage = couponService.lambdaQuery()
                    .eq(Coupon::getStatus, CouponStatus.UN_ISSUE)   // 查询未开始状态的
                    .le(Coupon::getIssueBeginTime, now)     // 发放开始时间早于当前时间
                    .page(pageQuery.toMpPage("id", true));// 根据id进行排序，避免重复处理数据
            List<Coupon> records = couponPage.getRecords();
            if (CollUtil.isEmpty(records)) { // 判空
                break;
            }
            // 更新优惠券状态
            records.stream().forEach(coupon -> coupon.setStatus(CouponStatus.ISSUING));
            // 批量更新优惠券
            couponService.updateBatchById(records);
            // 处理下一页数据
            if (couponPage.hasNext()) {
                // 翻页，数量为总分片数
                pageQuery.setPageNo(pageQuery.getPageNo() + shardTotal);
            } else {
                break;
            }
        }
        log.info("完成定时开始发放优惠券任务...");
    }

    /**
     * 定时结束发放优惠券
     * 利用XXL-JOB的数据分片功能实现
     */
    private void finishIssueCoupon(LocalDateTime now, int shardTotal, int shardIndex) {

        log.info("开始执行定时结束发放优惠券任务...");
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPageNo(shardIndex + 1);    // 页码（分片索引从0开始，页码从1开始）
        pageQuery.setPageSize(20);   // 页面大小，根据数据量动态调整
        while (true) {
            log.info("当前页号：{}", pageQuery.getPageNo());
            // 分页查询所有发放中的，发放结束时间早于当前时间的优惠券
            Page<Coupon> couponPage = couponService.lambdaQuery()
                    .eq(Coupon::getStatus, CouponStatus.ISSUING)   // 查询发放中状态的
                    .le(Coupon::getIssueEndTime, now)     // 发放结束时间早于当前时间
                    .page(pageQuery.toMpPage("id", true));// 根据id进行排序，避免重复处理数据
            List<Coupon> records = couponPage.getRecords();
            if (CollUtil.isEmpty(records)) { // 判空
                break;
            }
            // 更新优惠券状态为已完成
            for (Coupon coupon : records) {
                coupon.setStatus(CouponStatus.FINISHED);
            }
            // 批量更新优惠券
            couponService.updateBatchById(records);
            // 处理下一页数据
            if (couponPage.hasNext()) {
                // 翻页，数量为总分片数
                pageQuery.setPageNo(pageQuery.getPageNo() + shardTotal);
            } else {
                break;
            }

        }
        log.info("完成定时结束发放优惠券任务...");
    }

}
