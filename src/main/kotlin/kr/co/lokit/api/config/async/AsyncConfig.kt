package kr.co.lokit.api.config.async

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.VirtualThreadTaskExecutor
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig {
    @Bean
    fun taskExecutor(): Executor = VirtualThreadTaskExecutor("virtual-")

    @Bean
    fun taskScheduler(): TaskScheduler =
        SimpleAsyncTaskScheduler().apply {
            setVirtualThreads(true)
            setThreadNamePrefix("scheduled-vt-")
        }
}
