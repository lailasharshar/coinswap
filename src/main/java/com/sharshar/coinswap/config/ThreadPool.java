package com.sharshar.coinswap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Defines the thread pool used to individually run the swaps since they can take a really long time.
 *
 * As requests come in, threads will be created up to core pool size and then tasks will be added to the queue until
 * it reaches queue capacity. When the queue is full new threads will be created up to maxPoolSize. Once all the
 * threads are in use and the queue is full tasks will be rejected. As the queue reduces, so does the number of
 * active threads. When we load swap descriptors, we will reset the core pool size to the size of the list so
 * there is essentially one thread for every swap request
 *
 * Created by lsharshar on 9/24/2018.
 */
@Configuration
public class ThreadPool {
	@Bean
	public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(20);
		executor.setQueueCapacity(20);
		executor.setThreadNamePrefix("swap_thread_");
		executor.initialize();
		return executor;
	}
}
