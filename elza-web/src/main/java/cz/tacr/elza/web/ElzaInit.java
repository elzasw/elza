package cz.tacr.elza.web;

import cz.tacr.elza.ElzaCore;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

public class ElzaInit extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        ElzaCore.configure();
        //System.setProperty("spring.config.location", "classpath:/elza-ui.yaml");
        return builder.sources(ElzaWebApp.class);
    }
}
