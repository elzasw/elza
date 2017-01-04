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

	public static final String REPOSITORY_IDENTIFIER_PARAM_NAME = "repositoryIdentifier";
	public static final String BASE_PATH_PARAM_NAME = "storageBasePath";
	public static final String REJECT_MODE_PARAM_NAME = "rejectMode";

	private static final Map<String, Object> DEFAULT_PROPERTIES = new HashMap<String, Object>() {{
		put(REPOSITORY_IDENTIFIER_PARAM_NAME, "defaultTestStorage");
		put(BASE_PATH_PARAM_NAME, "/storage");
	}};

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(DCStorageApp.class);
		app.setDefaultProperties(DEFAULT_PROPERTIES);
		app.run(args);
	}

	@Bean
	public DaoRequestsService daoRequestsService() {
		return new CoreService().getDaoRequestsService();
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(DCStorageApp.class).properties(DEFAULT_PROPERTIES);
	}
}