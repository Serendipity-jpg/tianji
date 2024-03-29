package com.tianji.learning.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * <p>
 * 学霸天梯榜
 * </p>
 *
 * @author Sakura
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@TableName("points_board")
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@ApiModel(value="PointsBoard对象", description="学霸天梯榜")
public class PointsBoard implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "榜单id")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @ApiModelProperty(value = "学生id")
    @TableField("user_id")
    private Long userId;

    @ApiModelProperty(value = "积分值")
    @TableField("points")
    private Integer points;

    @ApiModelProperty(value = "名次，只记录赛季前100")
    @TableField("rank")
    private Integer rank;

    @ApiModelProperty(value = "赛季，例如 1,就是第一赛季，2-就是第二赛季")
    @TableField("season")
    private Integer season;


}
