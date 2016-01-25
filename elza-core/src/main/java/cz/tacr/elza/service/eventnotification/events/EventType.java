package cz.tacr.elza.service.eventnotification.events;

/**
 * Typ události o změně, která bude odeslána klientovi.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.01.2016
 */
public enum EventType {

    FINDING_AID_CREATE(EventId.class),
    FINDING_AID_DELETE(EventId.class),

    NODE_DELETE(EventIdInVersion.class),

    ADD_LEVEL_AFTER(EventAddNode.class),
    ADD_LEVEL_BEFORE(EventAddNode.class),
    ADD_LEVEL_UNDER(EventAddNode.class),

    MOVE_LEVEL_AFTER(EventNodeMove.class),
    MOVE_LEVEL_BEFORE(EventNodeMove.class),
    MOVE_LEVEL_UNDER(EventNodeMove.class),

    DELETE_LEVEL(EventIdInVersion.class),

    PARTY_CREATE(EventId.class),
    PARTY_UPDATE(EventId.class),

    RECORD_CREATE(EventId.class),
    RECORD_UPDATE(EventId.class);


    private Class<? extends AbstractEventSimple> eventClass;

    EventType(final Class<? extends AbstractEventSimple> eventClass) {
        this.eventClass = eventClass;
    }

    public Class<? extends AbstractEventSimple> getEventClass() {
        return eventClass;
    }
}
