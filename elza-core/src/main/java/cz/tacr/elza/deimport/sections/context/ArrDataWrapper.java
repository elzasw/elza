package cz.tacr.elza.deimport.sections.context;

import java.util.Objects;

import org.hibernate.Session;
import org.springframework.util.Assert;

import cz.tacr.elza.deimport.context.IdHolder;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;

public class ArrDataWrapper implements EntityWrapper {

    private final ArrData entity;

    private final IdHolder descItemIdHolder;

    ArrDataWrapper(ArrData entity, IdHolder descItemIdHolder) {
        this.entity = Objects.requireNonNull(entity);
        this.descItemIdHolder = Objects.requireNonNull(descItemIdHolder);
    }

    @Override
    public boolean isCreate() {
        return true;
    }

    @Override
    public boolean isUpdate() {
        return false;
    }

    @Override
    public ArrData getEntity() {
        return entity;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        Assert.isNull(entity.getItem());
        entity.setItem(descItemIdHolder.getEntityRef(session, ArrDescItem.class));
    }
}
