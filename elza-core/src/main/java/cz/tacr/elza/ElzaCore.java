package cz.tacr.elza;

import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Spouštěcí třída pro modul elza-core.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Configuration
@EntityScan(basePackageClasses = {ElzaCore.class})
@ComponentScan(basePackageClasses = {ElzaCore.class})
@EnableJpaRepositories(basePackageClasses = {ElzaCore.class})
@EnableAutoConfiguration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class ElzaCore {

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

}
