package cz.tacr.elza.deimport.sections.context;

import java.io.Serializable;

import cz.tacr.elza.deimport.context.IdHolder;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulPacketType;

public class PacketImportInfo extends IdHolder {

    private final RulPacketType packetType;

    private final String storageNumber;

    public PacketImportInfo(RulPacketType packetType, String storageNumber) {
        this.packetType = packetType;
        this.storageNumber = storageNumber;
    }

    public RulPacketType getPacketType() {
        return packetType;
    }

    public String getStorageNumber() {
        return storageNumber;
    }

    @Override
    protected void init(Serializable id) {
        super.init(id);
    }

    @Override
    public void checkReferenceClass(Class<?> entityClass) {
        if (ArrPacket.class != entityClass) {
            throw new IllegalStateException(
                    "IdHolder entity class ArrPacket does not match with class " + entityClass.getSimpleName());
        }
    }
}
