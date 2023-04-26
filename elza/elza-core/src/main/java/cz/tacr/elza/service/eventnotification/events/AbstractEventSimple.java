package cz.tacr.elza.service.eventnotification.events;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;



/**
 * Předek událostí, které jsou odesílány klientům.
 *
 * @since 14.01.2016
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class AbstractEventSimple {

    /**
     * Typ události.
     */
    private EventType eventType;
    /**
     * Stav události
     */
    private String state;
    /**
     * Kód
     */
    private String code;

    public AbstractEventSimple(final EventType eventType) {
        // Check if event type match
        if (!eventType.getEventClass().equals(this.getClass())) {
            throw new BusinessException("Incorrect class", BaseCode.INVALID_STATE)
                    .set("EventClass", eventType.getEventClass())
                    .set("ObjectClass", this.getClass());
        }

        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "AbstractEventSimple{" +
                "eventType=" + eventType +
                ",state=" + state +
                ",code=" + code +
                '}';
    }
}
