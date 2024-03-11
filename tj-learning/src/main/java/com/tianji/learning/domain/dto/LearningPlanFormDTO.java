package com.tianji.learning.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;


@Data
@ApiModel(description = "学习计划表单实体")
public class LearningPlanFormDTO {
    @NotNull
    @ApiModelProperty("课程表id")
    private Long courseId;

    @NotNull
    @Range(min = 1, max = 50)
    @ApiModelProperty("每周计划学习频率")
    private Integer freq;
}

