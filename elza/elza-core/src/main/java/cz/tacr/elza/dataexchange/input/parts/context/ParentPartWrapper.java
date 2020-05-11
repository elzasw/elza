package cz.tacr.elza.dataexchange.input.parts.context;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.storage.RefUpdateWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ApPart;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;

public class ParentPartWrapper implements RefUpdateWrapper {

    private final PartInfo partInfo;

    private final EntityIdHolder<ApPart> partIdHolder;

    private ApPart entity;

    public ParentPartWrapper(PartInfo partInfo, EntityIdHolder<ApPart> partIdHolder) {
        this.partInfo = Validate.notNull(partInfo);
        this.partIdHolder = Validate.notNull(partIdHolder);
    }

    @Override
    public boolean isIgnored() {
        return partInfo.getSaveMethod().equals(SaveMethod.IGNORE);
    }

    @Override
    public boolean isLoaded(Session session) {
        entity = partInfo.getEntityRef(session);
        return HibernateUtils.isInitialized(entity);
    }

    @Override
    public void merge(Session session) {
        // prepare parent part
        ApPart parentPart = partIdHolder.getEntityRef(session);
        entity.setParentPart(parentPart);
        // merge entity
        session.merge(entity);
        // update part info
        partInfo.onEntityPersist();
    }

    @Override
    public void executeUpdateQuery(Session session) {
        ApPart parentPart = partIdHolder.getEntityRef(session);

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaUpdate<ApPart> query = cb.createCriteriaUpdate(ApPart.class);
        Root<ApPart> root = query.from(ApPart.class);
        query.set(ApPart.PARENT_PART, parentPart);
        query.where(cb.equal(root.get(ApPart.PART_ID), partInfo.getEntityId()));

        int affected = session.createQuery(query).executeUpdate();
        Validate.isTrue(affected == 1);

        partInfo.onEntityPersist();
    }


}
