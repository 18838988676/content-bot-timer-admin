package com.botbrain.timer.core.util;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ScheduledExecutorTask;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * @Copyright：botBrain.ai
 * @Author: WangMingChao
 * @Date: 2019/11/14.
 * @Description: 定时线程池
 */
@Configuration
public class ExecutorUtils {

    @Bean("scheduledExecutorService")
    public ScheduledExecutorService getScheduledExecutorTask(){
        ScheduledExecutorService executorService= Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                int num=0;
                return new Thread(r,"check-osKey-changeThread"+num++);
            }
        });
        return  executorService;
    }

}
