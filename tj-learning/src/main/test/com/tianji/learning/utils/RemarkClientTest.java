package com.tianji.learning.utils;

import com.tianji.api.client.remark.RemarkClient;
import com.tianji.learning.LearningApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * @author: hong.jian
 * @date 2024-03-08 20:33
 */
@SpringBootTest(classes = LearningApplication.class)
public class RemarkClientTest {

    @Autowired
    private RemarkClient remarkClient;

    @Test
    public void test(){
        System.out.println(remarkClient.getLikesStatusByBizIds(List.of(1771175499951771650L)));
    }
}


