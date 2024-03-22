package com.tianji.learning.domain.query;

import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.utils.DateUtils;
import com.tianji.learning.enums.QuestionStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 互动问题分页参数
 * @author: hong.jian
 * @date 2024-03-17 15:26
 */
@Data
@ApiModel(description = "互动问题管理端分页参数实体")
public class QuestionAdminPageQuery extends PageQuery {

    @ApiModelProperty(value = "课程名称")
    private String courseName;

    @ApiModelProperty(value = "管理端问题状态：0-未查看，1-已查看")
    private Integer status;

    @ApiModelProperty(value = "提问起始时间")
    @DateTimeFormat(pattern = DateUtils.DEFAULT_DATE_TIME_FORMAT)
    private LocalDateTime beginTime;

    @ApiModelProperty(value = "提问起始时间")
    @DateTimeFormat(pattern = DateUtils.DEFAULT_DATE_TIME_FORMAT)
    private LocalDateTime endTime;
}
