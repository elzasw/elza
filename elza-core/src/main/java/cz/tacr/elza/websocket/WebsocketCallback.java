package cz.tacr.elza.websocket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Třída pro možnost poslání zpět zprávy klientovi s odpovědí operace- např. při založení dat jsou tímto callbackem
 * poslána založená data atp. Jedná se o obdobu návratové hodnoty v kontroleru.
 *
 */
@Service
public class WebsocketCallback {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Poslání výstupních dat operace u websocket zpět na klienta.
     *
     * @param resultData data pro poslání
     * @param receiptId  receipt id
     */
	protected void send(final Object resultData, final String receiptId) {
		Validate.notNull(receiptId);
        final Map<String, Object> sendHeader = new HashMap<>();
        sendHeader.put("receipt-id", receiptId);
        messagingTemplate.convertAndSend("/topic/api/changes", resultData, sendHeader);
    }

    /**
     * Poslání výstupních dat operace u websocket zpět na klienta.
     *
     * @param resultData     data pro poslání
     * @param headerAccessor hlavička, která byla u vstupního volání websocket - kvůli načtení správného receipt id
     */
    public void sendResult(final Object resultData, final SimpMessageHeaderAccessor headerAccessor) {
        final List<String> receipt = headerAccessor.getNativeHeader("receipt");
        final String receiptId = receipt == null || receipt.isEmpty() ? null : receipt.get(0);

        if (receiptId == null) {
            throw new IllegalStateException("Cannot send callback data, receipt is not defined in header.");
        }

        send(resultData, receiptId);
    }

	/**
	 * Poslání dat zpět až po provedení commitu transakce.
	 * 
	 * @param resultData
	 *            data pro poslání
	 * @param headerAccessor
	 *            geader sccessor
	 */
	public void sendAfterCommit(final Object resultData, final SimpMessageHeaderAccessor headerAccessor) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
			@Override
			public void afterCommit() {
				// Odeslání dat zpět
				sendResult(resultData, headerAccessor);
			}
		});
	}
}
