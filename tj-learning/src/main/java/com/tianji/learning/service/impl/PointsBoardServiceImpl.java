package com.tianji.learning.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.constants.RedisConstants;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.LearningConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardItemVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.service.IPointsBoardService;
import com.tianji.learning.util.TableInfoContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author Sakura
 */
@Service
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {
    private final StringRedisTemplate redisTemplate;
    private final UserClient userClient;
    private final PointsBoardMapper pointsBoardMapper;

    /**
     * 查询赛季积分榜-当前赛季和历史赛季共用
     * season为null或0表示当前赛季
     *
     * @param query 查询参数
     * @return 赛季积分列表
     */
    @Override
    public PointsBoardVO queryPointsBoardList(PointsBoardQuery query) {
        // 获取当前登录用户
        Long season = query.getSeason();    // season为null或0表示当前赛季；否则为历史赛季
        boolean isCurrentBoard = season == null || season == 0;  // 是否为当前赛季
        // 拼接key，用于查询redis中的zset
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + formatter.format(now);
        // 查询当前用户排名，根据query.season区分当前赛季（redis）和历史赛季（db）
        PointsBoardVO boardVO = isCurrentBoard ? queryMyCurrentBoardRank(key) : queryMyHistoryBoardRank(season);
        // 分类查询赛季列表，根据query.season区分当前赛季（redis）和历史赛季（db）
        List<PointsBoard> boards = isCurrentBoard ? queryCurrentBoardRankList(query, key) : queryHistoryBoardRankList(season, query);
        if (CollUtil.isEmpty(boards)) {  // 判空
            return boardVO;
        }
        // 保存用户id，批量查询用户信息
        Set<Long> userIds = boards.stream().map(PointsBoard::getUserId).collect(Collectors.toSet());
        // 封装用户id到用户姓名的映射
        Map<Long, String> userMaps = userClient.queryUserByIds(userIds).stream()
                .collect(Collectors.toMap(UserDTO::getId, userDTO -> userDTO.getName()));
        // 封装VO并返回
        List<PointsBoardItemVO> boardItemVOS = boards.stream().map(board -> PointsBoardItemVO.builder()
                .rank(board.getRank())
                .name(userMaps.getOrDefault(board.getUserId(), "")) // 根据用户id取出用户名称
                .points(board.getPoints())
                .build()).collect(Collectors.toList());
        boardVO.setBoardList(boardItemVOS);
        return boardVO;
    }

    /**
     * 查询当前用户的指定历史赛季积分和排名
     *
     * @param seasonId 赛季id
     * @return 当前用户的指定历史赛季积分和排名
     */
    private PointsBoardVO queryMyHistoryBoardRank(Long seasonId) {
        // 获取当前登录用户
        Long userId = UserContext.getUser();
        // 拼接表名
        String tableName = LearningConstants.POINTS_BOARD_TABLE_PREFIX + seasonId;
        TableInfoContext.setInfo(tableName);    // 设置动态表名
        PointsBoard pointsBoard = this.lambdaQuery()
                .select(PointsBoard::getId,PointsBoard::getPoints,PointsBoard::getUserId)   // 设置查询字段
                .eq(PointsBoard::getUserId,userId).one();
        if (pointsBoard == null){
            throw new BizIllegalException("查询历史赛季用户信息异常");
        }
        TableInfoContext.remove();  // 移除动态表名
        return PointsBoardVO.builder()
                .rank(pointsBoard.getId() == null? 0:pointsBoard.getId().intValue())    // rank排名赋值
                .points(pointsBoard.getPoints() == null? 0:pointsBoard.getPoints()) // points积分赋值
                .build();
    }

    /**
     * 分页查询指定历史赛季积分和排名
     *
     * @param seasonId 赛季id
     * @param query    分页参数
     * @return 指定历史赛季积分和排名
     */
    private List<PointsBoard> queryHistoryBoardRankList(Long seasonId, PointsBoardQuery query) {
        // 拼接表名
        String tableName = LearningConstants.POINTS_BOARD_TABLE_PREFIX + seasonId;
        TableInfoContext.setInfo(tableName);    // 设置动态表名
        // 根据排名降序分页查询
        Page<PointsBoard> boardPage = this.lambdaQuery()
                .select(PointsBoard::getId,PointsBoard::getPoints,PointsBoard::getUserId)   // 设置查询字段，id，user_id和points
                .page(query.toMpPage("id", false));
        if (CollUtil.isEmpty(boardPage.getRecords())){
            throw new BizIllegalException("查询历史赛季排行榜信息异常");
        }
        List<PointsBoard> boardList = boardPage.getRecords().stream().map(board -> {
            // 排名rank暂存在了id字段，
            board.setRank(board.getId() == null ? 0 : board.getId().intValue());
            board.setId(null);
            return board;
        }).collect(Collectors.toList());
        TableInfoContext.remove();  // 移除动态表名
        return boardList;
    }

    /**
     * 查询当前用户的指定历史赛季积分和排名
     *
     * @param seasonId 赛季id
     * @return 当前用户的指定历史赛季积分和排名
     */
    // private PointsBoardVO queryMyHistoryBoardRank(Long seasonId) {
    //     // 获取当前登录用户
    //     Long userId = UserContext.getUser();
    //     // 拼接表名
    //     String tableName = LearningConstants.POINTS_BOARD_TABLE_PREFIX + seasonId;
    //     PointsBoardVO pointsBoardVO = pointsBoardMapper.queryMyHistoryBoardRank(tableName, userId);
    //     return pointsBoardVO;
    // }

    /**
     * 分页查询指定历史赛季积分和排名
     *
     * @param seasonId 赛季id
     * @param query    分页参数
     * @return 指定历史赛季积分和排名
     */
    // private List<PointsBoard> queryHistoryBoardRankList(Long seasonId, PointsBoardQuery query) {
    //     // 拼接表名
    //     String tableName = LearningConstants.POINTS_BOARD_TABLE_PREFIX + seasonId;
    //     // 查询列表
    //     return pointsBoardMapper.queryHistoryBoardRankList(tableName, query.getPageNo() - 1, query.getPageSize());
    // }


    /**
     * 分页查询当前赛季积分和排名列表-redis
     *
     * @param key redis的zset的key
     * @return 当前用户的当前赛季积分和排名
     */
    @Override
    public List<PointsBoard> queryCurrentBoardRankList(PointsBoardQuery query, String key) {
        int start = Math.max(0, query.getPageNo() - 1) * query.getPageSize();    // 开始下标,pageNo大于等于1
        int end = start + query.getPageSize() - 1;    // 结束下标
        // 查询redis中的zset
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
        if (CollUtil.isEmpty(typedTuples)) {    // 判空
            return Collections.emptyList();
        }
        int randIndex = start + 1;  // 排名计数器
        List<PointsBoard> boards = new ArrayList<>();
        // 封装结果并返回
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            // 循环遍历
            Double score = typedTuple.getScore();   // 积分数
            String value = typedTuple.getValue();   // 用户id
            if (StrUtil.isBlank(value) || score == null) {   // 判空防止空指针
                continue;
            }
            PointsBoard board = PointsBoard.builder().rank(randIndex++)
                    .points(score.intValue())
                    .userId(Long.parseLong(value))
                    .build();
            boards.add(board);
        }
        return boards;
    }

    /**
     * 查询当前用户的当前赛季积分和排名
     *
     * @param key redis的zset的key
     * @return 当前用户的当前赛季积分和排名
     */
    private PointsBoardVO queryMyCurrentBoardRank(String key) {
        // 获取当前登录用户
        Long userId = UserContext.getUser();
        // 获取当前赛季积分
        Double score = redisTemplate.opsForZSet().score(key, userId.toString());
        // 获取当前赛季排名,zset默认是按score升序排名，rank升序排名，reverseRank降序排名
        Long rank = redisTemplate.opsForZSet().reverseRank(key, userId.toString());
        // build 返回
        return PointsBoardVO.builder()
                .points(score == null ? 0 : score.intValue())
                .rank(rank == null ? 0 : rank.intValue() + 1)   // 排名从0开始，需要+1
                .build();
    }


}
