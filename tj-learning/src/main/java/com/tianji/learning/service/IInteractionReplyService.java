package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionReply;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;

/**
 * <p>
 * 互动问题的回答或评论 服务类
 * </p>
 *
 * @author Sakura
 */
public interface IInteractionReplyService extends IService<InteractionReply> {

    /**
     * 新增回答或评论
     */
    void addReply(ReplyDTO replyDTO);

    /**
     * 用户端分页查询回答或评论列表
     * @param pageQuery 分页参数
     * @return  分页列表
     */
    PageDTO<ReplyVO> pageUser(ReplyPageQuery pageQuery);


    /**
     * 管理端端分页查询回答或评论列表
     * @param pageQuery 分页参数
     * @return  分页列表
     */
    PageDTO<ReplyVO> pageAdmin(ReplyPageQuery pageQuery);


    /**
     * Restful风格，显示/隐藏问题
     *
     * @param id     问题id
     * @param hidden 问题0显示/1隐藏
     * @return  是否成功
     */
    boolean updateReplyHiddenById(Long id, Boolean hidden);
}
