package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.mq.SignInMessage;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import com.tianji.learning.task.LearningRecordDelayTaskHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 学习记录表 服务实现类
 * </p>
 *
 * @author Sakura
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {

    private final ILearningLessonService learningLessonService;

    private final CourseClient courseClient;

    private final LearningRecordDelayTaskHandler taskHandler;

    private final RabbitMqHelper rabbitMqHelper;

    /**
     * 查询当前用户指定课程的学习进度
     *
     * @param courseId 课程id
     * @return 课表信息、学习记录及进度信息
     */
    @Override
    public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {
        // 获取当前登录用户
        Long userId = UserContext.getUser();
        // 根据用户userId和课程courseId获取最近学习的小节id和课表id
        LearningLesson learningLesson = learningLessonService.lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .eq(LearningLesson::getUserId, userId).one();
        // 判NULL防止NPE
        if (Objects.isNull(learningLesson)) {
            throw new BizIllegalException("该课程未加入课表");
        }
        // 根据课表id获取学习记录
        List<LearningRecord> learningRecordList = this.lambdaQuery()
                .eq(LearningRecord::getLessonId, learningLesson.getId()).list();
        // copyToList有判空校验，不再赘余
        List<LearningRecordDTO> learningRecordDTOList = BeanUtil.copyToList(learningRecordList, LearningRecordDTO.class);
        // 封装结果到DTO
        LearningLessonDTO learningLessonDTO = new LearningLessonDTO();
        learningLessonDTO.setId(learningLesson.getId());
        learningLessonDTO.setLatestSectionId(learningLesson.getLatestSectionId());
        learningLessonDTO.setRecords(learningRecordDTOList);
        return learningLessonDTO;

    }

    // /**
    //  * 提交学习记录
    //  *
    //  * @param dto 学习记录表单
    //  */
    // @Override
    // public void submitLearningRecord(LearningRecordFormDTO dto) {
    //     // 获取当前登录用户
    //     Long userId = UserContext.getUser();
    //     LearningRecord learningRecord = BeanUtil.toBean(dto, LearningRecord.class);
    //     // 处理学习记录
    //     Boolean finished;
    //     if (dto.getSectionType().equals(SectionType.EXAM)) {
    //         finished = true;
    //     } else {
    //         // 视频播放进度超过50判定为本节学完
    //         finished = dto.getMoment() * 100 / dto.getDuration() > 50;
    //     }
    //     LocalDateTime now = LocalDateTime.now();
    //     learningRecord.setUserId(userId);
    //     learningRecord.setFinished(finished);
    //     learningRecord.setUpdateTime(now);
    //     // 如果已完成，赋值
    //     learningRecord.setFinishTime(Boolean.TRUE.equals(finished) ? now : null);
    //     // 查询该用户该课程该小节是否已存在
    //     LearningRecord oldRecord = this.lambdaQuery().eq(LearningRecord::getLessonId, dto.getLessonId())
    //             .eq(LearningRecord::getSectionId, dto.getSectionId()).one();
    //     // 根据查询结果来判断是新增还是删除
    //     if (oldRecord != null) {
    //         // 赋值主键id和创建时间字段
    //         learningRecord.setId(oldRecord.getId());
    //         learningRecord.setCreateTime(oldRecord.getCreateTime());
    //         this.updateById(learningRecord);
    //     } else {
    //         // 新增
    //         learningRecord.setCreateTime(now);
    //         this.save(learningRecord);
    //     }
    // }

    /**
     * 提交学习记录
     *
     * @param dto 学习记录表单
     */
    @Override
    public void submitLearningRecord(LearningRecordFormDTO dto) {
        // 获取当前登录用户
        Long userId = UserContext.getUser();
        // 处理学习记录
        boolean finished;
        if (dto.getSectionType().equals(SectionType.EXAM)) {
            // 提交考试记录
            finished = handleExamRecord(userId, dto);
        } else {
            // 提交视频播放记录
            finished = handleVideoRecord(userId, dto);
        }
        // 如果本小节不是首次学完，由于使用了异步延迟任务，不需要往下执行
        if (!finished) {
            return;
        }
        // 处理课表数据
        handleLessonData(dto);
    }

    /**
     * 是否已完成该小节
     * 处理课表数据
     */
    private void handleLessonData(LearningRecordFormDTO dto) {
        // 根据lessonId查询课表记录
        LearningLesson learningLesson = learningLessonService.getById(dto.getLessonId());
        if (learningLesson == null) {
            throw new BizIllegalException("未查询到课表记录");
        }
        // boolean allFinished = false;
        // Integer allSections = 0;
        // 由finished字段可知是否是否为第一次完成小节
        // 使用异步延迟延误后不需要判断了
        // feign远程调用课程服务，查询课程信息的小节综述
        CourseFullInfoDTO courseInfo = courseClient.getCourseInfoById(learningLesson.getCourseId(), false, false);
        if (courseInfo == null) {
            throw new BizIllegalException("未查询到课程记录");
        }
        Integer allSections = courseInfo.getSectionNum();   // 该课程所有小节数
        // 判断该课程所有小节是否已学完
        boolean allFinished = learningLesson.getLearnedSections() + 1 >= allSections;
        // 更新课表信息：课表状态，已学小节数，最近学习小节id，最近学习时间
        learningLessonService.lambdaUpdate()
                // 如果当前小节未学完，更新最近学习小节id和最近学习时间
                .set(LearningLesson::getLatestSectionId, learningLesson.getLatestSectionId() + 1)
                .set(LearningLesson::getLatestLearnTime, dto.getCommitTime())
                // 如果当前小姐已学完，更新已学小节数
                .set(LearningLesson::getLearnedSections, learningLesson.getLearnedSections())
                // 如果该课表所以小节已学完，则更新课表状态为已学完
                .set(allFinished, LearningLesson::getStatus, LessonStatus.FINISHED)
                // 首次学习需要将状态由未开始更新为学习中
                // .set(learningLesson.getLearnedSections() == 0 , LearningLesson::getStatus, LessonStatus.LEARNING)
                .set(learningLesson.getStatus() == LessonStatus.NOT_BEGIN, LearningLesson::getStatus, LessonStatus.LEARNING)
                .eq(LearningLesson::getId, learningLesson.getId())
                .update();

    }

    /**
     * 处理该小节视频播放记录
     *
     * @param userId 用户id
     * @param dto    学习记录DTO
     * @return 是否已完成该小节
     */
    private boolean handleVideoRecord(Long userId, LearningRecordFormDTO dto) {
        // 查询该小节视频进度记录是否已存在，根据lessonId和sectionId进行匹配
        LearningRecord oldRecord = queryOldRecord(dto.getLessonId(), dto.getSectionId());
        // 根据查询结果来判断是新增还是删除
        if (oldRecord == null) {
            // po转dto
            LearningRecord learningRecord = BeanUtil.toBean(dto, LearningRecord.class);
            // 视频播放小节是否已完成根据
            learningRecord.setUserId(userId);
            // 保存到Learning-record表
            // 由于前段每15秒发送提交学习记录请求，所以新增时默认未完成
            boolean result = this.save(learningRecord);
            if (!result) {
                throw new DbException("新增视频播放记录失败");
            }
            // 返回false是因为新增，默认为未完成
            return false;
        }
        // 判断本小节是否是首次完成：之前未完成且视频播放进度大于50%
        boolean isFinished = !oldRecord.getFinished() && dto.getMoment() * 2 >= dto.getDuration();
        // 更新视频播放进度，根据主键id进行匹配
        if (!isFinished) {
            LearningRecord record = LearningRecord.builder()
                    .id(oldRecord.getId())
                    .lessonId(dto.getLessonId())
                    .sectionId(dto.getSectionId())
                    .finished(oldRecord.getFinished())
                    .moment(dto.getMoment())
                    .build();
            // 添加指定学习记录到redis，并提交延迟任务到延迟队列DelayQueue
            taskHandler.addLearningRecordTask(record);
            // 返回，本小节未完成
            return false;
        }
        // 首次完成视频播放，可以增加积分，发送MQ消息实现
        if (isFinished) {
            // 发送MQ消息实现观看学习视频获取积分
            rabbitMqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE,
                    MqConstants.Key.LEARN_SECTION,
                    SignInMessage.of(userId,10));   // 学习一个视频 + 10积分
        }
        boolean result = this.lambdaUpdate()
                .set(LearningRecord::getMoment, dto.getMoment())
                // 只有首次完成视频播放才更新finished字段和finish_time字段
                .set(LearningRecord::getFinished, true)
                .set(LearningRecord::getFinishTime, dto.getCommitTime())
                .eq(LearningRecord::getId, oldRecord.getId())
                .update();
        if (!result) {
            throw new DbException("更新视频播放记录失败");
        }
        // 清理redis相应record
        taskHandler.cleanRecordCache(dto.getLessonId(), dto.getSectionId());
        return true;
    }

    /**
     * 查询指定学习记录是否已存在，
     */
    private LearningRecord queryOldRecord(Long lessonId, Long sectionId) {
        // 查询redis缓存
        LearningRecord cacheRecord = taskHandler.readRecordCache(lessonId, sectionId);
        // redis缓存命中，直接返回
        if (cacheRecord != null) {
            return cacheRecord;
        }
        // redis缓存未命中,查询数据库
        LearningRecord dbRecord = this.lambdaQuery().eq(LearningRecord::getLessonId, lessonId)
                .eq(LearningRecord::getSectionId, sectionId).one();
        // 数据库查询结果为null，表示记录不存在，需要新增学习记录，返回null即可
        if (dbRecord == null) {
            return null;
        }
        // 数据库查询结果写入redis缓存
        taskHandler.writeRecordCache(dbRecord);
        return dbRecord;
    }

    /**
     * 处理该小节考试记录
     *
     * @param userId 用户id
     * @param dto    学习记录DTO
     * @return 是否已完成该小节
     */
    private boolean handleExamRecord(Long userId, LearningRecordFormDTO dto) {
        // po转dto
        LearningRecord learningRecord = BeanUtil.toBean(dto, LearningRecord.class);
        // 考试小节提交后默认已完成
        learningRecord.setUserId(userId)
                .setFinished(true)
                .setFinishTime(dto.getCommitTime());
        // 保存到Learning-record表
        boolean result = this.save(learningRecord);
        if (!result) {
            throw new DbException("新增考试记录失败");
        }
        return true;
    }
}
