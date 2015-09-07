package cz.tacr.elza;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;

public class ElzaInit extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        ElzaCore.configure();
        System.setProperty("spring.config.location", "classpath:/elza-ui.yaml");
        return builder.sources(ElzaApp.class);
    }
}
