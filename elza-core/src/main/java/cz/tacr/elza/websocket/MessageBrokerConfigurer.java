package cz.tacr.elza.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.filter.RequestContextFilter;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

import java.util.Arrays;

/**
 * Konfigurace message brokera.
 *
 * @since 02.12.2015
 * @author Pavel Stánek [pavel.stanek@marbes.cz]
 */
@Configuration
@EnableWebSocketMessageBroker
public class MessageBrokerConfigurer extends AbstractWebSocketMessageBrokerConfigurer {
    @Autowired
    private SessionRegistry sessionRegistry;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // TODO - přidat error handler
        registry.addEndpoint("/stomp");
    }

    @Bean
    @ConditionalOnMissingBean
    public RequestContextFilter requestContextFilter() {
        RequestContextFilter rcf = new RequestContextFilter();
        rcf.setThreadContextInheritable(true);
        return rcf;
    }
    @Bean
    @ConditionalOnMissingBean
    public RequestContextListener RequestContextListener() {
        return new RequestContextListener();
    }

    @Bean
    public HttpSessionIdHandshakeInterceptor httpSessionIdHandshakeInterceptor() {
        return new HttpSessionIdHandshakeInterceptor();
    }

    @Bean
    public SessionKeepAliveChannelInterceptor sessionKeepAliveChannelInterceptor() {
        return new SessionKeepAliveChannelInterceptor();
    }

    @Autowired
    private WebSocketHandler subProtocolWebSocketHandler;



//    @Bean
//    public FilterRegistrationBean sessionRepositoryFilterRegistration() {
//        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
//        filterRegistrationBean.setFilter(new DelegatingFilterProxy(new SessionRepositoryFilter<>(inMemorySessionRepository())));
//        filterRegistrationBean.setUrlPatterns(Arrays.asList("/*"));
//        return filterRegistrationBean;
//    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry
                .enableSimpleBroker("/topic");
        // TODO doplnit!!!
//                .setHeartbeatValue(new long[] { 10000, 10000 })
//                .setTaskScheduler(new ThreadPoolTaskScheduler());
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Autowired
    private WebSocketThreadPoolTaskExecutor clientInboundChannelExecutor;

    @Override
    public void configureWebSocketTransport(final WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(delegate -> new ExecutorWebSocketHandlerDecorator(delegate, clientInboundChannelExecutor));

        registration.addDecoratorFactory(new WebSocketHandlerDecoratorFactory() {
            @Override
            public WebSocketHandler decorate(WebSocketHandler webSocketHandler) {
                return new WebSocketSessionCapturingHandlerDecorator(webSocketHandler);
            }
        });

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
