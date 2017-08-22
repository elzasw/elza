package cz.tacr.elza.deimport.sections.context;

import java.util.Objects;

import org.hibernate.Session;

import cz.tacr.elza.deimport.context.IdHolder;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.ArrPacket;

public class ArrPacketWrapper implements EntityWrapper {

    private final ArrPacket entity;

    private final PacketImportInfo info;

    ArrPacketWrapper(ArrPacket entity, PacketImportInfo info) {
        this.entity = Objects.requireNonNull(entity);
        this.info = Objects.requireNonNull(info);
    }

    public IdHolder getIdHolder() {
        return info;
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
