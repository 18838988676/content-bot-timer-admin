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


    //new
    @Bean("scheduledExecutorServiceAlarm")
    public ScheduledExecutorService getScheduledExecutorTaskforClearAlarmMails() {
        ScheduledExecutorService sc = new ScheduledThreadPoolExecutor(5, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                int num = 0;
                return new Thread(r, "saveAlarmForInterval-thread:" + num++);
            }
        });
        return sc;
    }

    //    @Bean("old")
    public ScheduledExecutorService OldgetScheduledExecutorTaskforClearAlarmMails() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "saveAlarmForInterval-thread");
            }
        });
        return executorService;
    }

}