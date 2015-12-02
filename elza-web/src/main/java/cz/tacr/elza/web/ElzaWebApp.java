package cz.tacr.elza.web;

import cz.tacr.elza.ElzaCore;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

/**
 * @author by Pavel Stánek, pavel.stanek@marbes.cz.
 * @since 2.12.15
 */
@EnableAutoConfiguration
@Configuration
@EnableConfigurationProperties
@Import({ElzaCore.class})
@ComponentScan(basePackageClasses = {ElzaWebApp.class, ElzaCore.class})
@EnableScheduling
public class ElzaWebApp {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** Vstupní bod. */
    public static void main(final String[] args) {
        ElzaCore.configure();
        System.setProperty("spring.config.location", "classpath:/elza-ui.yaml");
        SpringApplication.run(ElzaWebApp.class, args);
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
}
