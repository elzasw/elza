package cz.tacr.elza.service.eventnotification.events;

/**
 * Typ události o změně, která bude odeslána klientovi.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.01.2016
 */
public enum EventType {

    FUND_CREATE(EventId.class),
    FUND_UPDATE(EventId.class),
    FUND_DELETE(EventId.class),

    APPROVE_VERSION(EventFund.class),

    NODE_DELETE(EventIdInVersion.class),

    ADD_LEVEL_AFTER(EventAddNode.class),
    ADD_LEVEL_BEFORE(EventAddNode.class),
    ADD_LEVEL_UNDER(EventAddNode.class),

    MOVE_LEVEL_AFTER(EventNodeMove.class),
    MOVE_LEVEL_BEFORE(EventNodeMove.class),
    MOVE_LEVEL_UNDER(EventNodeMove.class),

    DELETE_LEVEL(EventDeleteNode.class),

    PARTY_CREATE(EventId.class),
    PARTY_DELETE(EventId.class),
    PARTY_UPDATE(EventId.class),

    RECORD_CREATE(EventId.class),
    RECORD_DELETE(EventId.class),
    RECORD_UPDATE(EventId.class),

    COPY_OLDER_SIBLING_ATTRIBUTE(EventIdInVersion.class),

    NODES_CHANGE(EventIdsInVersion.class),

    DESC_ITEM_CHANGE(EventChangeDescItem.class),
    OUTPUT_ITEM_CHANGE(EventChangeOutputItem.class),
    FUND_RECORD_CHANGE(EventNodeIdVersionInVersion.class),

    INDEXING_FINISHED(ActionEvent.class),

    PACKAGE(ActionEvent.class),

    PACKETS_CHANGE(EventId.class),

    INSTITUTION_CHANGE(ActionEvent.class),

    VISIBLE_POLICY_CHANGE(EventIdsInVersion.class),

    CONFORMITY_INFO(EventIdsInVersion.class),

    OUTPUT_CHANGES(EventIdsInVersion.class),
    OUTPUT_CHANGES_DETAIL(EventIdsInVersion.class),

    BULK_ACTION_STATE_CHANGE(EventStringInVersion.class);

    private Class<? extends AbstractEventSimple> eventClass;

    EventType(final Class<? extends AbstractEventSimple> eventClass) {
        this.eventClass = eventClass;
    }

    public Class<? extends AbstractEventSimple> getEventClass() {
        return eventClass;
    }
}
