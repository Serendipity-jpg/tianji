package com.tianji.learning.domain.query;

import com.tianji.common.domain.query.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 互动问题分页参数
 * @author: hong.jian
 * @date 2024-03-17 15:26
 */
@Data
@ApiModel(description = "互动问题分页参数实体")
public class QuestionPageQuery extends PageQuery {

    @ApiModelProperty(value = "是否只查询自己提问的互动问题")
    private Boolean onlyMine;

    @ApiModelProperty(value = "课程id")
    private Long courseId;

    @ApiModelProperty(value = "小节id")
    private Long sectionId;


}
