package cz.tacr.elza.service.eventnotification.events;

import java.util.Set;

/**
 * Událost, která se vztahuje k AS / verzím AS.
 *
 * @author Martin Šlapa
 * @since 16.11.2016
 */
public class EventFunds extends AbstractEventSimple {

    private Set<Integer> fundIds;

    private Set<Integer> fundVersionIds;

    public EventFunds(final EventType eventType, final Set<Integer> fundIds, final Set<Integer> fundVersionIds) {
        super(eventType);
        this.fundIds = fundIds;
        this.fundVersionIds = fundVersionIds;
    }

    public Set<Integer> getFundIds() {
        return fundIds;
    }

    public Set<Integer> getFundVersionIds() {
        return fundVersionIds;
    }
}
