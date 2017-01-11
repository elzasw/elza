package cz.tacr.elza.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Třída pro možnost poslání zpět zprávy klientovi s odpovědí operace- např. při založení dat jsou tímto callbackem
 * poslána založená data atp. Jedná se o obdobu návratové hodnoty v kontroleru.
 *
 * @author Pavel Stánek [pavel.stanek@marbes.cz]
 * @since 22.11.2016
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
    public void send(final Object resultData, final String receiptId) {
        Assert.notNull(receiptId);
        final Map sendHeader = new HashMap();
        sendHeader.put("receipt-id", receiptId);
        messagingTemplate.convertAndSend("/topic/api/changes", resultData, sendHeader);
    }

    /**
     * Poslání výstupních dat operace u websocket zpět na klienta.
     *
     * @param resultData     data pro poslání
     * @param headerAccessor hlavička, která byla u vstupního volání websocket - kvůli načtení správného receipt id
     */
    public void send(final Object resultData, final SimpMessageHeaderAccessor headerAccessor) {
        final List<String> receipt = headerAccessor.getNativeHeader("receipt");
        final String receiptId = receipt == null || receipt.isEmpty() ? null : receipt.get(0);

        if (receiptId == null) {
            throw new IllegalStateException("Cannot send callback data, receipt is not defined in header.");
        }

        send(resultData, receiptId);
    }
}
