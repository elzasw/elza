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
    FUND_INVALID(EventFunds.class),

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
    PARTIES_CREATE(EventId.class),
    PARTY_DELETE(EventId.class),
    PARTY_UPDATE(EventId.class),

    ACCESS_POINT_CREATE(EventId.class),
    ACCESS_POINT_DELETE(EventId.class),
    ACCESS_POINT_UPDATE(EventId.class),

    /**
     * Asi drive pouzivano pro notifikaci o zkopirovanem atributu
     * 
     * Nove staci notifikace o zmenenem uzlu
     */
    //COPY_OLDER_SIBLING_ATTRIBUTE(EventIdInVersion.class),

    NODES_CHANGE(EventIdsInVersion.class),

    // při použití UNDO, kde se reálně entity JP můžou mazat
    DELETE_NODES(EventIdsInVersion.class),

    DESC_ITEM_CHANGE(EventChangeDescItem.class),
    OUTPUT_ITEM_CHANGE(EventChangeOutputItem.class),
    FUND_RECORD_CHANGE(EventNodeIdVersionInVersion.class),
    FUND_EXTENSION_CHANGE(EventNodeIdVersionInVersion.class),

    DAO_LINK_CREATE(EventIdNodeIdInVersion.class),
    DAO_LINK_DELETE(EventIdNodeIdInVersion.class),

    INDEXING_FINISHED(ActionEvent.class),

    PACKAGE(ActionEvent.class),

    FILES_CHANGE(EventStringInVersion.class),

    INSTITUTION_CHANGE(ActionEvent.class),

    VISIBLE_POLICY_CHANGE(EventIdsInVersion.class),

    CONFORMITY_INFO(EventIdsInVersion.class),

    STRUCTURE_DATA_CHANGE(EventStructureDataChange.class),

    OUTPUT_STATE_CHANGE(EventIdAndStringInVersion.class),
    OUTPUT_CHANGES(EventIdsInVersion.class),
    OUTPUT_CHANGES_DETAIL(EventIdsInVersion.class),

    USER_CHANGE(EventId.class),
    USER_CREATE(EventId.class),
    GROUP_CHANGE(EventId.class),
    GROUP_CREATE(EventId.class),
    GROUP_DELETE(EventId.class),

    REQUEST_CHANGE(EventIdNodeIdInVersion.class),
    REQUEST_CREATE(EventIdNodeIdInVersion.class),

    REQUEST_DAO_CHANGE(EventIdDaoIdInVersion.class),
    REQUEST_DAO_CREATE(EventIdDaoIdInVersion.class),

    REQUEST_DELETE(EventIdNodeIdInVersion.class),

    REQUEST_ITEM_QUEUE_CREATE(EventIdRequestIdInVersion.class),
    REQUEST_ITEM_QUEUE_CHANGE(EventIdRequestIdInVersion.class),
    REQUEST_ITEM_QUEUE_DELETE(EventIdRequestIdInVersion.class),

    EXTERNAL_SYSTEM_UPDATE(EventId.class),
    EXTERNAL_SYSTEM_CREATE(EventId.class),
    EXTERNAL_SYSTEM_DELETE(EventId.class),

    ISSUE_LIST_CREATE(EventId.class),
    ISSUE_LIST_UPDATE(EventId.class),
    ISSUE_CREATE(EventIdInIssueList.class),
    ISSUE_UPDATE(EventIdInIssueList.class),

    BULK_ACTION_STATE_CHANGE(EventStringInVersion.class);

    private Class<? extends AbstractEventSimple> eventClass;

    EventType(final Class<? extends AbstractEventSimple> eventClass) {
        this.eventClass = eventClass;
    }

    public Class<? extends AbstractEventSimple> getEventClass() {
        return eventClass;
    }
}
