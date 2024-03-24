import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.remark.RemarkClient;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.LearningApplication;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.service.ILearningLessonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: hong.jian
 * @date 2024-03-08 20:33
 */
@SpringBootTest(classes = LearningApplication.class)
public class MPTest {


    @Autowired
    private ILearningLessonService lessonService;

    @Autowired
    private RemarkClient remarkClient;

    @Test
    public void test() {
        Page<LearningLesson> page = new Page<>(1, 2);
        LambdaQueryWrapper<LearningLesson> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningLesson::getUserId, 2L);
        wrapper.orderByDesc(LearningLesson::getLatestLearnTime);
        Page<LearningLesson> learningLessonPage = lessonService.page(page, wrapper);
        PageDTO<LearningLesson> pageDTO = PageDTO.of(learningLessonPage);
        System.out.println(pageDTO);
    }

    @Test
    public void test2() {
        PageQuery pageQuery = new PageQuery();
        pageQuery.setIsAsc(false);
        pageQuery.setPageNo(1);
        pageQuery.setPageSize(2);
        pageQuery.setSortBy("latest_learn_time");
        LambdaQueryWrapper<LearningLesson> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningLesson::getUserId, 2L);
        wrapper.orderByDesc(LearningLesson::getLatestLearnTime);
        Page<LearningLesson> learningLessonPage = lessonService.page(pageQuery.toMpPage("latest_learn_time", false));
        PageDTO<LearningLesson> pageDTO = PageDTO.of(learningLessonPage);
        System.out.println(pageDTO);
    }


    @Test
    public void test3() {
        PageQuery pageQuery = new PageQuery();
        pageQuery.setIsAsc(false);
        pageQuery.setPageNo(1);
        pageQuery.setPageSize(2);
        pageQuery.setSortBy("latest_learn_time");
        List<OrderItem> orderItems = new ArrayList<>();
        orderItems.add(new OrderItem(pageQuery.getSortBy(), pageQuery.getIsAsc()));
        Page<LearningLesson> of = Page.of(pageQuery.getPageNo(), pageQuery.getPageSize());
        of.addOrder(orderItems);
        LambdaQueryWrapper<LearningLesson> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningLesson::getUserId, 2L);
        wrapper.orderByDesc(LearningLesson::getLatestLearnTime);
        Page<LearningLesson> learningLessonPage = lessonService.lambdaQuery().eq(LearningLesson::getUserId, 2L)
                .page(of);
        PageDTO<LearningLesson> pageDTO = PageDTO.of(learningLessonPage);
        System.out.println(pageDTO);
    }

    @Test
    private void test4(){
        remarkClient.getLikesStatusByBizIds(List.of(1771175499951771650L));
    }
}


