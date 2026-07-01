package no.nav.sikkerhetstjenesten.loggkamel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Executor pool for parallel processing of split log lines.
 * Uses a bounded thread pool with a queue to prevent thread exhaustion and provide backpressure.
 * When the queue fills up, new tasks will block the caller (CallerRuns policy) until space becomes available.
 */
@Configuration
public class LogLinePublishExecutorConfig {

    @Value("${executor.logLinePublish.corePoolSize:8}")
    private int corePoolSize;

    @Value("${executor.logLinePublish.maxPoolSize:16}")
    private int maxPoolSize;

    @Value("${executor.logLinePublish.queueSize:2000}")
    private int queueSize;

    @Value("${executor.logLinePublish.keepAliveSeconds:60}")
    private long keepAliveSeconds;

    @Bean(name = "logLinePublishPool")
    public ExecutorService logLinePublishPool() {
        // CallerRuns: if queue is full, the caller thread (splitter) blocks until space available
        // This provides natural backpressure without data loss
        RejectedExecutionHandler rejectionHandler = new ThreadPoolExecutor.CallerRunsPolicy();

        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueSize),
                r -> {
                    Thread t = new Thread(r, "loggkamel-log-line-publish-" + Thread.currentThread().getName());
                    t.setDaemon(false);
                    return t;
                },
                rejectionHandler
        );
    }
}
