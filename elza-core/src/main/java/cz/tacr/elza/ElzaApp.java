package cz.tacr.elza;

import com.google.common.eventbus.EventBus;
import com.vaadin.server.AxVaadinServlet;
import cz.req.ax.AxAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import ru.xpoft.vaadin.VaadinMessageSource;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Configuration
@Import({ElzaConf.class})
@EntityScan(basePackageClasses = {ElzaApp.class})
@ComponentScan(basePackageClasses = {ElzaApp.class, AxAction.class})
@EnableJpaRepositories(basePackageClasses = {ElzaApp.class})
@EnableAutoConfiguration
@EnableWebMvc
//@WebAppConfiguration
public class ElzaApp {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) {
        SpringApplication.run(ElzaApp.class, args);
    }

    @Bean
    public ServletRegistrationBean vaadinServlet() {
        AxVaadinServlet servlet = new AxVaadinServlet();
        ServletRegistrationBean registration = new ServletRegistrationBean(servlet, "/ui/*","/VAADIN/*");
        registration.addInitParameter("beanName", "elzaUI");
        registration.addInitParameter("widgetset", "com.vaadin.DefaultWidgetSet");
        registration.addInitParameter("systemMessagesBeanName", "DEFAULT");
        registration.addInitParameter("heartbeatInterval", "28");
        return registration;
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

    @Bean
    public VaadinMessageSource vaadinMessageSource() {
        return new VaadinMessageSource();
    }

    @Bean
    public OpenEntityManagerInViewFilter openEntityManagerInViewFilter() {
        return new OpenEntityManagerInViewFilter();
    }

    @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer() {
        return container -> container.addInitializers(new ServletRegistrationBean() {
            @Override
            public void onStartup(ServletContext servletContext) throws ServletException {
                servletContext.setInitParameter("productionMode", "false");
            }
        });
    }

}
