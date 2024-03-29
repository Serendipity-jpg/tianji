package com.tianji.learning.mapper;

import com.tianji.learning.domain.po.PointsBoardSeason;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 积分赛季表 Mapper 接口
 * </p>
 *
 * @author Sakura
 */
public interface PointsBoardSeasonMapper extends BaseMapper<PointsBoardSeason> {

    /**
     * 创建上赛季表
     *
     * @param tableName 上赛季表名
     */
    void createPointsBoardTableOfLastSeason(@Param("tableName") String tableName);
}
