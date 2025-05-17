package com.spotistate.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig {

    @Bean(name = ["taskExecutor"])
    fun taskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        // Set core pool size based on number of processors
        executor.corePoolSize = Runtime.getRuntime().availableProcessors()
        // Maximum number of threads
        executor.maxPoolSize = Runtime.getRuntime().availableProcessors() * 2
        // Queue capacity
        executor.queueCapacity = 500
        // Thread name prefix for easier debugging
        executor.setThreadNamePrefix("SpotistateAsync-")
        executor.initialize()
        return executor
    }
}
