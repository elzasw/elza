package cz.tacr.elza.service.eventnotification.events;

import java.util.List;

public class EventIdDaoIdInVersion extends EventVersion {

    /**
     * Id entity.
     */
    private Integer entityId;

    /**
     * Id daos.
     */
    private List<Integer> daoIds;

    public EventIdDaoIdInVersion(final EventType eventType, final Integer versionId, final Integer entityId, final List<Integer> daoIds) {
        super(eventType, versionId);
        this.entityId = entityId;
        this.daoIds = daoIds;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(final Integer entityId) {
        this.entityId = entityId;
    }

    public List<Integer> getDaoIds() {
        return daoIds;
    }

    public void setDaoIds(final List<Integer> daoIds) {
        this.daoIds = daoIds;
    }
}
