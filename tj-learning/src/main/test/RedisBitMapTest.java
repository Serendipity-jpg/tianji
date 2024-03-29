import com.tianji.api.constants.RedisConstants;
import com.tianji.learning.LearningApplication;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.service.IPointsRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: hong.jian
 * @date 2024-03-08 20:33
 */
@SpringBootTest(classes = LearningApplication.class)
public class RedisBitMapTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private IPointsRecordService pointsRecordService;

    @Test
    public void test1() {
        // 相当于setbit bm 0 0
        Boolean bm = redisTemplate.opsForValue().setBit("bm", 0, false);
        System.out.println(bm);
    }

    @Test
    public void test2() {
        // 相当于bitfield bm get u8 0
        List<Long> bm = redisTemplate.opsForValue().bitField("bm",
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(8)).valueAt(0));
        System.out.println(bm.get(0));
    }

    /**
     * 批量生成历史榜单数据
     */
    @Test
    public void test3() {
        LocalDate time = LocalDate.now().minusMonths(1);
        DateTimeFormatter yearMonth = DateTimeFormatter.ofPattern("yyyyMM");
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + time.format(yearMonth);
        for (int i = 1; i <= 50; i++) {
            redisTemplate.opsForZSet().add(key, String.valueOf(i), i);
        }
    }

    /**
     * 批量生成历史积分明细数据
     */
    @Test
    public void test4() {
        String tableName = "points_record_5";
        ArrayList<PointsRecord> pointsRecords = new ArrayList<>();
        for(Long userId : List.of(2L,129L,1548889371405492225L)){
            for (int i = 1; i <= 100; i++) {
                PointsRecord pointsRecord = new PointsRecord();
                pointsRecord.setPoints(i);
                pointsRecord.setType(PointsRecordType.QA);
                pointsRecord.setUserId(userId);
                pointsRecords.add(pointsRecord);
            }
        }
        pointsRecordService.saveBatch(pointsRecords);
    }

}


