package com.tianji.learning.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.domain.po.PointsBoardSeason;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 积分赛季表 控制器
 * </p>
 *
 * @author Sakura
 */
@Api(tags = "PointsBoardSeason管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/boards/seasons")
public class PointsBoardSeasonController {

    private final IPointsBoardSeasonService pointsBoardSeasonService;

    /**
     * 查询历史赛季列表（无分页）
     */
    @ApiOperation("查询历史赛季列表")
    @GetMapping("/list")
    public List<PointsBoardSeason> getHistorySeasonList(){
        return pointsBoardSeasonService.getHistorySeasonList();
    }

}
