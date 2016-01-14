package cz.tacr.elza.service.eventnotification.events;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * Událost nesoucí seznam ovlivněných id.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.01.2016
 */
public class EventId extends AbstractEventSimple<EventId> {

    /**
     * Seznam id.
     */
    private Set<Integer> ids = Collections.EMPTY_SET;

    public EventId(final EventType eventType, final Integer... ids) {
        super(eventType);
        this.ids = new HashSet<>();

        for (Integer id : ids) {
            this.ids.add(id);
        }
    }

    public EventId(final EventType eventType, final Set<Integer> ids) {
        super(eventType);
        this.ids = ids;
    }

    public Set<Integer> getIds() {
        return ids;
    }

    public void setIds(final Set<Integer> ids) {
        this.ids = ids;
    }

    @Override
    public void appendEventData(final EventId event) {
        ids.addAll(event.getIds());
    }
}
