package com.botbrain.timer.core.model;

import com.botbrain.timer.core.util.IntervalAlarmService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ContentBotTimerAdminApplicationTests {

    @Autowired
    private IntervalAlarmService hhhhhh;
    @Test
    public void test() {
        System.out.println(hhhhhh);
    }

}
