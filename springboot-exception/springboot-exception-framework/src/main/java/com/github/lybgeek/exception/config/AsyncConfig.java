package com.github.lybgeek.exception.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * 配置异步方法的异常处理器
 */
@Configuration
@EnableAsync// 开启异步支持
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        // 配置线程池（可选，不配置用默认的）
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        // 自定义异步异常处理器
        return (ex, method, params) -> {
            log.error("异步方法[{}]执行失败，参数：{}", method.getName(), Arrays.toString(params), ex);
            // 这里可以加一些额外的处理，比如发送告警邮件、短信等
        };
    }
}
