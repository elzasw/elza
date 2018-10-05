package cz.tacr.elza.dao;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@ComponentScan(basePackageClasses = DCStorageApp.class)
@EnableAutoConfiguration
@EnableConfigurationProperties
@EnableWebMvc
public class DCStorageApp extends SpringBootServletInitializer {

    private static final Map<String, Object> DEFAULT_PROPERTIES = new HashMap<String, Object>();

    // Initialize default properties
    {
        DEFAULT_PROPERTIES.put("dcstorage.repositoryIdentifier", "defaultRepository");
        DEFAULT_PROPERTIES.put("dcstorage.basePath", "storage");
        DEFAULT_PROPERTIES.put("dcstorage.rejectMode", false);
        DEFAULT_PROPERTIES.put("server.port", 8536);

    }

	@Autowired
	private DCStorageConfig storageConfig;

	@Bean
	public DispatcherServlet dispatcherServlet() {
		return new DispatcherServlet();
	}

	@Bean
	public ServletRegistrationBean dispatcherServletRegistration() {
		String contextPath = "/" + storageConfig.getRepositoryIdentifier() + "/*";
		ServletRegistrationBean registration = new ServletRegistrationBean(dispatcherServlet(), contextPath);
		registration.setName(DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME);
		return registration;
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(DCStorageApp.class).properties(DEFAULT_PROPERTIES);
	}

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(DCStorageApp.class);
		app.setDefaultProperties(DEFAULT_PROPERTIES);
		app.run(args);
	}
}