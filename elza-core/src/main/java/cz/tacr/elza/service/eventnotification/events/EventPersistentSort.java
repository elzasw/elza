package cz.tacr.elza.service.eventnotification.events;

import cz.tacr.elza.domain.ArrBulkActionRun;

/**
 * Událost, pro refresh stromu po řazení
 *
 * @author Karel Šoupa [<a href="mailto:karel.soupa@marbes.cz">karel.soupa@marbes.cz</a>]
 * @since 3.4.2018
 */
public class EventPersistentSort extends EventVersion {

    public ArrBulkActionRun.State state;

    public EventPersistentSort(final EventType eventType, final Integer versionId, final ArrBulkActionRun.State state) {
        super(eventType, versionId);
        this.state = state;
    }

    public ArrBulkActionRun.State getState() {
        return state;
    }
}
