package cz.tacr.elza.websocket.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.DelegatingWebSocketMessageBrokerConfiguration;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

/**
 * Custom configuration is used to modify {@link #clientInboundChannelExecutor()}
 * and register StompExtensionMessageHandler after SimpleBrokerMessageHandler.
 */
@Configuration
public class WebSocketMessageBrokerConfiguration extends DelegatingWebSocketMessageBrokerConfiguration {

    @Bean
    @Override
    public WebSocketThreadPoolTaskExecutor clientInboundChannelExecutor() {
        return new WebSocketThreadPoolTaskExecutor();
    }

    @Bean
    @DependsOn("simpleBrokerMessageHandler")
    public StompExtensionMessageHandler stompExtensionMessageHandler() {
        return new StompExtensionMessageHandler(clientInboundChannel(), clientOutboundChannel(), brokerChannel());
    }
    
    @Override
    protected void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(
                delegate -> new ExecutorWebSocketHandlerDecorator(delegate, clientInboundChannelExecutor()));
        super.configureWebSocketTransport(registration);
    }

    /**
     * Decorator is used to add/remove WebSocket session for {@link WebSocketThreadPoolTaskExecutor}.
     */
    private static class ExecutorWebSocketHandlerDecorator extends WebSocketHandlerDecorator {

        private final WebSocketThreadPoolTaskExecutor executor;

        public ExecutorWebSocketHandlerDecorator(WebSocketHandler delegate, WebSocketThreadPoolTaskExecutor executor) {
            super(delegate);
            this.executor = executor;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            executor.addSession(session.getId());
            super.afterConnectionEstablished(session);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
            super.afterConnectionClosed(session, closeStatus);
            executor.removeSession(session.getId());
        }
    }
}