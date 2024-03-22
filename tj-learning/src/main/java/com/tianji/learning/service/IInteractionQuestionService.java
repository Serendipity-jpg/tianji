package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;

/**
 * <p>
 * 互动提问的问题表 服务类
 * </p>
 *
 * @author Sakura
 */
public interface IInteractionQuestionService extends IService<InteractionQuestion> {

    /**
     * 新增互动问题
     */
    void addInteractionQuestion(QuestionFormDTO dto);

    /**
     * 修改互动问题
     * @param dto  修改参数
     * @param id    互动问题id
     */
    void updateInteractionQuestion(QuestionFormDTO dto,Long id);

    /**
     * 用户端互动问题分页
     */
    PageDTO<QuestionVO> getInterationQuestionByPage(QuestionPageQuery pageQuery);

    /**
     * 用户端获取互动问题详情
     * @param id   互动问题id
     * @return  互动问题详细信息
     */
    QuestionVO detail(Long id);

    /**
     * 管理端获取互动问题详情
     * @param id   互动问题id
     * @return  互动问题详细信息
     */
    QuestionAdminVO detailAdmin(Long id);

    /**
     * 管理端互动问题分页
     */
    PageDTO<QuestionAdminVO> getInterationQuestionByAdminPage(QuestionAdminPageQuery pageQuery);

    /**
     * 用户删除自身提问接口
     */
    boolean deleteMyQuestionById(Long id);

    /**
     * Restful风格，显示/隐藏问题
     *
     * @param id     问题id
     * @param hidden 问题0显示/1隐藏
     * @return  是否成功
     */
    boolean updateQuestionHiddenById(Long id, Boolean hidden);


}
