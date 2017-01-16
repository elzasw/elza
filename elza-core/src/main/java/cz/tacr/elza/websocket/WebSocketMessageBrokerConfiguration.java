package cz.tacr.elza.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.socket.config.annotation.DelegatingWebSocketMessageBrokerConfiguration;


/**
 * Custom configuration is used to modify {@link #clientInboundChannelExecutor()}
 * and register StompExtensionMessageHandler after SimpleBrokerMessageHandler.
 *
 * @author Jaroslav Todt [jaroslav.todt@lightcomp.cz]
 * @since 25.8.2016
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
}