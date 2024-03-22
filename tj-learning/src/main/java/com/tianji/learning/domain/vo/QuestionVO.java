package com.tianji.learning.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "问题表信息")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionVO {

    @ApiModelProperty(value = "主键，互动问题的id")
    private Long id;

    @ApiModelProperty(value = "互动问题的标题")
    private String title;

    @ApiModelProperty(value = "互动问题的描述")
    private String description;


    @ApiModelProperty(value = "提问学员id")
    private Long userId;

    @ApiModelProperty(value = "提问学员用户昵称")
    private String userName;

    @ApiModelProperty(value = "提问学员用户头像")
    private String userIcon;

    @ApiModelProperty(value = "最新的一个回答的id")
    private Long latestAnswerId;

    @ApiModelProperty(value = "最新的一个回答的用户昵称")
    private String latestReplyUser;

    @ApiModelProperty(value = "最新的一个回答的内容")
    private String latestReplyContent;

    @ApiModelProperty(value = "问题下的回答数量")
    private Integer answerTimes;

    @ApiModelProperty(value = "是否匿名，默认false")
    private Boolean anonymity;

    @ApiModelProperty(value = "提问时间")
    private LocalDateTime createTime;


}
