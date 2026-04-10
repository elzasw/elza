package cz.tacr.elza;

import java.util.Map;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
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

import com.google.common.eventbus.EventBus;

import cz.tacr.elza.service.ClientEventDispatcher;
import cz.tacr.elza.websocket.service.WebScoketClientEventService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;


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
    @Value("${elza.hibernate.index.thread_max:2}")
    private int threadMax;

    @Value("${elza.asyncActions.node.threadCount:2}")
    @Min(1)
    @Max(16)
    private int nodeThreadCount;
    
    @Value("${elza.asyncActions.bulk.threadCount:2}")
    @Min(1)
    @Max(16)
    private int bulkThreadCount;

    @Value("${elza.asyncActions.output.threadCount:2}")
    @Min(1)
    @Max(16)
    private int outputThreadCount;

    @Value("${elza.asyncActions.ap.threadCount:2}")
    @Min(1)
    @Max(16)
    private int apThreadCount;

    public static void main(final String[] args) {
        configure();
        SpringApplication.run(ElzaCore.class, args);
    }

    public static void configure() {
        System.setProperty("spring.config.name", "elza");
        System.setProperty("spring.liquibase.database-change-log-table", "DB_DATABASECHANGELOG");
        System.setProperty("spring.liquibase.database-change-log-lock-table", "DB_DATABASECHANGELOGLOCK");
        //System.setProperty("net.sf.jasperreports.compiler.classpath", System.getProperty("java.class.path"));
    }

    @Bean
    public EventBus eventBus() {
        return new EventBus(// exception handler
                (exception, busContext) -> logger.error("Event bus exception: " + busContext.getSubscriberMethod(),
                                                        exception));
    }

//    @Bean
//    public MultipartResolver multipartResolver() {
//        return new StandardServletMultipartResolver();
//    }


    @Bean
    public ScriptEvaluator groovyScriptEvaluator() {
        return new GroovyScriptEvaluator();
    }

    @Bean(name = "threadPoolTaskExecutorBA")
    public ThreadPoolTaskExecutor threadPoolTaskExecutorBulkAction() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize((bulkThreadCount + 1) / 2);
        threadPoolTaskExecutor.setMaxPoolSize(bulkThreadCount);
        threadPoolTaskExecutor.setQueueCapacity(512);
        threadPoolTaskExecutor.afterPropertiesSet();
        
        logger.debug("Creating threadPoolTaskExecutorBA, corePoolSize: {}, maxPoolSize: {}",
        		threadPoolTaskExecutor.getCorePoolSize(),
        		threadPoolTaskExecutor.getMaxPoolSize());        
        return threadPoolTaskExecutor;
    }

    @Bean(name = "threadPoolTaskExecutorAR")
    public ThreadPoolTaskExecutor threadPoolTaskExecutorAsyncRequest() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize((nodeThreadCount + 1) / 2);
        threadPoolTaskExecutor.setMaxPoolSize(nodeThreadCount);
        threadPoolTaskExecutor.setQueueCapacity(512);
        threadPoolTaskExecutor.afterPropertiesSet();
        
        logger.debug("Creating threadPoolTaskExecutorAR, corePoolSize: {}, maxPoolSize: {}",
        		threadPoolTaskExecutor.getCorePoolSize(),
        		threadPoolTaskExecutor.getMaxPoolSize());
        return threadPoolTaskExecutor;
    }

    // Request queue
    // TODO: add to configuration or maybe use with another queue
    @Bean(name = "threadPoolTaskExecutorRQ")
    public ThreadPoolTaskExecutor threadPoolTaskExecutorRequestQueue() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(1);
        threadPoolTaskExecutor.setMaxPoolSize(1);
        threadPoolTaskExecutor.afterPropertiesSet();
        
        logger.debug("Creating threadPoolTaskExecutorRQ, corePoolSize: {}, maxPoolSize: {}",
        		threadPoolTaskExecutor.getCorePoolSize(),
        		threadPoolTaskExecutor.getMaxPoolSize());
        return threadPoolTaskExecutor;
    }

    @Bean(name = "threadPoolTaskExecutorOP")
    public ThreadPoolTaskExecutor threadPoolTaskExecutorOutput() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize((outputThreadCount+1) / 2);
        threadPoolTaskExecutor.setMaxPoolSize(outputThreadCount);
        threadPoolTaskExecutor.afterPropertiesSet();
        
        logger.debug("Creating threadPoolTaskExecutorOP, corePoolSize: {}, maxPoolSize: {}",
        		threadPoolTaskExecutor.getCorePoolSize(),
        		threadPoolTaskExecutor.getMaxPoolSize());
        return threadPoolTaskExecutor;
    }

    @Bean(name = "threadPoolTaskExecutorAP")
    public ThreadPoolTaskExecutor threadPoolTaskExecutorAp() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize((apThreadCount+1) / 2);
        threadPoolTaskExecutor.setMaxPoolSize(apThreadCount);
        threadPoolTaskExecutor.setQueueCapacity(512);
        threadPoolTaskExecutor.afterPropertiesSet();
        
        logger.debug("Creating threadPoolTaskExecutorAP, corePoolSize: {}, maxPoolSize: {}",
        		threadPoolTaskExecutor.getCorePoolSize(),
        		threadPoolTaskExecutor.getMaxPoolSize());
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
        
        logger.debug("Creating threadPoolTaskExecutorHS, corePoolSize: {}, maxPoolSize: {}",
        		threadPoolTaskExecutor.getCorePoolSize(),
        		threadPoolTaskExecutor.getMaxPoolSize());        
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
}
