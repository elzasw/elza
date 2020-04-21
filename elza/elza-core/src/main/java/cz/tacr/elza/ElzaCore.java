package cz.tacr.elza;

import java.util.Map;
import java.util.concurrent.Executor;

import javax.annotation.PostConstruct;
import javax.servlet.MultipartConfigElement;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
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

import cz.tacr.elza.service.ClientEventDispatcher;
import cz.tacr.elza.websocket.service.WebScoketClientEventService;


/**
 * Spouštěcí třída pro modul elza-core.
 *
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

    private static final Logger logger = LoggerFactory.getLogger(ElzaCore.class);

    /**
     * pocet threadu vyhrazenych pro hromadnou indexaci Hibernate Search
     */
    @Value("${elza.hibernate.index.thread_max:4}")
    private int threadMax;

    @Value("${elza.asyncActions.node.threadCount:5}")
    @Min(1)
    @Max(50)
    private int nodeThreadCount;

    @Value("${elza.asyncActions.bulk.threadCount:5}")
    @Min(1)
    @Max(50)
    private int bulkThreadCount;

    @Value("${elza.asyncActions.output.threadCount:5}")
    @Min(1)
    @Max(50)
    private int outputThreadCount;

    public static void main(final String[] args) {
        configure();
        SpringApplication.run(ElzaCore.class, args);
    }

    public static void configure() {
        System.setProperty("spring.config.name", "elza");
        System.setProperty("liquibase.databaseChangeLogTableName", "DB_DATABASECHANGELOG");
        System.setProperty("liquibase.databaseChangeLogLockTableName", "DB_DATABASECHANGELOGLOCK");
    }

    @Bean
    public EventBus eventBus() {
        return new EventBus(// exception handler
                (exception, busContext) -> logger.error("Event bus exception: " + busContext.getSubscriberMethod(),
                                                        exception));
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
        threadPoolTaskExecutor.setCorePoolSize(bulkThreadCount);
        threadPoolTaskExecutor.setMaxPoolSize(48);
        threadPoolTaskExecutor.setQueueCapacity(512);
        threadPoolTaskExecutor.afterPropertiesSet();
        return threadPoolTaskExecutor;
    }

    @Bean(name = "threadPoolTaskExecutorAR")
    public ThreadPoolTaskExecutor threadPoolTaskExecutorAsyncRequest() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(nodeThreadCount);
        threadPoolTaskExecutor.setMaxPoolSize(48);
        threadPoolTaskExecutor.setQueueCapacity(512);
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

    @Bean(name = "threadPoolTaskExecutorOP")
    public ThreadPoolTaskExecutor threadPoolTaskExecutorOutput() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(outputThreadCount);
        threadPoolTaskExecutor.setMaxPoolSize(10);
        threadPoolTaskExecutor.afterPropertiesSet();
        return threadPoolTaskExecutor;
    }

    /**
     * ThreadPoolTaskExecutor pro indexaci Hibernate Search
     */
    @Bean(name = "threadPoolTaskExecutorHS")
    public ThreadPoolTaskExecutor threadPoolTaskExecutorHibernateSearch() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(threadMax);
        // threadPoolTaskExecutor.setMaxPoolSize();
        threadPoolTaskExecutor.setThreadNamePrefix("HibernateSearchIndex-");
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }

    @Bean(name = "conformityUpdateTaskExecutor")
    public Executor conformityUpdateTaskExecutor() {
        return threadPoolTaskExecutorBulkAction();
    }

    @Bean(name = "asyncRequestTaskExecutor")
    public Executor asyncRequestTaskExectutor() {
        return threadPoolTaskExecutorAsyncRequest();
    }

    @Bean
    public ClientEventDispatcher clientEventDispatcher() {
        return new WebScoketClientEventService();
    }

    @PostConstruct
    public void registerEventBusListeners() {
        Map<String, Object> busListenerMap = context.getBeansWithAnnotation(EventBusListener.class);

        for (Map.Entry<String, Object> listenerEntry : busListenerMap.entrySet()) {
            logger.info("Registrace objektu {} pro příjem událostí.", listenerEntry.getKey());
            eventBus().register(listenerEntry.getValue());

        }
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize("25MB");
        factory.setMaxRequestSize("100MB");
        return factory.createMultipartConfig();
    }
}
