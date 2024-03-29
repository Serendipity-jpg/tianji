package com.tianji.learning.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

@Data
@ApiModel(description = "积分榜单信息")
@Builder
public class PointsBoardItemVO {
    @ApiModelProperty("积分值")
    private Integer points;
    @ApiModelProperty("名次")
    private Integer rank;
    @ApiModelProperty("学生姓名")
    private String name;
}
