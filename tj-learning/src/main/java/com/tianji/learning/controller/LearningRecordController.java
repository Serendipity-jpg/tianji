package com.tianji.learning.controller;

import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.tianji.learning.service.ILearningRecordService;
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
@Api(tags = "学习记录相关接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/learning-records")
public class LearningRecordController {

    private final ILearningRecordService learningRecordService;


    /**
     * 查询当前用户指定课程的学习进度
     * @param courseId 课程id
     * @return 课表信息、学习记录及进度信息
     */
    @ApiOperation("查询指定用户指定课程的学习进度")
    @GetMapping("/course/{courseId}")
    public LearningLessonDTO queryLearningRecordByCourse(@PathVariable("courseId") Long courseId){
        return learningRecordService.queryLearningRecordByCourse(courseId);
    }


    /**
     * 提交学习记录
     * Validated 可以对整个对象的属性进行批量校验
     * @param dto   学习记录表单
     */
    @ApiOperation("提交学习记录")
    @PostMapping
    void submitLearningRecord(@RequestBody @Validated LearningRecordFormDTO dto){
         learningRecordService.submitLearningRecord(dto);
    }

}
