package com.tianji.learning.service;

import com.tianji.learning.domain.po.PointsRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mq.SignInMessage;

import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务类
 * </p>
 *
 * @author Sakura
 */
public interface IPointsRecordService extends IService<PointsRecord> {

    /**
     * 保存积分
     * @param message   积分消息
     * @param recordType    积分类型
     */
    void addPointRecord(SignInMessage message, PointsRecordType recordType);

    /**
     * 获取今日积分
     * @return  今日积分列表
     */
    List<PointsStatisticsVO> getTodayPoints();
}
