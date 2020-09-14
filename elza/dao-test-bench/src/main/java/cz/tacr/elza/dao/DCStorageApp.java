package cz.tacr.elza.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
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

	@Autowired
	private DCStorageConfig storageConfig;

	@Bean
	public DispatcherServlet dispatcherServlet() {
		return new DispatcherServlet();
	}

	/*
	@Bean
	public ServletRegistrationBean dispatcherServletRegistration() {
		String contextPath = "/" + storageConfig.getRepositoryIdentifier() + "/*";
		ServletRegistrationBean registration = new ServletRegistrationBean(dispatcherServlet(), contextPath);
		registration.setName(DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME);
		return registration;
	}*/

	public static void main(String[] args) {
		System.setProperty("server.port", "8085");
		SpringApplication app = new SpringApplication(DCStorageApp.class);
		app.run(args);
	}
}