import com.tianji.learning.LearningApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

/**
 * @author: hong.jian
 * @date 2024-03-08 20:33
 */
@SpringBootTest(classes = LearningApplication.class)
public class RedisBitMapTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

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

}


