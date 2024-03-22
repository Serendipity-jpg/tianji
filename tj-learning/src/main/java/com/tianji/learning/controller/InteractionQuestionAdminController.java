package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.service.IInteractionQuestionService;
import com.tianji.learning.service.IInteractionReplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 互动提问的问题表 控制器
 * </p>
 *
 * @author Sakura
 */
@Api(tags = "InteractionQuestion管理（管理端）")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class InteractionQuestionAdminController {

    private final IInteractionQuestionService interactionQuestionService;

    private final IInteractionReplyService interactionReplyService;

    /**
     * 管理端互动问题分页
     */
    @ApiOperation("管理端互动问题分页")
    @GetMapping("/questions/page")
    public PageDTO<QuestionAdminVO> page(QuestionAdminPageQuery pageQuery) {
        return interactionQuestionService.getInterationQuestionByAdminPage(pageQuery);
    }

    /**
     * 管理端互动问题详情
     */
    @ApiOperation("管理端互动问题详情")
    @GetMapping("/questions/{id}")
    public QuestionAdminVO detail(@PathVariable("id") Long id) {
        return interactionQuestionService.detailAdmin(id);
    }


    /**
     * Restful风格，显示/隐藏问题
     *
     * @param id     问题id
     * @param hidden 问题0显示/1隐藏
     * @return  是否成功
     */
    @ApiOperation("显示/隐藏问题")
    @PutMapping("/questions/{id}/hidden/{hidden}")
    public boolean updateQuestionHiddenById(@PathVariable("id") Long id,
                                    @PathVariable("hidden") Boolean hidden) {
        return interactionQuestionService.updateQuestionHiddenById(id, hidden);
    }

    /**
     * 管理端分页查询回答或评论列表
     */
    @ApiOperation("分页查询回答或评论列表")
    @GetMapping("/replies/page")
    public PageDTO<ReplyVO> page(ReplyPageQuery pageQuery){
        return interactionReplyService.pageAdmin(pageQuery);
    }

    /**
     * Restful风格，显示/隐藏回答或评论
     *
     * @param id     回答或评论id
     * @param hidden 回答或评论 0显示/1隐藏
     * @return  是否成功
     */
    @ApiOperation("显示/隐藏回答或评论")
    @PutMapping("/replies/{id}/hidden/{hidden}")
    public boolean updateReplyHiddenById(@PathVariable("id") Long id,
                                    @PathVariable("hidden") Boolean hidden) {
        return interactionReplyService.updateReplyHiddenById(id, hidden);
    }
}
