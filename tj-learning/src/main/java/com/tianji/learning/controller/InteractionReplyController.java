package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import org.springframework.web.bind.annotation.*;
import com.tianji.learning.service.IInteractionReplyService;
import com.tianji.learning.domain.po.InteractionReply;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 互动问题的回答或评论 控制器
 * </p>
 *
 * @author Sakura
 */
@Api(tags = "InteractionReply管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/replies")
public class InteractionReplyController {

    private final IInteractionReplyService interactionReplyService;

    /**
     * 新增评论或回答
     */
    @ApiOperation("新增回答或评论")
    @PostMapping
    public void addReply(@RequestBody ReplyDTO replyDTO){
        interactionReplyService.addReply(replyDTO);
    }

    /**
     * 用户端分页查询回答或评论列表
     */
    @ApiOperation("分页查询回答或评论列表")
    @GetMapping("/page")
    public PageDTO<ReplyVO> page( ReplyPageQuery pageQuery){
        return interactionReplyService.pageUser(pageQuery);
    }


}
