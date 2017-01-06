package cz.tacr.elza.dao;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import cz.tacr.elza.ws.core.v1.CoreService;
import cz.tacr.elza.ws.core.v1.DaoRequestsService;

@Configuration
@ComponentScan(basePackageClasses = DCStorageApp.class)
@EnableAutoConfiguration
@EnableConfigurationProperties
@EnableWebMvc
public class DCStorageApp extends SpringBootServletInitializer {

	private static final Map<String, Object> DEFAULT_PROPERTIES = new HashMap<String, Object>() {{
		put("dcstorage.repositoryIdentifier", "defaultReporisotory");
		put("dcstorage.basePath", "/storage");
		put("dcstorage.rejectMode", false);
	}};

	@Bean
	public DaoRequestsService daoRequestsService() {
		return new CoreService().getDaoRequestsService();
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