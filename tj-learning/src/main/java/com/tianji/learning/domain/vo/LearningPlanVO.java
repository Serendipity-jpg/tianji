package com.tianji.learning.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "课程计划信息")
public class LearningPlanVO {

    @ApiModelProperty("主键lessonId")
    private Long id;

    @ApiModelProperty("课程id")
    private Long courseId;

    @ApiModelProperty("课程名称")
    private String courseName;

    @ApiModelProperty("每周计划学习章节数")
    private Integer weekFreq;

    @ApiModelProperty("课程章节数量")
    private Integer sections;

    @ApiModelProperty("本周已学习章节数")
    private Integer weekLearnedSections;

    @ApiModelProperty("总已学习章节数")
    private Integer learnedSections;

    @ApiModelProperty("最近一次学习时间")
    private LocalDateTime latestLearnTime;
}
