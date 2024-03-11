package com.tianji.learning.service;

import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningRecord;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 学习记录表 服务类
 * </p>
 *
 * @author Sakura
 */
public interface ILearningRecordService extends IService<LearningRecord> {

    /**
     * 查询当前用户指定课程的学习进度
     * @param courseId 课程id
     * @return 课表信息、学习记录及进度信息
     */
    LearningLessonDTO queryLearningRecordByCourse(Long courseId);

    /**
     * 提交学习记录
     * @param dto   学习记录表单
     */
    void submitLearningRecord(LearningRecordFormDTO dto);
}
