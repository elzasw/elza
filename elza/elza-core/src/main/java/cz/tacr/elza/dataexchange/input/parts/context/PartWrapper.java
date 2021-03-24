package cz.tacr.elza.dataexchange.input.parts.context;

import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import java.util.List;

public class PartWrapper implements EntityWrapper {

    private final ApPart entity;

    private SaveMethod saveMethod = SaveMethod.CREATE;

    private final PartInfo partInfo;

    private List<ItemWrapper> itemQueue;

    public PartWrapper(ApPart entity, PartInfo partInfo, List<ItemWrapper> itemWrapperList) {
        this.entity = Validate.notNull(entity);
        this.partInfo = Validate.notNull(partInfo);
        this.itemQueue = itemWrapperList;
    }

    public PartInfo getPartInfo() {
        return partInfo;
    }

    public List<ItemWrapper> getItemQueue() {
        return itemQueue;
    }

    @Override
    public ApPart getEntity() {
        return entity;
    }

    @Override
    public SaveMethod getSaveMethod() {
        return saveMethod;
    }

    @Override
    public void beforeEntitySave(Session session) {
        Validate.isTrue(entity.getAccessPointId() == null);
        entity.setAccessPoint(partInfo.getApInfo().getEntityRef(session));
    }

    @Override
    public void afterEntitySave(Session session) {

    }

    @Override
    public void persist(Session session) {
        session.persist(entity);
        for(ItemWrapper itemWrapper: itemQueue) {
            session.persist(((ApItem) itemWrapper.getEntity()).getData());
            session.persist(itemWrapper.getEntity());
        }
    }
}
