package cz.tacr.elza.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpSession;

import java.util.Map;

import static com.sun.org.apache.xml.internal.serializer.utils.Utils.messages;

/**
 * @author Pavel Stánek [pavel.stanek@marbes.cz]
 * @since 24.10.2016
 */
@Component
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

//    public class HttpSessionIdHandshakeInterceptor implements HandshakeInterceptor {
//
//        public boolean beforeHandshake(ServerHttpRequest request,
//                                       ServerHttpResponse response,
//                                       WebSocketHandler wsHandler,
//                                       Map<String, Object> attributes)
//                throws Exception {
//        }
//
//        @Override
//        public boolean beforeHandshake(final ServerHttpRequest request, final ServerHttpResponse response, final WebSocketHandler wsHandler, final Map<String, Object> attributes) throws Exception {
//            if (request instanceof ServletServerHttpRequest) {
//                ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
//                HttpSession session = servletRequest.getServletRequest().getSession(false);
//                if (session != null) {
//                    attributes.put(SESSION_ATTR, session.getId());
//                }
//            }
//            return true;
//        }
//
//        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
//                                   WebSocketHandler wsHandler, Exception ex) {
//        }
//    }
//
//
    @Override
    protected void customizeClientInboundChannel(ChannelRegistration registration) {
//        registration.setInterceptors(null);
    }

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
                .nullDestMatcher().authenticated()
                .simpDestMatchers("/app/**").authenticated()
        ;
    }

    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }
}