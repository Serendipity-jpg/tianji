package com.tianji.learning.controller;

import com.tianji.api.dto.leanring.LearningLessonDTO;
import org.springframework.web.bind.annotation.*;
import com.tianji.learning.service.ILearningRecordService;
import com.tianji.learning.domain.po.LearningRecord;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 学习记录表 控制器
 * </p>
 *
 * @author Sakura
 */
@Api(tags = "LearningRecord管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/learningRecord")
public class LearningRecordController {

    private final ILearningRecordService learningRecordService;


    /**
     * 查询当前用户指定课程的学习进度
     * @param courseId 课程id
     * @return 课表信息、学习记录及进度信息
     */
    @ApiOperation("查询当前用户指定课程的学习进度")
    @GetMapping("/learning-records/course/{courseId}")
    LearningLessonDTO queryLearningRecordByCourse(@PathVariable("courseId") Long courseId){
        return null;
    }


}
