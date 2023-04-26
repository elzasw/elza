package cz.tacr.elza.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.DelegatingWebSocketMessageBrokerConfiguration;


/**
 * Custom configuration is used to modify {@link #clientInboundChannelExecutor()}
 * and register StompExtensionMessageHandler after SimpleBrokerMessageHandler.
 */
@Configuration
public class WebSocketMessageBrokerConfiguration extends DelegatingWebSocketMessageBrokerConfiguration {

    @Bean
    @Override
    public WebSocketThreadPoolTaskExecutor clientInboundChannelExecutor() {
        WebSocketThreadPoolTaskExecutor wste = new WebSocketThreadPoolTaskExecutor();
        wste.setThreadNamePrefix("clientInboundChannel-");
        return wste;
    }

    @Bean
    public WebSocketThreadPoolTaskExecutor clientOutboundChannelExecutor() {
        WebSocketThreadPoolTaskExecutor wste = new WebSocketThreadPoolTaskExecutor();
        wste.setThreadNamePrefix("clientOutboundChannel-");
        return wste;
    }

}