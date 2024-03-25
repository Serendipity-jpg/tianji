package com.tianji.learning.controller;

import com.tianji.learning.domain.vo.PointsStatisticsVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tianji.learning.service.IPointsRecordService;
import com.tianji.learning.domain.po.PointsRecord;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 控制器
 * </p>
 *
 * @author Sakura
 */
@Api(tags = "PointsRecord管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/points")
public class PointsRecordController {

    private final IPointsRecordService pointsRecordService;

    /**
     * 获取今日积分
     */
    @ApiOperation("获取今日积分")
    @GetMapping("/today")
    public List<PointsStatisticsVO> getTodayPoints(){
        return pointsRecordService.getTodayPoints();
    }


}
