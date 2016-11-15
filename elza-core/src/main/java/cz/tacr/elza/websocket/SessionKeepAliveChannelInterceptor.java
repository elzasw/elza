package cz.tacr.elza.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

import java.util.Map;

/**
 * @author Pavel Stánek [pavel.stanek@marbes.cz]
 * @since 25.10.2016
 */
public class SessionKeepAliveChannelInterceptor extends ChannelInterceptorAdapter {

    private SessionRepository sessionRepository;

    private static final Logger logger = LoggerFactory.getLogger(SessionKeepAliveChannelInterceptor.class);

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        Map<String, Object> sessionHeaders = SimpMessageHeaderAccessor.getSessionAttributes(message.getHeaders());
        String sessionId = (String) sessionHeaders.get("JSESSIONID");
        if (sessionId != null) {
            Session session = sessionRepository.getSession(sessionId);
            if (session != null) {
                logger.info("Keeping session with id : " + sessionId + " alive ");
                sessionRepository.save(session);
            }
        }
        return super.preSend(message, channel);
    }

    @Autowired
    public void setSessionRepository(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }
}