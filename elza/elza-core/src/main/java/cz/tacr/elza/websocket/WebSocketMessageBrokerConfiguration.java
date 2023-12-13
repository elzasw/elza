package cz.tacr.elza.websocket;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.DelegatingWebSocketMessageBrokerConfiguration;


/**
 * Custom configuration is used to modify {@link #clientInboundChannelExecutor()}
 * and register StompExtensionMessageHandler after SimpleBrokerMessageHandler.
 */
@Configuration
public class WebSocketMessageBrokerConfiguration extends DelegatingWebSocketMessageBrokerConfiguration {

    public WebSocketMessageBrokerConfiguration() {

    }

    @Qualifier("clientInboundChannelExecutor")
    @Bean
    //@Override
    public WebSocketThreadPoolTaskExecutor clientInboundChannelExecutor() {
        WebSocketThreadPoolTaskExecutor wste = new WebSocketThreadPoolTaskExecutor();
        wste.setThreadNamePrefix("clientInboundChannel-");
        return wste;
    }

    @Qualifier("clientOutboundChannelExecutor")
    @Bean
    public WebSocketThreadPoolTaskExecutor clientOutboundChannelExecutor() {
        WebSocketThreadPoolTaskExecutor wste = new WebSocketThreadPoolTaskExecutor();
        wste.setThreadNamePrefix("clientOutboundChannel-");
        return wste;
    }

}