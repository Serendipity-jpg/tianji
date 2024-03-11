package com.tianji.learning.mapper;

import com.tianji.learning.domain.po.LearningRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * <p>
 * 学习记录表 Mapper 接口
 * </p>
 *
 * @author Sakura
 */
public interface LearningRecordMapper extends BaseMapper<LearningRecord> {

    /**
     * 统计本周内指定课程已学习（finished为1）的小节数
     * @param lessonId  课表id
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return  已学习的小节数
     */
    Integer getWeekLearnedSections(@Param("lessonId")Long lessonId,
                                   @Param("beginTime")LocalDateTime beginTime,
                                   @Param("endTime")LocalDateTime endTime);
}
