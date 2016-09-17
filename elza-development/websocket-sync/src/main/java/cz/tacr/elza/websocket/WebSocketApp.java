package cz.tacr.elza.websocket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;

@SpringBootApplication
public class WebSocketApp extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(WebSocketApp.class, args);
	}
}
