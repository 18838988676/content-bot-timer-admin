package com.botbrain.timer.schedule;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/**
 * @Copyright：botBrain.ai
 * @Author: WangMingChao
 * @Date: 2019/11/14.
 * @Description: 定时线程池
 */
@Configuration
public class ExecutorUtils {

    //new
    @Bean("scheduledExecutorServiceCheck")
    public ScheduledExecutorService getScheduledExecutorTaskforCheckOskey() {
        ScheduledExecutorService sc = new ScheduledThreadPoolExecutor(5, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                int num = 0;
                return new Thread(r, "check-osKey-changeThread：" + num++);
            }
        });
        return sc;
    }

    //    @Bean("old")
    public ScheduledExecutorService oldgetScheduledExecutorTaskforCheckOskey() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                int num = 0;
                return new Thread(r, "check-osKey-changeThread：" + num++);
            }
        });
        return executorService;
    }

}