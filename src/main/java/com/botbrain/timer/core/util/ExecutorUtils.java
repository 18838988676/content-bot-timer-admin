package com.botbrain.timer.core.util;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * Copyright：botBrain.ai
 * Author: WangMingChao
 * Date: 2019/11/14.
 * Description: 线程池
 */
@Configuration
public class ExecutorUtils {

    @Bean
    public ThreadPoolTaskExecutor checkExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(0);
        executor.setKeepAliveSeconds(10);
        executor.setThreadNamePrefix("admin-job");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }



}
