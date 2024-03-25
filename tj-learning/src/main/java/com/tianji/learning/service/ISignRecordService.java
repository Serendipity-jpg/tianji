package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.vo.SignResultVO;

import java.util.List;

/**
 * <p>
 * 签到 服务类
 * </p>
 *
 * @author Sakura
 */
public interface ISignRecordService  {

    /**
     * 学生签到
     */
    SignResultVO addSignRecords();

    /**
     * 查询用户本月签到记录
     * @return  用户本月签到记录
     */
    List<Byte> selectMonthSignRecords();
}
