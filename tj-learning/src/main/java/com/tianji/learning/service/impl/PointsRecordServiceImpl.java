package com.tianji.learning.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.mq.SignInMessage;
import com.tianji.learning.service.IPointsRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 *
 * @author Sakura
 */
@Service
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {

    private final PointsRecordMapper recordMapper;

    /**
     * 保存积分
     *
     * @param message    积分消息
     * @param recordType 积分类型
     */
    @Override
    public void addPointRecord(SignInMessage message, PointsRecordType recordType) {
        // 判断积分是否有上限,recordType.getMaxPoints()是否大于0
        boolean hasMaxPoints = recordType.getMaxPoints() > 0;
        Long userId = message.getUserId();  // 获取当前登录用户信息
        // 如果有上限制，查询该用户今日该积分类型已获得的积分数量
        int currentPoints = 0;
        if (hasMaxPoints) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime dayStartTime = DateUtils.getDayStartTime(now);    // 当前开始时间
            LocalDateTime dayEndTime = DateUtils.getDayEndTime(now);    // 当天结束时间
            QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
            // 封装查询wrapper
            wrapper.select("sum(points) as currentPoints")
                    .eq("user_id", userId) // 当前用户
                    .eq("type", recordType)   // 积分类型
                    .between("create_time", dayStartTime, dayEndTime);  // 当天
            Map<String, Object> map = this.getMap(wrapper);
            if (map != null) {
                // 将BigDecimal转为int
                BigDecimal bigDecimal = (BigDecimal) map.get("currentPoints");
                currentPoints = bigDecimal.intValue();
            }
            // 判断积分是否超过上限
            if (currentPoints >= recordType.getMaxPoints()) {
                return;
            }
        }
        // 计算实际应保存的积分数量
        int savePoints = hasMaxPoints ? Math.min(recordType.getMaxPoints() - currentPoints, message.getPoints()) : message.getPoints();
        // 保存积分
        PointsRecord pointsRecord = PointsRecord.builder().userId(userId)
                .type(recordType)
                .points(savePoints)
                .build();
        this.save(pointsRecord);
    }

    /**
     * 获取今日积分
     *
     * @return 今日积分列表
     */
    @Override
    public List<PointsStatisticsVO> getTodayPoints() {
        // 获取当前登录用户
        Long userId = UserContext.getUser();
        // 分类查询当前用户今日所得各类积分
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayStartTime = DateUtils.getDayStartTime(now);    // 当前开始时间
        LocalDateTime dayEndTime = DateUtils.getDayEndTime(now);    // 当天结束时间
        QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
        // 封装查询wrapper
        wrapper.select("type", "sum(points) as tmp")  // 查询类型和该类型积分数(使用临时变量暂存暂存BigDemical类型数据)
                .eq("user_id", userId) // 当前用户
                .between("create_time", dayStartTime, dayEndTime)  // 当天
                .groupBy("type");   // 根据类型分类
        wrapper.select("type", "sum(points) as userId");
        List<PointsRecord> records = this.list(wrapper);
        // 判空
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }
        // 封装到VO并返回
        List<PointsStatisticsVO> voList = records.stream().map(record -> {
            PointsStatisticsVO recordVO = new PointsStatisticsVO();
            recordVO.setPoints(record.getUserId().intValue()); // 该类型今日积分数，临时变量暂存
            recordVO.setType(record.getType().getDesc()); // 该类型名称
            recordVO.setMaxPoints(record.getType().getMaxPoints());   // 该类型上限积分数
            return recordVO;
        }).collect(Collectors.toList());
        return voList;
    }
}
