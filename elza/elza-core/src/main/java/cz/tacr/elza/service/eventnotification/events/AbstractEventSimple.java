package cz.tacr.elza.service.eventnotification.events;

import com.fasterxml.jackson.annotation.JsonTypeInfo;



/**
 * Předek událostí, které jsou odesílány klientům.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
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
