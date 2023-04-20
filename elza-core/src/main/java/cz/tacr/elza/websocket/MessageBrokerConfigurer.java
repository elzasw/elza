package cz.tacr.elza.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * Konfigurace message brokera.
 */
@Configuration
@EnableWebSocketMessageBroker
public class MessageBrokerConfigurer extends AbstractSecurityWebSocketMessageBrokerConfigurer {

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
    public void configureWebSocketTransport(final WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(delegate -> new ExecutorWebSocketHandlerDecorator(delegate,
                clientInboundChannelExecutor));
        registration.addDecoratorFactory(delegate -> new ExecutorWebSocketHandlerDecorator(delegate,
                clientOutboundChannelExecutor));
        registration.setSendBufferSizeLimit(512 * 1024);
        registration.setMessageSizeLimit(512 * 1024);
        super.configureWebSocketTransport(registration);
    }

    @Override
    public void configureMessageBroker(final MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user"); // direct message for subscribed user
        registry.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[] { 20000, 20000 })
                .setTaskScheduler(heartbeatTaskScheduler());
    }

    @Override
    public void registerStompEndpoints(final StompEndpointRegistry registry) {
        registry.setErrorHandler(new StompSubProtocolErrorHandler());
        registry.addEndpoint("/stomp")
                .setAllowedOrigins("*") // kvůli reverse-proxy
                // copy HTTP session attributes to simpSessionAttributes
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

    @Override
    protected void configureInbound(final MessageSecurityMetadataSourceRegistry messages) {
          messages
               .nullDestMatcher().authenticated()
               .simpTypeMatchers(SimpMessageType.MESSAGE, SimpMessageType.SUBSCRIBE).authenticated()
               .anyMessage().denyAll();
    }

    @Override
    protected boolean sameOriginDisabled() {
        return true;
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
            executor.addSession(session.getId());
            super.afterConnectionEstablished(session);
        }

        @Override
        public void afterConnectionClosed(final WebSocketSession session, final CloseStatus closeStatus) throws Exception {
            super.afterConnectionClosed(session, closeStatus);
            executor.removeSession(session.getId());
        }
    }
}
