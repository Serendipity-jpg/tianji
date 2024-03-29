package com.tianji.learning.controller;

import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tianji.learning.service.IPointsBoardService;
import com.tianji.learning.domain.po.PointsBoard;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 学霸天梯榜 控制器
 * </p>
 *
 * @author Sakura
 */
@Api(tags = "PointsBoard管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/boards")
public class PointsBoardController {

    private final IPointsBoardService pointsBoardService;

    /**
     * 查询赛季积分榜-当前赛季和历史赛季共用
     */
    @ApiOperation("查询赛季积分榜-当前赛季和历史赛季共用")
    @GetMapping
    public PointsBoardVO queryPointsBoardList(PointsBoardQuery query){
        return pointsBoardService.queryPointsBoardList(query);
    }
}
