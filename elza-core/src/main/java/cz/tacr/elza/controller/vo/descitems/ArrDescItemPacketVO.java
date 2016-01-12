package cz.tacr.elza.controller.vo.descitems;


import cz.tacr.elza.controller.vo.ArrPacketVO;


/**
 * VO hodnoty atributu - packet.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrDescItemPacketVO extends ArrDescItemVO {

    /**
     * obal
     */
    private ArrPacketVO packet;

    public ArrPacketVO getPacket() {
        return packet;
    }

    public void setPacket(final ArrPacketVO packet) {
        this.packet = packet;
    }
}