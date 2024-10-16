package cz.tacr.elza.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * Konfigurace message brokera.
 */
@Configuration
@EnableWebSocketMessageBroker
public class MessageBrokerConfigurer implements WebSocketMessageBrokerConfigurer {

    @Autowired
    @Qualifier("clientInboundChannelExecutor")
    private WebSocketThreadPoolTaskExecutor clientInboundChannelExecutor;

    @Autowired
    @Qualifier("clientOutboundChannelExecutor")
    private WebSocketThreadPoolTaskExecutor clientOutboundChannelExecutor;

    @Bean
    public TaskScheduler heartbeatTaskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(delegate -> new ExecutorWebSocketHandlerDecorator(delegate,
                clientInboundChannelExecutor));
        registration.addDecoratorFactory(delegate -> new ExecutorWebSocketHandlerDecorator(delegate,
                clientOutboundChannelExecutor));
        registration.setSendBufferSizeLimit(512 * 1024);
        registration.setMessageSizeLimit(512 * 1024);
        //super.configureWebSocketTransport(registration); // TODO Spring Boot v3
        // by https://docs.spring.io/spring-framework/reference/web/websocket/stomp/server-config.html
    }

    @Override
    public void configureMessageBroker(final MessageBrokerRegistry registry) {
        registry
        		.setApplicationDestinationPrefixes("/app")
        		.setUserDestinationPrefix("/user") // direct message for subscribed user
        		.enableSimpleBroker("/topic")
                // Hearth beat interval
                // 30000 - write interval for outgoing channel 
                //       - timeout for inbound channel is 3*30000 = 90s
                //       - heart beat is sent 30s after last activity (other then heart beat)
                // 10000 - default frequency for hearbeat packet (scheduler)
                // Recommended values for client:
                // 20000 - frequency of incomming heart beat from client to server
                //      - packet is sent each 20s, calculated as max of (20000 and 10000)
                // 45000 - client side max ttl for incomming packets is 2*45000 = 90s 
                .setHeartbeatValue(new long[] { 30000, 10000 })
                .setTaskScheduler(heartbeatTaskScheduler());
    }

    @Override
    public void registerStompEndpoints(final StompEndpointRegistry registry) {
        registry
        		.setErrorHandler(new StompSubProtocolErrorHandler())
        		.addEndpoint("/stomp")
                .setAllowedOrigins("*") // kvůli reverse-proxy
                // copy HTTP session attributes to simpSessionAttributes
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

    /**
     * Decorator is used to add/remove WebSocket session for {@link WebSocketThreadPoolTaskExecutor}.
     */
    private static class ExecutorWebSocketHandlerDecorator extends WebSocketHandlerDecorator {
        private final WebSocketThreadPoolTaskExecutor executor;

        public ExecutorWebSocketHandlerDecorator(final WebSocketHandler delegate, final WebSocketThreadPoolTaskExecutor executor) {
            super(delegate);
            this.executor = executor;
        }

        @Override
        public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
            executor.addSession(session);
            super.afterConnectionEstablished(session);
        }

        @Override
        public void afterConnectionClosed(final WebSocketSession session, final CloseStatus closeStatus) throws Exception {
            super.afterConnectionClosed(session, closeStatus);
            executor.removeSession(session);
        }
    }
}
