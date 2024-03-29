package com.tianji.learning.task;

import cn.hutool.core.collection.CollUtil;
import com.tianji.api.constants.RedisConstants;
import com.tianji.learning.constants.LearningConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsBoardService;
import com.tianji.learning.util.TableInfoContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author: hong.jian
 * @date 2024-03-27 14:51
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PointsBoardPersistentHandler {

    private final IPointsBoardSeasonService pointsBoardSeasonService;
    private final IPointsBoardService pointsBoardService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 定时任务创建上赛季榜单表
     */
    // @Scheduled(cron = "0 0 3 1 * ?")    // 一月一个赛季，每月1号的凌晨3点
    // @Scheduled(cron = "0 43 15 27 3 ?")    // 一月一个赛季，每月1号的凌晨3点
    @XxlJob("createPointsBoardTableJob")
    private void createPointsBoardTableOfLastSeason() {
        log.debug("创建上赛季榜单表开始执行....");
        PointsBoardSeason boardSeason = getLastPointsBoardSeason();
        // 创建上赛季榜单表，表名示例：points_board_7, 7为赛季id
        pointsBoardSeasonService.createPointsBoardTableOfLastSeason(LearningConstants.POINTS_BOARD_TABLE_PREFIX + boardSeason.getId());
        log.debug("创建上赛季榜单表成功 ....");
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
     * 持久化上赛季排行榜数据到DB
     * XxlJob注解内容要和任务名称一致
     * 使用XxlJob实现任务分片
     */
    @XxlJob("savePointsBoard2DB")
    public void savePointsBoard2DB() {
        log.debug("开始持久化上赛季排行榜数据到DB...");
        // 查询上赛季信息
        PointsBoardSeason boardSeason = getLastPointsBoardSeason();
        // 拼接表名并存入ThreadLocal
        String tableName = LearningConstants.POINTS_BOARD_TABLE_PREFIX + boardSeason.getId();
        TableInfoContext.setInfo(tableName);
        log.debug("动态表名：{}", tableName);

        // 分页从redis查询上赛季榜单数据
        LocalDate time = LocalDate.now().minusMonths(1);
        DateTimeFormatter yearMonth = DateTimeFormatter.ofPattern("yyyyMM");
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + time.format(yearMonth);
        // xxl-job分片广播
        int sharedIndex = XxlJobHelper.getShardIndex(); // 当前分片索引
        int shardTotal = XxlJobHelper.getShardTotal();  // 总分片数
        // 构建分页参数
        PointsBoardQuery pageQuery = new PointsBoardQuery();
        pageQuery.setPageNo(sharedIndex + 1); // 页码
        pageQuery.setPageSize(1000);    // 页面记录数

        while (true) {
            List<PointsBoard> boards = pointsBoardService.queryCurrentBoardRankList(pageQuery, key);
            if (CollUtil.isEmpty(boards)) {  // 结束循环
                break;
            }
            // 翻页，跳过N个页，N就是分片数量
            pageQuery.setPageNo(pageQuery.getPageNo() + shardTotal);   // 页码+total，跳过N页
            // 字段处理，rank赋值给id并清空
            for (PointsBoard board : boards){
                board.setId(board.getRank().longValue());   // 历史赛季排行榜中id存了rank排名
                board.setRank(null);    // 清空rank
            }
            // 持久化到db相应的赛季表中，批量新增
            pointsBoardService.saveBatch(boards);
        }
        // 清空ThreadLocal中的数据
        TableInfoContext.remove();
        log.debug("完成持久化上赛季排行榜数据到DB...");

    }

    /**
     * 清除redis的历史榜单
     */
    @XxlJob("clearPointsBoardFromRedis")
    public void clearPointsBoardFromRedis(){
        // 拼接key
        LocalDate time = LocalDate.now().minusMonths(1);
        DateTimeFormatter yearMonth = DateTimeFormatter.ofPattern("yyyyMM");
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + time.format(yearMonth);
        // 删除键
        redisTemplate.unlink(key);
    }
}
