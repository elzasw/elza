package cz.tacr.elza;

import com.vaadin.server.AxVaadinServlet;
import cz.req.ax.AxAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import ru.xpoft.vaadin.VaadinMessageSource;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 5.8.15
 */
@Configuration
@Import({ElzaCore.class})
@ComponentScan(basePackageClasses = {ElzaCore.class, AxAction.class})
@EnableAutoConfiguration
@EnableWebMvc
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class ElzaApp extends WebMvcAutoConfiguration {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) {
        SpringApplication.run(ElzaApp.class, args);
    }

    @Bean
    public ServletRegistrationBean vaadinServlet() {
        AxVaadinServlet servlet = new AxVaadinServlet();
        try {
            servlet.getCacheItem("/VAADIN/themes/elza/styles.css");
        } catch (Exception e) {
            logger.error("Error SASS precompile: " + e.getMessage());
        }
        ServletRegistrationBean registration = new ServletRegistrationBean(servlet, "/ui/*", "/VAADIN/*");
        registration.addInitParameter("beanName", "elzaUI");
        registration.addInitParameter("widgetset", "com.vaadin.DefaultWidgetSet");
        registration.addInitParameter("systemMessagesBeanName", "DEFAULT");
        registration.addInitParameter("heartbeatInterval", "28");
        registration.setLoadOnStartup(1);
        return registration;
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
