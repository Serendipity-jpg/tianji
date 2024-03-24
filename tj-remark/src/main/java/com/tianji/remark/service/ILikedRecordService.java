package com.tianji.remark.service;

import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 服务类
 * </p>
 *
 * @author Sakura
 */
public interface ILikedRecordService extends IService<LikedRecord> {

    /**
     * 点赞或者取消赞
     */
    void addLikeRecrod(LikeRecordFormDTO dto);

    /**
     * 批量查询点赞状态
     * 使用set返回可以自动去重
     * @param bizIds  业务id列表
     */
    Set<Long> getLikesStatusByBizIds(List<Long> bizIds);

    /**
     * 从redis取指定类型点赞数量并发送消息到RabbitMQ
     * @param bizType   业务类型
     * @param maxBizSize    每次任务取出的业务score标准
     */
    void readLikedTimesAndSendMessage(String bizType, int maxBizSize);
}
