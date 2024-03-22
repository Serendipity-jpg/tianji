package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动问题的回答或评论 服务实现类
 * </p>
 *
 * @author Sakura
 */
@Service
@RequiredArgsConstructor
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {

    private final InteractionQuestionMapper questionMapper;
    private final UserClient userClient;

    private final String DATA_FIELD_NAME_LIKED_TIME = "liked_times";
    private final String DATA_FIELD_NAME_CREATE_TIME = "create_time";


    /**
     * 新增回答或评论
     */
    @Override
    @Transactional
    public void addReply(ReplyDTO replyDTO) {
        // 拷贝实体
        InteractionReply reply = BeanUtil.toBean(replyDTO, InteractionReply.class);
        if (reply.getAnswerId() == null) {   // 当前是回答的话，不需要target_user_id字段
            reply.setTargetUserId(null);
        }
        // 获取当前登录用户
        Long userId = UserContext.getUser();
        reply.setUserId(userId);
        System.out.println(reply.getUserId());
        // 保存评论或回答
        this.save(reply);
        // 查询关联的问题
        InteractionQuestion question = questionMapper.selectById(reply.getQuestionId());
        if (question == null) {
            throw new BizIllegalException("参数异常");
        }
        // 根据answerId是否为null判断是回答还是评论，如果是需要在`interaction_question`中记录最新一次回答的id
        if (reply.getAnswerId() == null) {    // answerId为null表示当前是回答
            question.setLatestAnswerId(reply.getId());  // 更新问题的最新回答id
            question.setAnswerTimes(question.getAnswerTimes() + 1);   // 该问题的回答数量+1
        } else {     // 如果是评论
            // 获取评论关联的回答
            InteractionReply interactionReply = this.getById(reply.getAnswerId());
            interactionReply.setReplyTimes(interactionReply.getReplyTimes() + 1);   // 该回答的评论数量+1
            // 更新评论关联的回答
            this.updateById(interactionReply);
        }
        // 如果是学生提交，则需要更新问题状态为未查看
        if (replyDTO.getIsStudent()) {
            question.setStatus(QuestionStatus.UN_CHECK);
        }
        // 更新问题
        questionMapper.updateById(question);
        // TODO 尝试更新积分

    }

    /**
     * 用户端分页查询回答或评论列表
     * @param pageQuery 分页参数
     * @return  分页列表
     */
    @Override
    public PageDTO<ReplyVO> pageUser(ReplyPageQuery pageQuery) {
        // 校验问题id和回答id是否都为空
        if (pageQuery.getAnswerId() == null && pageQuery.getQuestionId() == null) {
            throw new BadRequestException("查询参数错误");
        }
        // 分页查询回答或评论列表
        Page<InteractionReply> replyPage = this.lambdaQuery()
                // 如果传问题id就把问题id作查询条件
                .eq(pageQuery.getQuestionId() != null, InteractionReply::getQuestionId, pageQuery.getQuestionId())
                .eq(InteractionReply::getAnswerId, pageQuery.getAnswerId() == null ? 0L : pageQuery.getAnswerId())     // 字段默认值0
                .eq(InteractionReply::getHidden, false)  // 未被管理员隐藏的
                .page(pageQuery.toMpPage(new OrderItem(DATA_FIELD_NAME_LIKED_TIME, false),
                        new OrderItem(DATA_FIELD_NAME_CREATE_TIME, true))); // 按照点赞次数降序排序降序，创建时间升序排序
        List<InteractionReply> records = replyPage.getRecords();
        if (CollUtil.isEmpty(records)) { // 查询不到，返回空集
            return PageDTO.of(replyPage, Collections.emptyList());
        }
        // 关联用户信息，先收集用户id，封装到map
        List<Long> userIds = new ArrayList<>();
        List<Long> targetUserIds = new ArrayList<>();   // 目标用户id
        List<Long> targetReplyIds = new ArrayList<>();   // 目标回复id
        for (InteractionReply reply : records) {
            if (!reply.getAnonymity()) { // 非匿名用户需要查询
                userIds.add(reply.getUserId());
                // userIds.add(reply.getTargetUserId());
            }
            // "target_user_id"字段默认值为0，查询评论时生效
            if (reply.getTargetUserId() != null && reply.getTargetUserId() > 0) {
                targetUserIds.add(reply.getTargetUserId());
            }
            // "target_reply_id"字段默认值为0，查询评论时生效
            if (reply.getTargetReplyId() != null && reply.getTargetReplyId() > 0) {
                targetReplyIds.add(reply.getTargetReplyId());
            }
        }
        // 查询目标回复列表并封装为Map
        Map<Long, InteractionReply> targetReplyMap = new HashMap<>();
        // targetReplyIds不为空，去查询数据库
        if (!CollUtil.isEmpty(targetReplyIds)) {
            // 查询目标评论，并封装为Map
            List<InteractionReply> targetReplies = listByIds(targetReplyIds);
            targetReplyMap = targetReplies.stream().collect(Collectors.toMap(InteractionReply::getId, reply -> reply));

        }
        // 查询用户和目标回复用户并封装为Map
        Map<Long, UserDTO> userMap = getUserDTOMap(userIds);
        Map<Long, UserDTO> targetUserMap = getUserDTOMap(targetUserIds);
        // 保存结果
        List<ReplyVO> replyVOS = new ArrayList<>();
        for (InteractionReply reply : records) {
            ReplyVO replyVO = BeanUtil.toBean(reply, ReplyVO.class);
            UserDTO userDTO = userMap.getOrDefault(reply.getUserId(), null);
            // 如果当前回答或评论匿名，不进行赋值
            if (!replyVO.getAnonymity() && userDTO != null) {
                replyVO.setUserIcon(userDTO.getIcon()); // 回答人头像
                replyVO.setUserName(userDTO.getName()); // 回答人昵称
                replyVO.setUserType(userDTO.getType()); // 回答人类型
            }
            UserDTO targetUserDTO = targetUserMap.getOrDefault(reply.getTargetUserId(), null);
            InteractionReply targetReply = targetReplyMap.getOrDefault(reply.getTargetReplyId(), null);
            // 如果目标评论匿名，不进行赋值
            if (targetReply != null && !targetReply.getAnonymity() && targetUserDTO != null) {    // 目标回复非匿名才赋值
                replyVO.setTargetUserName(targetUserDTO.getName()); // 目标用户昵称
            }
            replyVOS.add(replyVO);
        }
        // 返回结果
        return PageDTO.of(replyPage, replyVOS);
    }

    // feign远程调用：根据userId查询userDTO并封装为Map
    private Map<Long, UserDTO> getUserDTOMap(List<Long> userIds) {
        // feign远程调用，查询用户信息
        List<UserDTO> userDTOS = userClient.queryUserByIds(userIds);
        if (!CollUtil.isEmpty(userDTOS)) {
            // 封装到map
            return userDTOS.stream().collect(Collectors.toMap(UserDTO::getId, userDTO -> userDTO));
        }else{
            // 否则返回空map
            return new HashMap<>();
        }

    }

    /**
     * 评论分页查询
     */
    // private PageDTO<ReplyVO> commentPage(ReplyPageQuery pageQuery) {
    //     Page<InteractionReply> replyPage = this.lambdaQuery()
    //             .eq(InteractionReply::getAnswerId, pageQuery.getAnswerId())
    //             .eq(InteractionReply::getHidden, false)  // 未被管理员隐藏的
    //             .page(pageQuery.toMpPage(new OrderItem(DATA_FIELD_NAME_LIKED_TIME, false),
    //                     new OrderItem(DATA_FIELD_NAME_CREATE_TIME, true))); // 按照点赞次数降序排序降序，创建时间升序排序
    //     List<InteractionReply> records = replyPage.getRecords();
    //     if (CollUtil.isEmpty(records)) { // 查询不到，返回空集
    //         return PageDTO.of(replyPage, Collections.emptyList());
    //     }
    //     // 关联用户信息，先收集用户id，封装到map
    //     List<Long> userIds = new ArrayList<>();
    //     for (InteractionReply reply : records) {
    //         if (!reply.getAnonymity()) { // 非匿名用户需要查询
    //             userIds.add(reply.getUserId());
    //         }
    //         // "target_user_id"字段默认值为0
    //         if (reply.getTargetUserId() != null && reply.getTargetUserId() > 0) {
    //             userIds.add(reply.getTargetUserId());
    //         }
    //     }
    //     // feign远程调用，查询用户信息
    //     Map<Long, UserDTO> userMap = getUserDTOMap(userIds);
    //     List<ReplyVO> replyVOS = records.stream().map(reply -> {
    //         ReplyVO replyVO = BeanUtil.toBean(reply, ReplyVO.class);
    //         UserDTO userDTO = userMap.getOrDefault(replyVO.getUserId(), null);
    //         if (!replyVO.getAnonymity() && userDTO != null) {
    //             replyVO.setUserIcon(userDTO.getIcon()); // 回答人头像
    //             replyVO.setUserName(userDTO.getName()); // 回答人昵称
    //             replyVO.setUserType(userDTO.getType()); // 回答人类型
    //         }
    //         UserDTO targetUserDTO = userMap.getOrDefault(replyVO.getUserId(), null);
    //         if (targetUserDTO != null) {
    //             replyVO.setTargetUserName(targetUserDTO.getName()); // 目标用户昵称
    //         }
    //         return replyVO;
    //     }).collect(Collectors.toList());
    //     // 返回结果
    //     return PageDTO.of(replyPage, replyVOS);
    // }

    /**
     * 回答分页查询
     */
    // private PageDTO<ReplyVO> replyPage(ReplyPageQuery pageQuery) {
    //     Page<InteractionReply> replyPage = this.lambdaQuery().eq(InteractionReply::getAnswerId, 0)   // 回答的answer_id字段为0
    //             .eq(InteractionReply::getQuestionId, pageQuery.getQuestionId())
    //             .eq(InteractionReply::getHidden, false)  // 未被管理员隐藏的
    //             .page(pageQuery.toMpPage(new OrderItem(DATA_FIELD_NAME_LIKED_TIME, false),
    //                     new OrderItem(DATA_FIELD_NAME_CREATE_TIME, true))); // 按照点赞次数降序排序降序，创建时间升序排序
    //     List<InteractionReply> records = replyPage.getRecords();
    //     if (CollUtil.isEmpty(records)) { // 查询不到，返回空集
    //         return PageDTO.of(replyPage, Collections.emptyList());
    //     }
    //     // 关联用户信息，先收集用户id，封装到map
    //     List<Long> userIds = new ArrayList<>();
    //     for (InteractionReply reply : records) {
    //         if (!reply.getAnonymity()) { // 非匿名用户需要查询
    //             userIds.add(reply.getUserId());
    //         }
    //     }
    //     // feign远程调用，查询用户信息
    //     Map<Long, UserDTO> userMap = getUserDTOMap(userIds);
    //     // 封装VO
    //     List<ReplyVO> replyVOS = records.stream().map(reply -> {
    //         ReplyVO replyVO = BeanUtil.toBean(reply, ReplyVO.class);
    //         UserDTO userDTO = userMap.getOrDefault(replyVO.getUserId(), null);
    //         if (!replyVO.getAnonymity() && userDTO != null) {   // 未匿名且能查询到用户信息
    //             replyVO.setUserIcon(userDTO.getIcon()); // 回答人头像
    //             replyVO.setUserName(userDTO.getName()); // 回答人昵称
    //             replyVO.setUserType(userDTO.getType()); // 回答人类型
    //         }
    //         // replyVO.setLiked();  // TODO 是否点过赞，
    //         return replyVO;
    //     }).collect(Collectors.toList());
    //     // 返回结果
    //     return PageDTO.of(replyPage, replyVOS);
    // }


    /**
     * 管理端分页查询回答或评论列表
     * @param pageQuery 分页参数
     * @return  分页列表
     */
    @Override
    public PageDTO<ReplyVO> pageAdmin(ReplyPageQuery pageQuery) {
        // 校验问题id和回答id是否都为空
        if (pageQuery.getAnswerId() == null && pageQuery.getQuestionId() == null) {
            throw new BadRequestException("查询参数错误");
        }
        // 分页查询回答或评论列表
        Page<InteractionReply> replyPage = this.lambdaQuery()
                // 如果传问题id就把问题id作查询条件
                .eq(pageQuery.getQuestionId() != null, InteractionReply::getQuestionId, pageQuery.getQuestionId())
                .eq(InteractionReply::getAnswerId, pageQuery.getAnswerId() == null ? 0L : pageQuery.getAnswerId())     // 字段默认值0
                .page(pageQuery.toMpPage(new OrderItem(DATA_FIELD_NAME_LIKED_TIME, false),
                        new OrderItem(DATA_FIELD_NAME_CREATE_TIME, true))); // 按照点赞次数降序排序降序，创建时间升序排序
        List<InteractionReply> records = replyPage.getRecords();
        if (CollUtil.isEmpty(records)) { // 查询不到，返回空集
            return PageDTO.of(replyPage, Collections.emptyList());
        }
        // 关联用户信息，先收集用户id，封装到map
        List<Long> userIds = new ArrayList<>();
        List<Long> targetUserIds = new ArrayList<>();   // 目标用户id
        List<Long> targetReplyIds = new ArrayList<>();   // 目标回复id
        for (InteractionReply reply : records) {
            if (!reply.getAnonymity()) { // 非匿名用户需要查询
                userIds.add(reply.getUserId());
                // userIds.add(reply.getTargetUserId());
            }
            // "target_user_id"字段默认值为0，查询评论时生效
            if (reply.getTargetUserId() != null && reply.getTargetUserId() > 0) {
                targetUserIds.add(reply.getTargetUserId());
            }
            // "target_reply_id"字段默认值为0，查询评论时生效
            if (reply.getTargetReplyId() != null && reply.getTargetReplyId() > 0) {
                targetReplyIds.add(reply.getTargetReplyId());
            }
        }
        // 查询目标回复列表并封装为Map
        Map<Long, InteractionReply> targetReplyMap = new HashMap<>();
        // targetReplyIds不为空，去查询数据库
        if (!CollUtil.isEmpty(targetReplyIds)) {
            // 查询目标评论，并封装为Map
            List<InteractionReply> targetReplies = listByIds(targetReplyIds);
            targetReplyMap = targetReplies.stream().collect(Collectors.toMap(InteractionReply::getId, reply -> reply));

        }
        // 查询用户和目标回复用户并封装为Map
        Map<Long, UserDTO> userMap = getUserDTOMap(userIds);
        Map<Long, UserDTO> targetUserMap = getUserDTOMap(targetUserIds);
        // 保存结果
        List<ReplyVO> replyVOS = new ArrayList<>();
        for (InteractionReply reply : records) {
            ReplyVO replyVO = BeanUtil.toBean(reply, ReplyVO.class);
            UserDTO userDTO = userMap.getOrDefault(reply.getUserId(), null);
            // 如果当前回答或评论匿名，不进行赋值
            if (!replyVO.getAnonymity() && userDTO != null) {
                replyVO.setUserIcon(userDTO.getIcon()); // 回答人头像
                replyVO.setUserName(userDTO.getName()); // 回答人昵称
                replyVO.setUserType(userDTO.getType()); // 回答人类型
            }
            UserDTO targetUserDTO = targetUserMap.getOrDefault(reply.getTargetUserId(), null);
            InteractionReply targetReply = targetReplyMap.getOrDefault(reply.getTargetReplyId(), null);
            // 如果目标评论匿名，不进行赋值
            if (targetReply != null && !targetReply.getAnonymity() && targetUserDTO != null) {    // 目标回复非匿名才赋值
                replyVO.setTargetUserName(targetUserDTO.getName()); // 目标用户昵称
            }
            replyVOS.add(replyVO);
        }
        // 返回结果
        return PageDTO.of(replyPage, replyVOS);
    }

    /**
     * Restful风格，显示/隐藏回答或评论
     *
     * @param id     回答或评论id
     * @param hidden 回答或评论 0显示/1隐藏
     * @return  是否成功
     */
    @Override
    @Transactional
    public boolean updateReplyHiddenById(Long id, Boolean hidden) {
        InteractionReply reply = getById(id);
        if (reply == null){
            throw new BadRequestException("参数异常");
        }
        // 判断是回答还是评论
        if (reply.getAnswerId() == 0){   // 如果是回答，需要隐藏回答下的评论
            // 根据当前回答reply的id匹配该回答下的评论，修改状态为hidden
            this.lambdaUpdate().set(InteractionReply::getHidden,hidden)
                    .eq(InteractionReply::getAnswerId, reply.getId())
                    .update();

        }
        // 更新显示状态
        reply.setHidden(hidden);
        return updateById(reply);
    }


}
