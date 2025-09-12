package lk.ijse.spring.smartplantmanagementsystem.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;

@Configuration
@EnableAsync
@EnableCaching
public class AsyncConfig {
    @Bean(name = "aiExecutor")
    public ThreadPoolTaskExecutor aiExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(1);
        exec.setMaxPoolSize(2);
        exec.setQueueCapacity(10);
        exec.setThreadNamePrefix("AIExec-");
        exec.initialize();
        return exec;
    }

    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager("aiResponses");
        mgr.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofMinutes(5))
                        .maximumSize(100)
        );
        // ← use a boolean, not an enum
        mgr.setAsyncCacheMode(true);
        return mgr;
    }

}
