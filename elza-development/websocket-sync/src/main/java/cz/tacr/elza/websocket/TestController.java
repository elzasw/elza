package cz.tacr.elza.websocket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import cz.tacr.elza.websocket.core.WebSocketAwareController;
import cz.tacr.elza.websocket.fund.MessageBrokerConfigurer;

@Controller
@WebSocketAwareController
public class TestController {

	private static final ConcurrentHashMap<String, Integer> SESSION_MESSAGE = new ConcurrentHashMap<>();

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

    @Autowired
    @Qualifier("clientOutboundChannel")
    private transient AbstractSubscribableChannel clientOutboundChannel;

	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@MessageMapping("/chat")
	public void chat(Message message, StompHeaderAccessor headerAccessor) throws Exception {
		message.updateMessage(headerAccessor.getUser());

		String dest = MessageBrokerConfigurer.BROKER_DESTINATION + "/chat";
		String user = message.getRecipient();
		
		if (StringUtils.isEmpty(message.getRecipient())) {
			messagingTemplate.convertAndSend(dest, message);
		} else {
			messagingTemplate.convertAndSendToUser(user, dest, message);
		}
		
		if (!StringUtils.isEmpty(headerAccessor.getReceipt())) {
			sendReceipt(dest, headerAccessor);
			sendMessageToSender(dest, headerAccessor, message);
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
	
	private void sendReceipt(String destination, StompHeaderAccessor clientAccessor) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.RECEIPT);
		accessor.setSessionId(clientAccessor.getSessionId());
		accessor.setReceiptId(clientAccessor.getReceipt());
		accessor.setLeaveMutable(true);
		
		clientOutboundChannel.send(new GenericMessage<>(new byte[0], accessor.getMessageHeaders()));
	}
	
	private void sendMessageToSender(String destination, StompHeaderAccessor clientAccessor, Message message) {		
		StompHeaderAccessor  accessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
		accessor.setSessionId(clientAccessor.getSessionId());
		accessor.setReceiptId(clientAccessor.getReceipt());
		accessor.setLeaveMutable(true);

		String sender = clientAccessor.getUser().getName();
		
		messagingTemplate.convertAndSendToUser(sender, destination, message, accessor.getMessageHeaders());
	}
}
