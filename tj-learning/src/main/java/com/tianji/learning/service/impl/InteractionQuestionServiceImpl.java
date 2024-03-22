package com.tianji.learning.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author Sakura
 */
@Service
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {

    private final UserClient userClient;
    private final IInteractionReplyService interactionReplyService;
    private final SearchClient searchClient;
    private final CourseClient courseClient;
    private final CatalogueClient catalogueClient;
    private final CategoryCache categoryCache;

    /**
     * 新增互动问题
     */
    @Override
    public void addInteractionQuestion(QuestionFormDTO dto) {
        InteractionQuestion interactionQuestion = BeanUtils.copyBean(dto, InteractionQuestion.class);
        // 获取登录用户
        Long userId = UserContext.getUser();
        // 若干字段默认赋值(缺省非空属性都有默认值，省略)
        interactionQuestion.setUserId(userId);
        this.save(interactionQuestion);
    }

    /**
     * 修改互动问题
     *
     * @param dto
     */
    @Override
    public void updateInteractionQuestion(QuestionFormDTO dto, Long id) {
        InteractionQuestion interactionQuestion = this.getById(id);
        // 校验参数
        if (interactionQuestion == null || dto.getAnonymity() == null
                || StringUtils.isAllBlank(dto.getTitle(), dto.getDescription())) {
            throw new BizIllegalException("非法参数");
        }
        // 更新前端传来的三个字段
        interactionQuestion.setTitle(dto.getTitle())
                .setDescription(dto.getDescription())
                .setAnonymity(dto.getAnonymity());
        // 更新
        this.updateById(interactionQuestion);
    }

    /**
     * 用户端互动问题分页
     *
     * @param pageQuery 分页参数
     */
    @Override
    public PageDTO<QuestionVO> getInterationQuestionByPage(QuestionPageQuery pageQuery) {
        // 1.参数校验，课程id和小节id不能都为空
        if (pageQuery.getCourseId() == null && pageQuery.getSectionId() == null) {
            throw new BadRequestException("课程id和小节id不能都为空");
        }
        // 获取当前登录用户
        Long userId = UserContext.getUser();
        // 封装查询参数并进行分页查询
        Page<InteractionQuestion> pageInteractionQuestion = lambdaQuery()
                // 设置不查询descript字段（返回实体类中未设置该字段）
                .select(InteractionQuestion.class, tableFieldInfo -> !tableFieldInfo.getProperty().equals("description"))
                .eq(pageQuery.getOnlyMine(), InteractionQuestion::getUserId, userId)   // 是否只查询自己提问的问题
                .eq(pageQuery.getCourseId() != null, InteractionQuestion::getCourseId, pageQuery.getCourseId())
                .eq(pageQuery.getSectionId() != null, InteractionQuestion::getSectionId, pageQuery.getSectionId())
                .eq(InteractionQuestion::getHidden, false)  // 筛选未被管理员隐藏的提问
                .page(pageQuery.toMpPageDefaultSortByCreateTimeDesc());
        // 列表为空，返回空集合
        if (CollUtil.isEmpty(pageInteractionQuestion.getRecords())) {
            return PageDTO.empty(pageInteractionQuestion);
        }
        List<InteractionQuestion> interactionQuestionList = pageInteractionQuestion.getRecords();
        // 根据最新回答id，批量查询回答信息，提高性能
        Set<Long> latestAnswerIds = interactionQuestionList.stream()
                .filter(question -> question.getLatestAnswerId() != null)
                .map(InteractionQuestion::getLatestAnswerId).collect(Collectors.toSet());
        // 批量查询回答信息并封装到Map
        Map<Long, InteractionReply> replyMap = new HashMap<>();
        if (CollUtil.isNotEmpty(latestAnswerIds)) {
            replyMap = interactionReplyService.lambdaQuery()
                    .eq(InteractionReply::getHidden, false)   // 筛选未被管理员隐藏的回答
                    .in(InteractionReply::getId, latestAnswerIds)
                    .list()
                    .stream()
                    .collect(Collectors.toMap(InteractionReply::getId, interactionReply -> interactionReply));
        }
        // 根据提问用户id，批量查询用户信息，提高性能
        Set<Long> userIds = interactionQuestionList.stream()
                .filter(question -> !question.getAnonymity())   // 匿名的不查
                .map(InteractionQuestion::getUserId).collect(Collectors.toSet());
        // 最近回答的userId也要加上(因为需要查询最近回答用户昵称)
        for (InteractionReply reply : replyMap.values()) {
            if (!reply.getAnonymity()) { // 匿名用户不加
                userIds.add(reply.getUserId());
            }
        }
        // 批量查询用户信息并封装到Map，批量查询
        List<UserDTO> userDTOS = userClient.queryUserByIds(userIds);
        Map<Long, UserDTO> userMap = new HashMap<>();
        if (CollUtil.isNotEmpty(latestAnswerIds)) {
            userMap = userDTOS.stream()
                    .collect(Collectors.toMap(UserDTO::getId, userDTO -> userDTO));
        }
        // 封装到VO
        List<QuestionVO> questionVOS = new ArrayList<>();
        for (InteractionQuestion interactionQuestion : interactionQuestionList) {
            // 拷贝实体类属性
            QuestionVO questionVO = BeanUtils.copyBean(interactionQuestion, QuestionVO.class);
            // 如果不是匿名提问
            if (!questionVO.getAnonymity()) {
                UserDTO userDTO = userMap.get(interactionQuestion.getUserId());
                if (userDTO != null) {
                    // 根据userId查询提问的用户昵称和用户头像
                    questionVO.setUserName(userDTO.getUsername());
                    questionVO.setUserIcon(userDTO.getIcon());
                }
            }
            InteractionReply reply = replyMap.get(interactionQuestion.getLatestAnswerId());
            if (reply != null) {
                // 根据latestAnswerId查询最新回答的用户昵称和内容
                if (!reply.getAnonymity()) {
                    UserDTO replyUser = userMap.get(reply.getUserId());
                    if (replyUser != null) {
                        // 昵称赋值
                        questionVO.setLatestReplyUser(replyUser.getName());
                    }
                }
                questionVO.setLatestReplyContent(reply.getContent());
            }
            questionVOS.add(questionVO);
        }
        // 返回
        return PageDTO.of(pageInteractionQuestion, questionVOS);
    }

    /**
     * 用户端获取互动问题详情
     *
     * @param id 互动问题id
     * @return 互动问题详细信息
     */
    @Override
    public QuestionVO detail(Long id) {
        if (id == null) {
            throw new BadRequestException("参数有误");
        }
        InteractionQuestion question = getById(id);
        if (question == null) {
            throw new BadRequestException("问题不存在");
        }
        // 如果管理员设置了隐藏，返回null
        if (question.getHidden()) {
            return null;
        }
        // 拷贝实体类
        QuestionVO questionVO = BeanUtils.copyBean(question, QuestionVO.class);
        if (!question.getAnonymity()) {
            UserDTO userDTO = userClient.queryUserById(question.getUserId());
            if (userDTO != null) {
                questionVO.setUserName(userDTO.getName());
                questionVO.setUserIcon(userDTO.getIcon());
            }
        }
        return questionVO;
    }

    /**
     * 管理端获取互动问题详情
     *
     * @param id 互动问题id
     * @return 互动问题详细信息
     */
    @Override
    public QuestionAdminVO detailAdmin(Long id) {
        if (id == null) {
            throw new BadRequestException("参数有误");
        }
        InteractionQuestion question = getById(id);
        if (question == null) {
            throw new BadRequestException("问题不存在");
        }

        // 拷贝实体类
        QuestionAdminVO questionVO = BeanUtils.copyBean(question, QuestionAdminVO.class);
        if (!question.getAnonymity()) { // 用户信息
            UserDTO userDTO = userClient.queryUserById(question.getUserId());
            if (userDTO != null) {
                questionVO.setUserName(userDTO.getName());  // 用户名称
                questionVO.setUserIcon(userDTO.getIcon());  // 用户头像
            }
        }
        // 查询课程信息
        CourseFullInfoDTO courseInfo = courseClient.getCourseInfoById(question.getCourseId(), true, true);
        if (courseInfo == null) {
            throw new BadRequestException("课程不存在");
        }
        questionVO.setCourseName(courseInfo.getName()); // 课程名称
        questionVO.setCategoryName(categoryCache.getCategoryNames(courseInfo.getCategoryIds())); // 课程三级分类名称
        // 根据教师id列表去用户表查询
        List<UserDTO> teacherDTOS = userClient.queryUserByIds(courseInfo.getTeacherIds());
        if (!CollUtil.isEmpty(teacherDTOS)){
            // 拼接教师名称，形如：教师A/教师B/教师C
            StringBuilder teacherName = new StringBuilder();
            for(UserDTO userDTO: teacherDTOS){
                teacherName.append(userDTO.getName() + "/");
            }
            // 删掉最后一个斜杆
            teacherName.deleteCharAt(teacherName.length()-1);
            questionVO.setTeacherName(teacherName.toString());  // 课程负责教师名称
        }
        // 拼接考试名称
        // 查询章节信息
        List<CataSimpleInfoDTO> catalogueDTOs = catalogueClient
                .batchQueryCatalogue(List.of(question.getChapterId(), question.getSectionId()));
        if (!CollUtil.isEmpty(catalogueDTOs)) { // 列表不为空
            Map<Long, String> cataMap = catalogueDTOs.stream()
                    .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
            questionVO.setChapterName(cataMap.getOrDefault(question.getChapterId(),""));    // 章节名称
            questionVO.setSectionName(cataMap.getOrDefault(question.getSectionId(),""));    // 小节名称

        }
        // 更新问题为已查看
        question.setStatus(QuestionStatus.CHECKED);
        this.updateById(question);
        // 返回结果
        return questionVO;
    }

    /**
     * 管理端互动问题分页
     *
     * @param pageQuery 分页参数
     */
    @Override
    public PageDTO<QuestionAdminVO> getInterationQuestionByAdminPage(QuestionAdminPageQuery pageQuery) {
        // 如果用户传了课程名称参数，则从es中获取该名称对应的课程id
        List<Long> courseIdList = null;
        if (StringUtils.isNotBlank(pageQuery.getCourseName())) {
            // feign远程调用，从es中获取该名称对应的课程id
            courseIdList = searchClient.queryCoursesIdByName(pageQuery.getCourseName());
            // 判断查询结果是否为空
            if (CollUtil.isEmpty(courseIdList)) {
                return PageDTO.empty(0L, 0L);
            }
        }
        // 查询互动问题表
        Page<InteractionQuestion> questionPage = lambdaQuery()
                .eq(pageQuery.getStatus() != null, InteractionQuestion::getStatus, pageQuery.getStatus())
                .ge(pageQuery.getBeginTime() != null, InteractionQuestion::getCreateTime, pageQuery.getBeginTime())
                .le(pageQuery.getEndTime() != null, InteractionQuestion::getCreateTime, pageQuery.getEndTime())
                .in(!CollUtil.isEmpty(courseIdList), InteractionQuestion::getCourseId, courseIdList)   // 实现课程名称模糊查询
                .page(pageQuery.toMpPageDefaultSortByCreateTimeDesc());
        // 查询到的列表为空，则返回空集
        List<InteractionQuestion> records = questionPage.getRecords();
        if (CollUtil.isEmpty(records)) {
            return PageDTO.of(questionPage, Collections.emptyList());
        }
        // 这里用for循环而不是Stream流，减少循环次数
        Set<Long> userIds = new HashSet<>();
        Set<Long> courseIds = new HashSet<>();
        Set<Long> chapterAndSections = new HashSet<>();
        for (InteractionQuestion question : records) {
            userIds.add(question.getUserId());
            courseIds.add(question.getCourseId());
            chapterAndSections.add(question.getChapterId());
            chapterAndSections.add(question.getSectionId());
        }
        // feign远程调用用户服务，获取用户信息
        List<UserDTO> userDTOS = userClient.queryUserByIds(userIds);
        if (CollUtil.isEmpty(userDTOS)) {
            throw new BizIllegalException("用户不存在");
        }
        Map<Long, UserDTO> userMap = userDTOS.stream().collect(Collectors.toMap(UserDTO::getId, userDTO -> userDTO));
        // feign远程调用课程服务，获取课程信息
        List<CourseSimpleInfoDTO> courseDTOs = courseClient.getSimpleInfoList(courseIds);
        if (CollUtil.isEmpty(courseDTOs)) {
            throw new BizIllegalException("课程不存在");
        }
        Map<Long, CourseSimpleInfoDTO> courseMap = courseDTOs.stream()
                .collect(Collectors.toMap(CourseSimpleInfoDTO::getId, courseDTO -> courseDTO));
        // feign远程调用课程服务，获取章节信息
        List<CataSimpleInfoDTO> catalogueDTOs = catalogueClient.batchQueryCatalogue(chapterAndSections);
        if (CollUtil.isEmpty(catalogueDTOs)) {
            throw new BizIllegalException("章节不存在");
        }
        // 封装为章节id，章节名称（需要根据章节id赋值章节名称）
        Map<Long, String> catalogueMap = catalogueDTOs.stream()
                .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        // 封装VO并返回
        List<QuestionAdminVO> voList = new ArrayList<>();
        for (InteractionQuestion record : records) {
            QuestionAdminVO questionAdminVO = BeanUtils.copyBean(record, QuestionAdminVO.class);
            UserDTO userDTO = userMap.get(record.getUserId());
            if (userDTO != null) {
                questionAdminVO.setUserName(userDTO.getName());  // 用户昵称
            }
            CourseSimpleInfoDTO courseDTO = courseMap.get(record.getCourseId());
            if (courseDTO != null) {
                questionAdminVO.setCourseName(courseDTO.getName());    // 课程名称
                // 获取课程的三级分类id，根据三级分类id拼接分类名称
                String categoryName = categoryCache.getCategoryNames(courseDTO.getCategoryIds());
                questionAdminVO.setCategoryName(categoryName);  // 课程所述分类名称
            }
            // 使用getOrDefault防止异常
            questionAdminVO.setChapterName(catalogueMap.getOrDefault(record.getChapterId(), ""));   // 章节名称
            questionAdminVO.setSectionName(catalogueMap.getOrDefault(record.getSectionId(), ""));   // 小节名称
            voList.add(questionAdminVO);
        }
        return PageDTO.of(questionPage, voList);
    }


    /**
     * 用户删除自身提问接口
     *
     * @param id 提问id
     */
    @Override
    @Transactional
    public boolean deleteMyQuestionById(Long id) {
        // 查询问题是否存在
        InteractionQuestion question = getById(id);
        if (question == null) {
            throw new BadRequestException("问题不存在");
        }
        // 获取当前登录用户
        Long userId = UserContext.getUser();
        // 判断是否是当前用户提问的
        if (!question.getUserId().equals(userId)) {
            throw new BizIllegalException("只能删除自己的提问");
        }
        // 如果是则删除问题
        this.removeById(question.getId());
        //  删除问题下的回答及评论
        LambdaQueryWrapper<InteractionReply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InteractionReply::getQuestionId, question.getId());
        interactionReplyService.remove(wrapper);
        return false;
    }

    /**
     * Restful风格，显示/隐藏问题
     *
     * @param id     问题id
     * @param hidden 问题0显示/1隐藏
     * @return  是否成功
     */
    @Override
    public boolean updateQuestionHiddenById(Long id, Boolean hidden) {
        // 查询问题是否存在
        InteractionQuestion question = getById(id);
        if (question == null) {
            throw new BadRequestException("问题不存在");
        }
        // 更新隐藏字段
        question.setHidden(hidden);
        return this.updateById(question);
    }


}
