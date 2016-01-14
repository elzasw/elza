package cz.tacr.elza.service.eventnotification.events;

import com.fasterxml.jackson.annotation.JsonTypeInfo;


/**
 * Předek událostí, které jsou odesílány klientům.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.01.2016
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@type")
public abstract class AbstractEventSimple<T> {

    /**
     * Typ události.
     */
    private EventType eventType;

    public AbstractEventSimple(final EventType eventType) {
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }

    /**
     * Sloučí dvě události do jedné, (např. přidá id z jedné události k id druhé)
     *
     * @param event událost stejného typu
     */
    public abstract void appendEventData(T event);

    @Override
    public String toString() {
        return "AbstractEventSimple{" +
                "eventType=" + eventType +
                '}';
    }
}
