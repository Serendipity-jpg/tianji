package com.tianji.promotion.service;

import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 兑换码 服务类
 * </p>
 *
 * @author Sakura
 */
public interface IExchangeCodeService extends IService<ExchangeCode> {

    /**
     * 异步生成兑换码
     * @param coupon    优惠券信息
     */
    void asyncGenerateExchangeCode(Coupon coupon);

    /**
     * 判断兑换是否已兑换，并根据自增id 进行更新
     *
     * @param serialNum 自增id，偏移量
     * @param flag      更新值
     */
    boolean hasExchanged(long serialNum, boolean flag);

    /**
     * 根据兑换码id查询优惠券id
     * @param serialNum 兑换码id
     * @return  优惠券id
     */
    Long getExchangeTargetId(Long serialNum);
}
