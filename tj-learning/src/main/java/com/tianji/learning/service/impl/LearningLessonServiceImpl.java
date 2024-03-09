package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 *
 * @author Sakura
 */
@Service
@RequiredArgsConstructor
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {

    private final CourseClient courseClient;

    private final CatalogueClient catalogueClient;

    @Override
    public boolean addUserLesson(Long userId, List<Long> courseIds) {
        // 通过feign远程调用课程服务，得到课程信息
        List<CourseSimpleInfoDTO> courseSimpleInfoDTOList = courseClient.getSimpleInfoList(courseIds);
        // 封装po实体类，获得有效期
        List<LearningLesson> learningLessonList = courseSimpleInfoDTOList.stream().map(course -> {
            // 有效期计算：当前时间 + 课程有效期
            Integer validDuration = course.getValidDuration();
            // 非空校验
            if (validDuration == null || validDuration < 0) {
                validDuration = 0;
            }
            // 获取当前时间
            LocalDateTime now = LocalDateTime.now();
            return LearningLesson.builder().userId(userId).courseId(course.getId())
                    .createTime(now)
                    .expireTime(now.plusMonths(validDuration)).build();
        }).collect(Collectors.toList());
        // 批量保存
        return this.saveBatch(learningLessonList);
    }

    @Override
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery pageQuery) {
        // 获取当前用户信息
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BizIllegalException("用户未登录");
        }
        // 分页查询我的课表
        Page<LearningLesson> queryPage = pageQuery.toMpPage("latest_learn_time", true);
        Page<LearningLesson> lessonPage = this.lambdaQuery().eq(LearningLesson::getUserId, userId).page(queryPage);
        List<LearningLesson> records = lessonPage.getRecords();
        if (CollUtil.isEmpty(records)) {
            return PageDTO.empty(lessonPage);
        }

        // feign远程调用，给vo的部分课程字段赋值(名称，封面，课程总数等)
        List<Long> courseIds = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toList());
        List<CourseSimpleInfoDTO> courseInfoList = courseClient.getSimpleInfoList(courseIds);
        if (CollUtil.isEmpty(records)) {
            throw new BizIllegalException("关联课程不存在");
        }
        // 封装到map，空间换时间
        Map<Long, CourseSimpleInfoDTO> courseInfoMap = courseInfoList.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        // 将po中的数据封装到vo
        List<LearningLessonVO> learningLessonVOList = records.stream().map(learningLesson -> {
            LearningLessonVO learningLessonVO = new LearningLessonVO();
            BeanUtil.copyProperties(learningLesson, learningLessonVO);
            CourseSimpleInfoDTO infoDTO = courseInfoMap.get(learningLesson.getCourseId());
            if (infoDTO != null) {
                learningLessonVO.setCourseName(infoDTO.getName());
                learningLessonVO.setCourseCoverUrl(infoDTO.getCoverUrl());
                learningLessonVO.setSections(infoDTO.getSectionNum());
            }
            return learningLessonVO;
        }).collect(Collectors.toList());
        // 返回结果
        return PageDTO.of(lessonPage, learningLessonVOList);
    }

    @Override
    public LearningLessonVO now() {
        // 获取当前登录用户
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BizIllegalException("用户未登录");
        }
        // 查询当前用户最近学习课表，降序排序取第一条， status为1表示学习中
        LearningLesson learningLesson = this.lambdaQuery().eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getStatus, 1)
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 1 ").one();
        if (learningLesson == null) {
            return null;
        }
        // 查询当前用户报名的课程数
        Integer courseAmount = this.lambdaQuery().eq(LearningLesson::getUserId, userId).count();
        // feign远程调用查询相关课程的课程名、封面url等
        CourseFullInfoDTO courseInfo = courseClient.getCourseInfoById(learningLesson.getCourseId(), false, false);
        if (Objects.isNull(courseInfo)) {
            throw new BizIllegalException("课程不存在");
        }
        // feign远程调用查询相关小节的小节名称，小节编号
        List<CataSimpleInfoDTO> catalogueInfoList = catalogueClient.batchQueryCatalogue(List.of(learningLesson.getLatestSectionId()));
        if (CollUtil.isEmpty(catalogueInfoList)) {
            throw new BizIllegalException("最新学习小节不存在");
        }
        // 传参的小节id只有一个，所以可直接使用下标0
        CataSimpleInfoDTO catalogueInfo = catalogueInfoList.get(0);
        // 将po数据封装到vo
        LearningLessonVO learningLessonVO = new LearningLessonVO();
        BeanUtil.copyProperties(learningLesson, learningLessonVO);
        learningLessonVO.setCourseAmount(courseAmount); // 课程数量
        learningLessonVO.setCourseName(courseInfo.getName());   // 最近学习课程名称
        learningLessonVO.setCourseCoverUrl(courseInfo.getCoverUrl());   // 最近学习课程封面
        learningLessonVO.setSections(courseInfo.getSectionNum());   // 最近学习课程的章节数
        // 最近学习的小节id和小节名称
        learningLessonVO.setLatestSectionName(catalogueInfo.getName());
        learningLessonVO.setLatestSectionIndex(catalogueInfo.getCIndex());
        // 返回封装的vo
        return learningLessonVO;

    }

    /**
     * 删除课表中课程
     *
     * @param userId    用户id
     * @param courseIds 课程id列表
     */
    @Override
    public boolean deleteLessionById(Long userId, List<Long> courseIds) {
        // 封装userId和courseIds作为查询条件
        LambdaQueryWrapper<LearningLesson> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningLesson::getUserId, userId).in(LearningLesson::getCourseId, courseIds);
        // 删除操作
        return this.remove(wrapper);
    }


    /**
     * 校验当前用户是否可以学习当前课程
     *
     * @param courseId 课程id
     * @return lessonId，如果是报名了则返回lessonId，否则返回空
     */
    @Override
    public Long isLessonValid(Long courseId) {
        Long userId = UserContext.getUser();
        // 获取当前登录用户的userId
        // if (Objects.isNull(userId)) {
        //     throw new BizIllegalException("用户未登录");
        // }
        // 校验用户课表中是否有该课程
        LearningLesson learningLesson = this.lambdaQuery()
                .eq(LearningLesson::getUserId, userId).eq(LearningLesson::getCourseId, courseId).one();
        // 用户课表中没有该课程
        if (learningLesson == null) {
            // throw new BizIllegalException("该课程不在用户课表中");
            return null;
        }
        // 校验课程状态是否有效，即是否已过期，根据过期时间字段是否大于当前时间进行判断
        LocalDateTime expireTime = learningLesson.getExpireTime();
        // 当前时间晚于过期时间，已过期
        if (expireTime != null && LocalDateTime.now().isAfter(expireTime)) {
            // throw new BizIllegalException("该课程已过期");
            return null;
        }
        return learningLesson.getId();
    }

    /**
     * 根据课程id，查询当前用户的课表中是否有该课程，
     * 如果有该课程则需要返回课程的学习进度、课程有效期等信息。
     *
     * @param courseId 课程id
     * @return 指定课程状态
     */
    @Override
    public LearningLessonVO getLessonInfo(Long courseId) {
        // 获取当前登录用户的userId
        Long userId = UserContext.getUser();
        // 校验用户课表中是否有该课程
        LearningLesson learningLesson = this.lambdaQuery().eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId).one();
        // 用户课表中没有该课程
        if (learningLesson == null) {
            // throw new BizIllegalException("该课程不在用户课表中");
            return null;
        }
        // 封装数据到vo
        LearningLessonVO learningLessonVO = LearningLessonVO.builder().id(learningLesson.getId())
                .courseId(learningLesson.getCourseId())
                .status(learningLesson.getStatus())
                .learnedSections(learningLesson.getLearnedSections())
                .createTime(learningLesson.getCreateTime())
                .expireTime(learningLesson.getExpireTime())
                .planStatus(learningLesson.getPlanStatus())
                .build();

        return learningLessonVO;
    }

    /**
     * 统计课程学习人数
     *
     * @param courseId 课程id
     * @return 学习人数
     */
    @Override
    public Integer countLearningLessonByCourse(Long courseId) {
        // lambda查询，根据courseId进行匹配
        return this.lambdaQuery().eq(LearningLesson::getCourseId, courseId).count();
    }

}
