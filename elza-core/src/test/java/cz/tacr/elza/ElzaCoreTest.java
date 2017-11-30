package cz.tacr.elza;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.google.common.eventbus.EventBus;

import cz.tacr.elza.other.ClientEventDispatcherTest;
import cz.tacr.elza.service.ClientEventDispatcher;

@Configuration
@EntityScan(basePackageClasses = { ElzaCore.class })
@ComponentScan(basePackageClasses = { ElzaCore.class })
@EnableJpaRepositories(basePackageClasses = { ElzaCore.class })
@EnableAutoConfiguration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableTransactionManagement
public class ElzaCoreTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) {
        configure();
        SpringApplication.run(ElzaCore.class, args);
    }

    public static void configure() {
        System.setProperty("liquibase.databaseChangeLogTableName", "db_databasechangelog");
        System.setProperty("liquibase.databaseChangeLogLockTableName", "db_databasechangeloglock");
    }

    @Bean
    public EventBus eventBus() {
        return new EventBus((exception, context) -> {
            logger.error("Subscriber exception " + context.getSubscriberMethod(), exception);
        });
    }

    @Bean
    public ClientEventDispatcher clientEventDispatcher(){
        return new ClientEventDispatcherTest();
    }

    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(10);
        threadPoolTaskExecutor.setMaxPoolSize(48);
        threadPoolTaskExecutor.setQueueCapacity(512);
        threadPoolTaskExecutor.afterPropertiesSet();
        return threadPoolTaskExecutor;
    }

    @Bean
    public Executor conformityUpdateTaskExecutor() {
        return new Executor() {
            @Override
            public void execute(final Runnable command) {
                // metodu spustíme v běžícím vlákně
                command.run();
            }
        };
    }
}
