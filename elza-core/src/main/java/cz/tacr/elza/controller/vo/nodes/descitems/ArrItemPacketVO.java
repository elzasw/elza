package cz.tacr.elza.controller.vo.nodes.descitems;


import javax.persistence.EntityManager;

import cz.tacr.elza.controller.vo.ArrPacketVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;


/**
 * VO hodnoty atributu - packet.
 *
 */
public class ArrItemPacketVO extends ArrItemVO {

    /**
     * obal
     */
    private Integer value;

    private ArrPacketVO packet;

    public Integer getValue() {
        return value;
    }

    public void setValue(final Integer value) {
        this.value = value;
    }

    public ArrPacketVO getPacket() {
        return packet;
    }

    public void setPacket(final ArrPacketVO packet) {
        this.packet = packet;
    }

    // Entity can be created only from ID and not from embedded object
    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataPacketRef data = new ArrDataPacketRef();
        
        if(packet!=null) {
            throw new BusinessException("Inconsistent data, packet is not null", BaseCode.PROPERTY_IS_INVALID);
        }
        
        // try to map packet
        ArrPacket pck = null;
        if (value != null) {
            pck = em.getReference(ArrPacket.class, value);
        }
        data.setPacket(pck);

        data.setDataType(DataType.PACKET_REF.getEntity());
        return data;
    }
}