package cz.tacr.elza;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Nastavení pro asynchronní vykonání metod.
 *
 * @author Martin Šlapa
 * @since 26.01.2017
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "syncCacheTaskExecutor")
    public ThreadPoolTaskExecutor myTaskExecutor() {
        return new ThreadPoolTaskExecutor();
    }
}
