package com.tianji.learning.task;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.constants.LearningConstants;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsRecordService;
import com.tianji.learning.util.TableInfoContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 积分明细
 *
 * @author: hong.jian
 * @date 2024-03-27 14:51
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PointsRecordPersistentHandler {

    private final IPointsRecordService pointsRecordService;
    private final IPointsBoardSeasonService pointsBoardSeasonService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 定时任务创建上赛季积分明细表
     */
    // @Scheduled(cron = "0 0 3 1 * ?")    // 一月一个赛季，每月1号的凌晨3点
    // @Scheduled(cron = "0 43 15 27 3 ?")    // 一月一个赛季，每月1号的凌晨3点
    @XxlJob("createPointsRecordTableJob")
    private void createPointsRecordTableOfLastSeason() {
        log.debug("开始创建上赛季积分明细表....");
        // 查询上赛季信息
        PointsBoardSeason boardSeason = getLastPointsBoardSeason();
        // 拼接表名
        String tableName = LearningConstants.POINTS_RECORD_TABLE_PREFIX + boardSeason.getId();
        // 创建上赛季积分明细表，表名示例：points_record_7, 7为赛季id
        pointsRecordService.createPointsRecordTableOfLastSeason(tableName);
        log.debug("创建上赛季积分明细表成功 ....");
    }

    /**
     * 查询上赛季信息
     */
    private PointsBoardSeason getLastPointsBoardSeason() {
        // 获取上个月的当前时间点
        LocalDate time = LocalDate.now().minusMonths(1);
        // 从赛季表查询对应赛季信息
        PointsBoardSeason boardSeason = pointsBoardSeasonService.lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, time)
                .ge(PointsBoardSeason::getEndTime, time)
                .one();
        // 判空
        return boardSeason == null ? new PointsBoardSeason() : boardSeason;
    }

    /**
     * 迁移上赛季积分明细数据到上赛季积分明细表
     * XxlJob注解内容要和任务名称一致
     * 使用XxlJob实现任务分片
     */
    @XxlJob("movePointsRecord2DB")
    public void movePointsRecord2DB() {
        log.debug("开始迁移上赛季积分明细数据到赛季积分明细表...");
        // 查询上赛季信息
        PointsBoardSeason boardSeason = getLastPointsBoardSeason();
        // 拼接表名并存入ThreadLocal
        String targetTableName = LearningConstants.POINTS_RECORD_TABLE_PREFIX + boardSeason.getId();
        log.debug("动态表名：{}", targetTableName);
        // xxl-job分片广播
        int sharedIndex = XxlJobHelper.getShardIndex(); // 当前分片索引
        int shardTotal = XxlJobHelper.getShardTotal();  // 总分片数
        // 分页查询上赛季积分明细数据，先构建分页参数
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPageNo(sharedIndex + 1); // 页码
        pageQuery.setPageSize(50);    // 页面记录数

        while (true) {
            log.debug("当前页:{}", pageQuery.getPageNo());
            TableInfoContext.setInfo(null); // 使用默认表名，points_record
            // 按照id升序查询
            Page<PointsRecord> recordPage = pointsRecordService.page(pageQuery.toMpPage("id",true));
            if (CollUtil.isEmpty(recordPage.getRecords())) {  // 结束循环
                break;
            }
            // 翻页，跳过N个页，N就是分片数量
            pageQuery.setPageNo(pageQuery.getPageNo() + shardTotal);   // 页码+total，跳过N页
            // 使用动态表名，points_record_5
            TableInfoContext.setInfo(targetTableName);
            // 持久化到db相应的赛季表中，批量新增
            pointsRecordService.saveBatch(recordPage.getRecords());
        }
        // 清空ThreadLocal中的数据
        TableInfoContext.remove();
        log.debug("完成迁移上赛季积分明细数据到赛季积分明细表...");


    }

    /**
     * 清除当前赛季积分明细表的历史积分明细
     */
    @XxlJob("clearPointsRecordFromDB")
    public void clearPointsRecordFromDB() {
        log.debug("开始清除当前赛季积分明细表的历史积分明细...");
        // 不指定动态表名，默认使用points_record，wrapper为空，清空所有数据
        pointsRecordService.remove(null);
        log.debug("完成清除当前赛季积分明细表的历史积分明细...");
    }
}
