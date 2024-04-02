package com.tianji.promotion.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.service.ICouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 控制器
 * </p>
 *
 * @author Sakura
 */
@Api(tags = "Coupon管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/coupons")
public class CouponController {
    private final ICouponService couponService;

    /**
     * 新增优惠券
     *
     * @param dto 相关参数
     */
    @ApiOperation("新增优惠券")
    @PostMapping
    public void saveCoupon(@Validated @RequestBody CouponFormDTO dto) {
        couponService.saveCoupon(dto);
    }

    /**
     * 管理端分页查询优惠券
     *
     * @param query 分页查询参数
     */
    @ApiOperation("管理端分页查询优惠券")
    @GetMapping("/page")
    public PageDTO<CouponPageVO> couponPage(CouponQuery query) {
        return couponService.couponPage(query);
    }

    /**
     * 发放优惠券
     *
     * @param dto 发放优惠券的日期参数
     * @param id  优惠券id
     */
    @ApiOperation("发放优惠券")
    @PutMapping("/{id}/issue")
    public void issueCoupon(
            @PathVariable("id") Long id,
            @RequestBody @Validated CouponIssueFormDTO dto) {
        couponService.issueCoupon(id, dto);
    }

    /**
     * 修改优惠券
     *
     * @param dto 相关参数
     * @param id 优惠券id
     */
    @ApiOperation("修改优惠券")
    @PutMapping("/{id}")
    public void updateCoupon(@PathVariable("id") Long id,
                             @Validated @RequestBody CouponFormDTO dto) {
        couponService.updateCoupon(dto, id);
    }

    /**
     * 查询优惠券详情，需要封装关联的优惠券范围信息
     * @param id 优惠券id
     */
    @ApiOperation("查询优惠券详情")
    @GetMapping("/{id}")
    public CouponDetailVO queryCouponById(@PathVariable("id") Long id) {
        return couponService.queryCouponById(id);
    }

    /**
     * 删除优惠券，需要删除关联的优惠券范围信息
     * @param id 优惠券id
     */
    @ApiOperation("删除优惠券")
    @DeleteMapping("/{id}")
    public void deleteCoupon(@PathVariable("id") Long id) {
         couponService.deleteCoupon(id);
    }


    /**
     * 暂停发放优惠券
     * @param id 优惠券id
     */
    @ApiOperation("暂停发放优惠券")
    @PutMapping("/{id}/pause")
    public void pauseIssueCoupon(@PathVariable("id") Long id) {
        couponService.pauseIssueCoupon(id);
    }

    /**
     * 查询发放中的优惠券列表 - 用户端
     * @return  发放中的优惠券列表
     */
    @ApiOperation("查询发放中的优惠券列表 - 用户端")
    @GetMapping("list")
    public List<CouponVO> queryIssuingCouponList(){
        return couponService.queryIssuingCouponList();
    }
}
