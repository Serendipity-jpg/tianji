package com.tianji.learning.mapper;

import com.tianji.learning.domain.po.PointsRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 学习积分记录，每个月底清零 Mapper 接口
 * </p>
 *
 * @author Sakura
 */
public interface PointsRecordMapper extends BaseMapper<PointsRecord> {

    /**
     * 生成上赛季积分明细表
     *
     * @param tableName 表名
     */
    void createPointsRecordTableOfLastSeason(@Param("tableName") String tableName);
}
