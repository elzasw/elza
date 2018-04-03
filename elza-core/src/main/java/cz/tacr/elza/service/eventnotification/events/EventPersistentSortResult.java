package cz.tacr.elza.service.eventnotification.events;

import cz.tacr.elza.domain.ArrBulkActionRun;

/**
 * Událost, pro refresh stromu po řazení
 *
 * @author Karel Šoupa [<a href="mailto:karel.soupa@marbes.cz">karel.soupa@marbes.cz</a>]
 * @since 3.4.2018
 */
public class EventPersistentSortResult extends AbstractEventSimple {

    public ArrBulkActionRun.State state;

    public EventPersistentSortResult(EventType eventType, ArrBulkActionRun.State state) {
        super(eventType);
        this.state = state;
    }
}
