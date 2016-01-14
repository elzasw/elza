package cz.tacr.elza;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.google.common.eventbus.EventBus;

import cz.tacr.elza.service.ClientDataChangesServiceTest;
import cz.tacr.elza.service.IClientDataChangesService;
import cz.tacr.elza.service.websocket.ClientDataChangesService;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 3.12.2015
 */
@Configuration
@EntityScan(basePackageClasses = {ElzaCore.class})
@ComponentScan(basePackageClasses = {ElzaCore.class})
@EnableJpaRepositories(basePackageClasses = {ElzaCore.class})
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
        System.setProperty("spring.config.name", "elza");
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
                //metodu spustíme v běžícím vlákně
                command.run();
            }
        };
    }


    @Bean
    public IClientDataChangesService clientDataChangesService(){
        return new ClientDataChangesServiceTest();
    }


}
