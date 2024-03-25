package com.tianji.learning.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.mapper.PointsBoardSeasonMapper;
import com.tianji.learning.service.IPointsBoardSeasonService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 积分赛季表 服务实现类
 * </p>
 *
 * @author Sakura
 */
@Service
public class PointsBoardSeasonServiceImpl extends ServiceImpl<PointsBoardSeasonMapper, PointsBoardSeason> implements IPointsBoardSeasonService {

    /**
     * 查询历史赛季列表
     */
    @Override
    public List<PointsBoardSeason> getHistorySeasonList() {
        List<PointsBoardSeason> seasonList = this.lambdaQuery().list();
        if (CollUtil.isEmpty(seasonList)) {  // 判空
            throw new BizIllegalException("查询历史赛季列表失败");
        }
        return seasonList;
    }
}
