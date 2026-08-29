package cz.tacr.elza.service.da;

import cz.tacr.elza.controller.vo.DaAipActionVO;

/**
 * WebSocket message pushed to the topic of the user who asked for an action over AIPs, whenever
 * anything about that action changes.
 *
 * It carries the whole action, not what changed about it, so the client replaces what it holds and
 * a lost message is corrected by the next one. The client refetches the action when the connection
 * comes back, which covers the case where the last message was the lost one.
 *
 * The {@code eventType} discriminator follows the shape of the broadcast messages, so the client
 * routes it through the same handler.
 */
public class DaAipActionUpdateMessage {

    public static final String EVENT_TYPE = "AIP_ACTION_UPDATE";

    private final DaAipActionVO action;

    public DaAipActionUpdateMessage(final DaAipActionVO action) {
        this.action = action;
    }

    public String getEventType() {
        return EVENT_TYPE;
    }

    public DaAipActionVO getAction() {
        return action;
    }
}
