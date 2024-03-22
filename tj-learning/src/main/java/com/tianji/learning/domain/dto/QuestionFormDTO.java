package com.tianji.learning.domain.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

/**
 * @author: hong.jian
 * @date 2024-03-17 15:26
 */
@Data
@ApiModel(description = "互动问题新增/修改实体")
public class QuestionFormDTO {

    @ApiModelProperty(value = "互动问题标题")
    @NotNull(message = "标题不能为空")
    @Length(min = 1, max = 254, message = "标题长度太长")
    private String title;

    @ApiModelProperty(value = "互动问题描述")
    @NotNull(message = "问题描述不能为空")
    private String description;

    @ApiModelProperty(value = "所属课程id")
    @NotNull(message = "课程id不能为空")
    private Long courseId;

    @ApiModelProperty(value = "所属课程章id")
    @NotNull(message = "章id不能为空")
    private Long chapterId;

    @ApiModelProperty(value = "所属课程小节id")
    @NotNull(message = "小节id不能为空")
    private Long sectionId;

    @ApiModelProperty(value = "是否匿名，默认false")
    private Boolean anonymity;

}
