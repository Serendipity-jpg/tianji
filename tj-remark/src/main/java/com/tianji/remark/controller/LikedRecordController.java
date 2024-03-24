package com.tianji.remark.controller;

import cn.hutool.log.Log;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.tianji.remark.service.ILikedRecordService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 控制器
 * </p>
 *
 * @author Sakura
 */
@Api(tags = "LikedRecord管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/likes")
public class LikedRecordController {

    private final ILikedRecordService likedRecordService;

    /**
     * 点赞或者取消赞
     */
    @ApiOperation("点赞或取消赞")
    @PostMapping
    public void addLikeRecrod(@RequestBody @Validated LikeRecordFormDTO dto){
        likedRecordService.addLikeRecrod(dto);
    }

    /**
     * 批量查询点赞状态
     * 使用set返回可以自动去重
     */
    @ApiOperation("批量查询点赞状态")
    @GetMapping("/list")
    public Set<Long> getLikesStatusByBizIds(@RequestParam("bizIds")List<Long> bizIds){
        return likedRecordService.getLikesStatusByBizIds(bizIds);
    }
}
