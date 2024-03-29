package com.tianji.learning.service;

import com.tianji.learning.domain.po.PointsBoardSeason;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 积分赛季表 服务类
 * </p>
 *
 * @author Sakura
 */
public interface IPointsBoardSeasonService extends IService<PointsBoardSeason> {

    /**
     * 查询历史赛季列表
     */
    List<PointsBoardSeason> getHistorySeasonList();

    /**
     * 创建上赛季表
     */
    void createPointsBoardTableOfLastSeason(String tableName);


}
