package com.tianji.learning.domain.vo;

import com.tianji.learning.enums.QuestionStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "管理端问题表信息")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAdminVO {

    @ApiModelProperty(value = "主键，互动问题的id")
    private Long id;

    @ApiModelProperty(value = "互动问题的标题")
    private String title;

    @ApiModelProperty(value = "互动问题的描述")
    private String description;

    @ApiModelProperty(value = "问题下的回答数量")
    private Integer answerTimes;

    @ApiModelProperty(value = "提问时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "是否被隐藏，默认false")
    private Boolean hidden;

    @ApiModelProperty(value = "管理端问题状态：0-未查看，1-已查看")
    private QuestionStatus status;

    @ApiModelProperty(value = "提问学员用户id")
    private Long userId;

    @ApiModelProperty(value = "提问学员用户昵称")
    private String userName;

    @ApiModelProperty(value = "提问学员用户头像")
    private String userIcon;

    @ApiModelProperty(value = "课程名称")
    private String courseName;

    @ApiModelProperty(value = "课程章节名称")
    private String chapterName;

    @ApiModelProperty(value = "课程小节名称")
    private String sectionName;

    @ApiModelProperty(value = "课程三级分类名称，用/分隔")
    private String categoryName;

    // 补充字段，课程负责老师（用于管理端查看提问详情）
    @ApiModelProperty(value = "提问学员用户昵称")
    private String teacherName;


}
