package cz.tacr.elza.deimport.sections.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.deimport.context.EntityState;
import cz.tacr.elza.deimport.context.IdHolder;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.ArrPacket;

public class ArrPacketWrapper implements EntityWrapper {

    private final ArrPacket entity;

    private final PacketImportInfo info;

    ArrPacketWrapper(ArrPacket entity, PacketImportInfo info) {
        this.entity = Validate.notNull(entity);
        this.info = Validate.notNull(info);
    }

    public IdHolder getIdHolder() {
        return info;
    }

    @Override
    public EntityState getState() {
        return EntityState.CREATE;
    }

    @Override
    public ArrPacket getEntity() {
        return entity;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        // NOP
    }

    @Override
    public void afterEntityPersist() {
        info.init(entity.getPacketId());
    }
}
