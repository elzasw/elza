package cz.tacr.elza.drools.model;

/**
 * Objekt nového uzlu pro skripty pravidel.
 *
 * @author Martin Šlapa
 * @since 23.12.2015
 */
public class NewLevel extends Level {

    /**
     * Uzel sourozence před.
     */
    private Level siblingBefore;

    /**
     * Uzel sourozence po.
     */
    private Level siblingAfter;

    /**
     * Určuje u levelu kdo ho vytváří.
     */
    private EventSource eventSource;

    public Level getSiblingBefore() {
        return siblingBefore;
    }

    public void setSiblingBefore(final Level siblingBefore) {
        this.siblingBefore = siblingBefore;
    }

    public Level getSiblingAfter() {
        return siblingAfter;
    }

    public void setSiblingAfter(final Level siblingAfter) {
        this.siblingAfter = siblingAfter;
    }

    public EventSource getEventSource() {
        return eventSource;
    }

    public void setEventSource(final EventSource eventSource) {
        this.eventSource = eventSource;
    }
}
