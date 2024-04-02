package com.tianji.promotion.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.query.UserCouponQuery;
import com.tianji.promotion.domain.vo.CouponVO;
import org.springframework.web.bind.annotation.*;
import com.tianji.promotion.service.IUserCouponService;
import com.tianji.promotion.domain.po.UserCoupon;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 控制器
 * </p>
 *
 * @author Sakura
 */
@Api(tags = "UserCoupon管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/user-coupons")
public class UserCouponController {

    private final IUserCouponService userCouponService;

    /**
     * 用户领取优惠券
     * @param id    优惠券id
     */
    @ApiOperation("用户领取优惠券")
    @PostMapping("/{id}/receive")
    public void receiveCoupon(@PathVariable Long id){
        userCouponService.receiveCoupon(id);
    }

    /**
     * 用户凭兑换码兑换优惠券
     * @param code  兑换码
     */
    @ApiOperation("用户凭兑换码兑换优惠券")
    @PostMapping("/{code}/exchange")
    public void exchangeCoupon(@PathVariable String code){
        userCouponService.exchangeCoupon(code);
    }

    /**
     * 查询用户领取的优惠券列表-用户端
     * @param query 分页查询参数
     * @return  用户领取的优惠券列表
     */
    @ApiOperation("用户券列表-用户端")
    @GetMapping("/page")
    public PageDTO<CouponVO> queryUserCouponList(UserCouponQuery query){
       return userCouponService.queryUserCouponList(query);
    }
}
