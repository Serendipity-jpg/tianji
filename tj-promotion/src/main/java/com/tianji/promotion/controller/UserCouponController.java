package com.tianji.promotion.controller;

import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCouponDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.query.UserCouponQuery;
import com.tianji.promotion.domain.vo.CouponVO;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;
import com.tianji.promotion.service.IUserCouponService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    /**
     * 查询可用用户券方案 - 提供给trade-service远程调用
     * @param dtoList  订单课程信息列表
     * @return  折扣方案集合
     */
    @ApiOperation("查询可用用户券方案")
    @PostMapping("/available")
    public List<CouponDiscountDTO> findDiscountSolution(@RequestBody List<OrderCourseDTO> dtoList){
        return userCouponService.findDiscountSolution(dtoList);
    }

    /**
     * 根据券方案计算订单优惠明细
     * @param orderCouponDTO
     * @return
     */
    @ApiOperation("根据券方案计算订单优惠明细")
    @PostMapping("/discount")
    public CouponDiscountDTO queryDiscountDetailByOrder(
            @RequestBody OrderCouponDTO orderCouponDTO){
        return userCouponService.queryDiscountDetailByOrder(orderCouponDTO);
    }



    /**
     *
     */
    @ApiOperation("分页查询我的优惠券接口")
    @GetMapping("/rules")
    public List<String> queryDiscountRules(
            @ApiParam("用户优惠券id集合") @RequestParam("couponIds") List<Long> userCouponIds){
        return userCouponService.queryDiscountRules(userCouponIds);
    }
}
