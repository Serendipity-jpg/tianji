package com.tianji.api.dto.remark;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 统计某业务id下总的点赞数量
 */
@Data
@AllArgsConstructor
@Builder
@ApiModel(description = "点赞数量实体")
public class LikedTimesDTO {
    @ApiModelProperty("点赞业务id")
    private Long bizId;

    @ApiModelProperty("点赞数量")
    private Integer likedTimes;


}
