package cz.tacr.elza.websocket;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import cz.tacr.elza.websocket.core.WebSocketAwareController;
import cz.tacr.elza.websocket.fund.MessageBrokerConfigurer;

@Controller
@WebSocketAwareController
public class TestController {

	private static final ConcurrentHashMap<String, Integer> SESSION_MESSAGE = new ConcurrentHashMap<>();

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@MessageMapping("/chat")
	public void chat(Message message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
		message.updateMessage(headerAccessor.getUser());
		if (StringUtils.isEmpty(message.getRecipient())) {
			messagingTemplate.convertAndSend(MessageBrokerConfigurer.BROKER_DESTINATION + "/chat", message);
		} else {
			messagingTemplate.convertAndSendToUser(message.getRecipient(), MessageBrokerConfigurer.BROKER_DESTINATION + "/chat", message);
		}
		System.out.println(headerAccessor.getSessionAttributes());
	}

	@MessageMapping("/traffic")
	public void traffic(Message message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
		message.updateMessage(headerAccessor.getUser());
		updateOffset(message, headerAccessor);
	}

	@MessageMapping("/error")
	public void error(SimpMessageHeaderAccessor headerAccessor) throws Exception {
		throw new RuntimeException("Fake controller exception");
	}

	private void updateOffset(Message message, SimpMessageHeaderAccessor headerAccessor) {
		String id = headerAccessor.getSessionId(); // sessionId
		int c = Integer.parseInt(message.getText()); // current
		Integer l = SESSION_MESSAGE.put(id, c); // last
		if (c % 1000 == 0) {
			System.out.println("Message recieved, sessionId:" + id + ", offset:" + (c-l));
		}
	}
}
