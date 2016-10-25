package cz.tacr.elza.websocket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

	@MessageMapping("/chat")
	public void chat(Message message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
		System.out.println("$$$$$$$$$ controller");
		message.updateMessage(headerAccessor.getUser());

		final List<String> receipt = headerAccessor.getNativeHeader("receipt");
		final String receiptId = receipt == null || receipt.isEmpty() ? null : receipt.get(0);
		Map sendHeader = new HashMap();
		sendHeader.put("receipt-id", receiptId);

		if (StringUtils.isEmpty(message.getRecipient())) {
			messagingTemplate.convertAndSend(MessageBrokerConfigurer.BROKER_DESTINATION + "/chat", message, sendHeader);
		} else {
			messagingTemplate.convertAndSendToUser(message.getRecipient(), MessageBrokerConfigurer.BROKER_DESTINATION + "/chat", message, sendHeader);
		}
		return;
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
