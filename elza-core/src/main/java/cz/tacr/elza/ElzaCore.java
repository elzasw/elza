package cz.tacr.elza;

import java.util.Map;
import java.util.concurrent.Executor;

import javax.annotation.PostConstruct;
import javax.servlet.MultipartConfigElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.MultipartConfigFactory;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.ScriptEvaluator;
import org.springframework.scripting.groovy.GroovyScriptEvaluator;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import com.google.common.eventbus.EventBus;

import cz.tacr.elza.service.IClientDataChangesService;
import cz.tacr.elza.service.websocket.ClientDataChangesService;


/**
 * Spouštěcí třída pro modul elza-core.
 *
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Configuration
@EntityScan(basePackageClasses = {ElzaCore.class})
@ComponentScan(basePackageClasses = {ElzaCore.class})
@EnableJpaRepositories(basePackageClasses = {ElzaCore.class})
@EnableAutoConfiguration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableTransactionManagement
public class ElzaCore {

    @Autowired
    private ApplicationContext context;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static void main(final String[] args) {
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
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }


    @Bean
    public ScriptEvaluator groovyScriptEvaluator() {
        return new GroovyScriptEvaluator();
    }

    @Bean(name = "threadPoolTaskExecutorBA")
    public ThreadPoolTaskExecutor threadPoolTaskExecutorBulkAction() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(10);
        threadPoolTaskExecutor.setMaxPoolSize(48);
        threadPoolTaskExecutor.setQueueCapacity(512);
        threadPoolTaskExecutor.afterPropertiesSet();
        return threadPoolTaskExecutor;
    }

    @Bean(name = "threadPoolTaskExecutorOG")
    public ThreadPoolTaskExecutor threadPoolTaskExecutorOutputGenerator() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(10);
        threadPoolTaskExecutor.setMaxPoolSize(48);
        threadPoolTaskExecutor.setQueueCapacity(0);
        threadPoolTaskExecutor.afterPropertiesSet();
        return threadPoolTaskExecutor;
    }

    @Bean(name = "threadPoolTaskExecutorRQ")
    public ThreadPoolTaskExecutor threadPoolTaskExecutorRequestQueue() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(3);
        threadPoolTaskExecutor.setMaxPoolSize(10);
        threadPoolTaskExecutor.afterPropertiesSet();
        return threadPoolTaskExecutor;
    }

    @Bean(name = "conformityUpdateTaskExecutor")
    public Executor conformityUpdateTaskExecutor() {
        return threadPoolTaskExecutorBulkAction();
    }

    @Bean
    public IClientDataChangesService clientDataChangesService(){
        return new ClientDataChangesService();
    }


    @PostConstruct
    public void registerEventBusListeners() {
        Map<String, Object> busListenerMap = context.getBeansWithAnnotation(EventBusListener.class);

        for (Map.Entry<String, Object> listenerEntry : busListenerMap.entrySet()) {
            logger.info("Registrace objektu " + listenerEntry.getKey() + " pro příjem událostí.");
            eventBus().register(listenerEntry.getValue());

        }
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize("10MB");
        factory.setMaxRequestSize("10MB");
        return factory.createMultipartConfig();
    }
}
