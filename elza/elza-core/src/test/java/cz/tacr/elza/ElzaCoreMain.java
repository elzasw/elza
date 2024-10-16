package cz.tacr.elza;

import org.h2.server.web.JakartaWebServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import cz.tacr.elza.other.SimpleClientEventDispatcher;
import cz.tacr.elza.service.ClientEventDispatcher;

@Configuration
@EntityScan(basePackageClasses = { ElzaCore.class })
@ComponentScan(basePackageClasses = { ElzaCore.class })
//@EnableJpaRepositories(basePackageClasses = { ElzaCore.class })
@EnableAutoConfiguration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableTransactionManagement
public class ElzaCoreMain {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) {
        configure();
        SpringApplication.run(ElzaCore.class, args);
    }

    public static void configure() {
        System.setProperty("spring.liquibase.database-change-log-table", "db_databasechangelog");
        System.setProperty("spring.liquibase.database-change-log-lock-table", "db_databasechangeloglock");
    }

    /*
    @Bean
    public EventBus eventBusX() {
        return new EventBus((exception, context) -> {
            logger.error("Subscriber exception " + context.getSubscriberMethod(), exception);
        });
    }*/

    @Qualifier("testClientEventDispatcher")
    @Bean
    public ClientEventDispatcher clientEventDispatcher(){
        return new SimpleClientEventDispatcher();
    }

    /*
    @Bean
    public Executor conformityUpdateTaskExecutor() {
        return new Executor() {
            @Override
            public void execute(final Runnable command) {
                // metodu spustíme v běžícím vlákně
                command.run();
            }
        };
    }*/

    @Bean
    @ConditionalOnProperty(prefix = "elza.debug", name = "requests", havingValue = "true")
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(true);
        loggingFilter.setMaxPayloadLength(64000);
        return loggingFilter;
    }

    @Bean
    public ServletRegistrationBean h2servletRegistration() {
        ServletRegistrationBean registration = new ServletRegistrationBean(new JakartaWebServlet());
        registration.addUrlMappings("/console/*");
        return registration;
    }
}
