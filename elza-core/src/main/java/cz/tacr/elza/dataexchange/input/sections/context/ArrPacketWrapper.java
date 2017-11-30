package cz.tacr.elza.dataexchange.input.sections.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;
import cz.tacr.elza.domain.ArrPacket;

public class ArrPacketWrapper implements EntityWrapper {

    private final ArrPacket entity;

    private final PacketInfo info;

    ArrPacketWrapper(ArrPacket entity, PacketInfo info) {
        this.entity = Validate.notNull(entity);
        this.info = Validate.notNull(info);
    }

    public PacketInfo getPacketInfo() {
        return info;
    }

    @Override
    public PersistMethod getPersistMethod() {
        return PersistMethod.CREATE;
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
        info.setEntityId(entity.getPacketId());
    }
}
