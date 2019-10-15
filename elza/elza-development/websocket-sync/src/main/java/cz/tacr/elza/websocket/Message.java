package cz.tacr.elza.websocket;

import java.security.Principal;
import java.util.Date;

public class Message {

	private String recipient;

	private String sender;

	private String text;

	private Date sent;

	private Date recieved;

	public String getRecipient() {
		return recipient;
	}

	public String getSender() {
		return sender;
	}

	public String getText() {
		return text;
	}

	public Date getSent() {
		return sent;
	}

	public Date getRecieved() {
		return recieved;
	}

	public void updateMessage(Principal principal) {
		sender = principal.getName();
		recieved = new Date();
	}

	@Override
	public String toString() {
		return "Message [recipient=" + recipient + ", sender=" + sender + ", text=" + text + ", sent=" + sent
				+ ", recieved=" + recieved + "]";
	}
}
