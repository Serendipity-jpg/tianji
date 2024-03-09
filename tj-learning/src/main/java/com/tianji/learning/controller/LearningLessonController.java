package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * <p>
 * 学生课程表 控制器
 * </p>
 *
 * @author Sakura
 */
@Api(tags = "LearningLesson管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/lessons")
public class LearningLessonController {

    private final ILearningLessonService learningLessonService;

    @ApiOperation("分页查询我的课表")
    @GetMapping("/page")
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery pageQuery) {
       return learningLessonService.queryMyLessons(pageQuery);
    }

    @ApiOperation("查询当前用户最新的学习课程")
    @GetMapping("/now")
    public LearningLessonVO now(){
        return learningLessonService.now();
    }

    @ApiOperation("删除课表中课程")
    @DeleteMapping("/{courseId}")
    public boolean deleteLessionById(@PathVariable(value = "courseId") Long courseId){
        Long userId = UserContext.getUser();
        // 获取当前登录用户的userId
        if (Objects.isNull(userId)){
            throw new BizIllegalException("用户未登录");
        }
        return learningLessonService.deleteLessionById(userId, CollUtils.singletonList(courseId));
    }


    /**
     * 校验当前用户是否可以学习当前课程
     * @param courseId 课程id
     * @return lessonId，如果是报名了则返回lessonId，否则返回空
     */
    @ApiOperation("校验当前用户是否可以学习当前课程")
    @GetMapping("/{courseId}/valid")
    public Long isLessonValid(@PathVariable("courseId") Long courseId){
        return learningLessonService.isLessonValid(courseId);
    }

    /**
     *   根据课程id，查询当前用户的课表中是否有该课程，
     *   如果有该课程则需要返回课程的学习进度、课程有效期等信息。
     * @param courseId  课程id
     * @return  指定课程状态
     */
    @ApiOperation("查询用户课表中指定课程状态")
    @GetMapping("/{courseId}")
    public LearningLessonVO getLessonInfo(@PathVariable("courseId") Long courseId){
        return learningLessonService.getLessonInfo(courseId);
    }

    /**
     * 统计课程学习人数
     * @param courseId 课程id
     * @return 学习人数
     */
    @ApiOperation("统计课程学习人数")
    @GetMapping("/{courseId}/count")
    Integer countLearningLessonByCourse(@PathVariable("courseId") Long courseId){
        return learningLessonService.countLearningLessonByCourse(courseId);
    }

}
