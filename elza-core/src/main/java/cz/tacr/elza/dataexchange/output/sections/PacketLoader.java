package cz.tacr.elza.dataexchange.output.sections;

import javax.persistence.EntityManager;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ArrPacket;

public class PacketLoader extends AbstractEntityLoader<Integer, ArrPacket> {

    public PacketLoader(EntityManager em, int batchSize) {
        super(ArrPacket.class, ArrPacket.PACKET_ID, em, batchSize);
    }
}
