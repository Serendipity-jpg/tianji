package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;

import java.util.List;

/**
 * <p>
 * 学生课程表 服务类
 * </p>
 *
 * @author Sakura
 */
public interface ILearningLessonService extends IService<LearningLesson> {

    /**
     * 用户批量新增课表
     * @param userId    用户id
     * @param courseIds     课程id列表
     * @return  新增是否成功
     */
    boolean addUserLesson(Long userId, List<Long> courseIds);


    /**
     * 分页查询我的课表
     * @param pageQuery 分页查询参数
     * @return  分页课表信息
     */
    PageDTO<LearningLessonVO> queryMyLessons(PageQuery pageQuery);

    /**
     * 查询用户最近学习课表信息
     * @return  用户最近学习课表信息
     */
    LearningLessonVO now();

    /**
     * 删除课表中课程
     * @param courseIds  课程id列表
     */
    boolean deleteLessionById(Long userId, List<Long> courseIds);

    /**
     * 校验当前用户是否可以学习当前课程
     * @param courseId 课程id
     * @return lessonId，如果是报名了则返回lessonId，否则返回空
     */
    Long isLessonValid(Long courseId);


    /**
     *   根据课程id，查询当前用户的课表中是否有该课程，
     *   如果有该课程则需要返回课程的学习进度、课程有效期等信息。
     * @param courseId  课程id
     * @return  指定课程状态
     */
    LearningLessonVO getLessonInfo(Long courseId);


    /**
     * 统计课程学习人数
     * @param courseId 课程id
     * @return 学习人数
     */
    Integer countLearningLessonByCourse(Long courseId);

    /**
     * 创建学习计划
     * @param courseId 课程id
     * @param weekFreq 每周计划学习频数
     */
    void createLessonPlan(Long courseId, Integer weekFreq);

    /**
     * 分页查询我的学习计划
     * @param pageQuery 分页参数
     */
    LearningPlanPageVO queryMyPlans(PageQuery pageQuery);
}
