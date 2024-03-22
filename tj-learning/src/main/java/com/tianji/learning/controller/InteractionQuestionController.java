package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 互动提问的问题表 控制器
 * </p>
 *
 * @author Sakura
 */
@Api(tags = "InteractionQuestion管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/questions")
public class InteractionQuestionController {

    private final IInteractionQuestionService interactionQuestionService;

    /**
     * 新增互动问题
     */
    @ApiOperation("新增互动问题")
    @PostMapping
    public void addInteractionQuestion(@Validated @RequestBody QuestionFormDTO dto){
        interactionQuestionService.addInteractionQuestion(dto);
    }

    /**
     * 修改互动问题
     * @param dto  修改参数
     * @param id    互动问题id
     */
    @ApiOperation("修改互动问题")
    @PutMapping("/{id}")
    public void updateInteractionQuestion(@RequestBody QuestionFormDTO dto,@PathVariable(value = "id")Long id){
        interactionQuestionService.updateInteractionQuestion(dto,id);
    }

    /**
     * 用户端互动问题分页
     */
    @ApiOperation("用户端互动问题分页")
    @GetMapping("/page")
    public PageDTO<QuestionVO> page(QuestionPageQuery pageQuery){
        return interactionQuestionService.getInterationQuestionByPage(pageQuery);
    }

    /**
     * 用户端获取互动问题详情
     */
    @ApiOperation("用户端获取互动问题详情")
    @GetMapping("/{id}")
    public QuestionVO detail(@PathVariable("id")Long id){
        return interactionQuestionService.detail(id);
    }


    /**
     * 用户删除自身提问接口
     */
    @ApiOperation("删除提问")
    @DeleteMapping("/{id}")
    public boolean deleteMyQuestionById(@PathVariable("id")Long id){
        return interactionQuestionService.deleteMyQuestionById(id);
    }
}
