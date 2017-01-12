package cz.tacr.elza.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 *
 * @since 02.12.2015
 * @author Pavel Stánek [pavel.stanek@marbes.cz]
 */
@Configuration
@EnableWebSocketMessageBroker
public class MessageBrokerConfigurer extends AbstractSecurityWebSocketMessageBrokerConfigurer {
    @Bean
    public TaskScheduler heartbeatTaskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    @Override
    public void configureMessageBroker(final MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user"); // direct message for current user (@SentToUser) or session (broadcast=false)
        registry
                .enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[] { 10000, 10000 })
                .setTaskScheduler(heartbeatTaskScheduler());
    }

    @Override
    public void registerStompEndpoints(final StompEndpointRegistry registry) {
        registry.setErrorHandler(new StompSubProtocolErrorHandler());
        registry.addEndpoint("/stomp")
                // copy HTTP session attributes to simpSessionAttributes
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

//    @Autowired
//    private WebSocketHandler subProtocolWebSocketHandler;

    @Override
    protected void configureInbound(final MessageSecurityMetadataSourceRegistry messages) {
        messages
//                .nullDestMatcher().authenticated()
                .simpDestMatchers("/app/**").authenticated();
    }

    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }


    @Autowired
    private WebSocketThreadPoolTaskExecutor clientInboundChannelExecutor;

    @Override
    public void configureWebSocketTransport(final WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(
                delegate -> new ExecutorWebSocketHandlerDecorator(delegate, clientInboundChannelExecutor));
        super.configureWebSocketTransport(registration);
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
