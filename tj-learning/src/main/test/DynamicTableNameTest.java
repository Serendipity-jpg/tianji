import com.tianji.learning.LearningApplication;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.service.IPointsBoardService;
import com.tianji.learning.util.TableInfoContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author: hong.jian
 * @date 2024-03-28 14:54
 */
@SpringBootTest(classes = LearningApplication.class)
public class DynamicTableNameTest {

    @Autowired
    IPointsBoardService pointsBoardService;

    @Test
    public void test(){
        TableInfoContext.setInfo("points_board_6");
        PointsBoard board = new PointsBoard();
        board.setUserId(1548889371405492225L).setId(11L).setPoints(10);
        pointsBoardService.save(board);
    }

    // @Test
    // public void test2(){
    //     TableInfoContext.setInfo("points_record_5");
    //     PointsRecord pointsRecord = new PointsRecord();
    //     pointsRecord.setPoints()
    // }
}
