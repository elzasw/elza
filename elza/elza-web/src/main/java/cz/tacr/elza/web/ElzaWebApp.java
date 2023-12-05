package cz.tacr.elza.web;

import java.util.Locale;


import jakarta.servlet.Filter;
import jakarta.servlet.MultipartConfigElement;
import org.h2.server.web.JakartaWebServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.util.unit.DataSize;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import cz.tacr.elza.ElzaCore;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Servlet/Web container specific configuration
 *
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
    private static final Logger logger = LoggerFactory.getLogger(ElzaWebApp.class);

    @Value("${elza.upload.max_file_size:25MB}")
    protected String uploadMaxFileSize;

    @Value("${elza.upload.max_request_size:100MB}")
    protected String uploadMaxRequestSize;

    /** Vstupní bod. */
    public static void main(final String[] args) {
        ElzaCore.configure();
        //System.setProperty("spring.config.location", "classpath:/elza-ui.yaml");
        SpringApplication.run(ElzaWebApp.class, args);
    }

    @Bean
    @ConditionalOnProperty(prefix = "elza.debug.h2", name = "console", havingValue = "true")
    public ServletRegistrationBean<JakartaWebServlet> h2servletRegistration() {
        logger.info("Initializing H2 Console, listening on /console (elza.debug.h2=true)");
        ServletRegistrationBean<JakartaWebServlet> registration = new ServletRegistrationBean<>(new JakartaWebServlet());
        registration.addUrlMappings("/console/*");
        return registration;
    }

//    @Bean
//    public MultipartResolver multipartResolver() {
//        return new StandardServletMultipartResolver();
//    }

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

    /**
     * Allow to accept forwarded headers (X-Forwarded-Prefix, X-Forwarded-Host,...)
     *
     * This option should be used when Elza is running behind gateway
     *
     * @return Filter pro forwarding
     */
    @Bean
    @ConditionalOnProperty(prefix = "elza.security", name = "acceptForwardedHeaders", havingValue = "true")
    public Filter forwardedHeaderFilter() {
        logger.info("Initializing ForwardedHeaderFilter (acceptForwardedHeaders=true)");
        return new ForwardedHeaderFilter();
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        logger.info("Initializing MultipartConfigFactory with parameters ({}, {})",
                    uploadMaxFileSize,
                    uploadMaxRequestSize);
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.parse(uploadMaxFileSize));
        factory.setMaxRequestSize(DataSize.parse(uploadMaxRequestSize));
        return factory.createMultipartConfig();
    }

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

    /**
     * Bin for customizing Swagger UI
     * 
     * @param serverUrl
     * @param title
     * @param description
     * @param appVersion
     * @return
     */
    @Bean
    public OpenAPI openAPI(@Value("${server.url:/}") String serverUrl,
                           @Value("${api.title:OpenAPI definition}") String title,
                           @Value("${api.description:}") String description,
                           @Value("${api.version:1.0.0}") String appVersion) {
        return new OpenAPI()
                .addServersItem(new Server().url(serverUrl))
                .info(new Info()
                        .title(title)
                        .description(description)
                        .version(appVersion));
    }
}
