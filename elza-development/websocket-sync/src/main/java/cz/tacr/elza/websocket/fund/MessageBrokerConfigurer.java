package cz.tacr.elza.websocket.fund;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

/**
 * @author Jaroslav Todt [jaroslav.todt@lightcomp.cz]
 * @since 25.8.2016
 */
@Configuration
@EnableWebSocketMessageBroker
public class MessageBrokerConfigurer extends AbstractWebSocketMessageBrokerConfigurer {

	public static final String BROKER_DESTINATION = "/fundNotification";

	@Bean
	public TaskScheduler heartbeatTaskScheduler() {
		return new ThreadPoolTaskScheduler();
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.setApplicationDestinationPrefixes("/fund"); // URL prefix where server is listening
		config.setUserDestinationPrefix("/user"); // direct message for current user (@SentToUser) or session (broadcast=false)
		config.enableSimpleBroker(BROKER_DESTINATION) // notifications from server (client must be subscribed)
				.setHeartbeatValue(new long[] { 10000, 10000 })
				.setTaskScheduler(heartbeatTaskScheduler());
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.setErrorHandler(new StompSubProtocolErrorHandler());
		registry.addEndpoint("/stomp");
	}
}