package cz.tacr.elza.web;

import java.util.Locale;

import javax.servlet.MultipartConfigElement;

import org.h2.server.web.WebServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import cz.tacr.elza.ElzaCore;

/**
 * @author by Pavel Stánek, pavel.stanek@marbes.cz.
 * @since 2.12.15
 */
@EnableAutoConfiguration
@Configuration
@EnableConfigurationProperties
@Import({ElzaCore.class})
@ComponentScan(basePackageClasses = { ElzaWebApp.class, ElzaCore.class }, lazyInit = true)
@EnableScheduling
@EnableAsync
public class ElzaWebApp {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** Vstupní bod. */
    public static void main(final String[] args) {
        ElzaCore.configure();
        System.setProperty("spring.config.location", "classpath:/elza-ui.yaml");
        SpringApplication.run(ElzaWebApp.class, args);
    }

    @Bean
    @ConditionalOnProperty(prefix = "elza.debug.h2", name = "console", havingValue = "true")
    public ServletRegistrationBean h2servletRegistration() {
        ServletRegistrationBean registration = new ServletRegistrationBean(new WebServlet());
        registration.addUrlMappings("/console/*");
        return registration;
    }

    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    /**
     * Locale resolver, implicitně je čeština.
     *
     * @return locale resolver
     */
    @Bean
    public LocaleResolver localeResolver() {
        final SessionLocaleResolver slr = new SessionLocaleResolver();
        slr.setDefaultLocale(new Locale("cs", "CZ"));
        return slr;
    }

    /**
     * Resource texty pro překlad.
     *
     * @return resource bundle
     */
    @Bean
    public ReloadableResourceBundleMessageSource messageSource() {
        final ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasenames("classpath:/messages");
        ms.setCacheSeconds(5);
        return ms;
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize("25MB");
        factory.setMaxRequestSize("100MB");
        return factory.createMultipartConfig();
    }
}
