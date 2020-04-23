package cz.tacr.elza.dataexchange.input.parts.context;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.storage.RefUpdateWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApPart;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;

public class PrefferedPartWrapper implements RefUpdateWrapper {

    private final AccessPointInfo apInfo;

    private final EntityIdHolder<ApPart> partIdHolder;

    private ApAccessPoint entity;

    public PrefferedPartWrapper(AccessPointInfo apInfo, EntityIdHolder<ApPart> partIdHolder) {
        this.apInfo = apInfo;
        this.partIdHolder = partIdHolder;
    }

    @Override
    public boolean isIgnored() {
        return apInfo.getSaveMethod().equals(SaveMethod.IGNORE);
    }

    @Override
    public boolean isLoaded(Session session) {
        entity = apInfo.getEntityRef(session);
        return HibernateUtils.isInitialized(entity);
    }

    @Override
    public void merge(Session session) {
        // prepare prefferedPart
        ApPart prefferedPart = partIdHolder.getEntityRef(session);
        entity.setPreferredPart(prefferedPart);
        // merge entity
        session.merge(entity);
        // update ap info
        apInfo.onEntityPersist();
    }

    @Override
    public void executeUpdateQuery(Session session) {
        ApPart prefferedPart = partIdHolder.getEntityRef(session);

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaUpdate<ApAccessPoint> query = cb.createCriteriaUpdate(ApAccessPoint.class);
        Root<ApAccessPoint> root = query.from(ApAccessPoint.class);
        query.set(ApAccessPoint.FIELD_PREFFERED_PART, prefferedPart);
        query.where(cb.equal(root.get(ApAccessPoint.FIELD_ACCESS_POINT_ID), apInfo.getEntityId()));

        int affected = session.createQuery(query).executeUpdate();
        Validate.isTrue(affected == 1);
        apInfo.onEntityPersist();
    }
}
