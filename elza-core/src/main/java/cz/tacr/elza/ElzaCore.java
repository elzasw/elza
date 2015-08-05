package cz.tacr.elza;

import com.google.common.eventbus.EventBus;
import cz.req.ax.AxAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import ru.xpoft.vaadin.VaadinMessageSource;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Configuration
@EntityScan(basePackageClasses = {ElzaCore.class})
@ComponentScan(basePackageClasses = {ElzaCore.class, AxAction.class})
@EnableJpaRepositories(basePackageClasses = {ElzaCore.class})
@EnableAutoConfiguration
@EnableWebMvc
public class ElzaCore {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) {
        SpringApplication.run(ElzaCore.class, args);
    }

    @Bean
    public ServerProperties servlet() {
        ServerProperties properties = new ServerProperties();
        properties.setServletPath("/api/*");
        return properties;
    }

    @Bean
    public EventBus eventBus() {
        return new EventBus((exception, context) -> {
            logger.error("Subscriber exception " + context.getSubscriberMethod(), exception);
        });
    }

}
