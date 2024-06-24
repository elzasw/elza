package cz.tacr.elza.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

	// by https://docs.spring.io/spring-security/reference/servlet/integrations/websocket.html

	@Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(MessageMatcherDelegatingAuthorizationManager.Builder messages) {
		messages
		  	.nullDestMatcher().authenticated()
		  	.simpTypeMatchers(SimpMessageType.MESSAGE, SimpMessageType.SUBSCRIBE).authenticated()
		  	.anyMessage().denyAll();

        return messages.build();
    }

	// by https://www.reddit.com/r/SpringBoot/comments/10gabkf/authenticate_the_user_when_using_websockets/
	// At this point (v6.3.1), CSRF is not configurable when using @EnableWebSocketSecurity, though this will likely be added in a future release.
	@Bean
    public ChannelInterceptor csrfChannelInterceptor() {
        return new ChannelInterceptor(){};
    }
}
