package cz.tacr.elza.controller.vo.nodes.descitems;


import cz.tacr.elza.controller.vo.ArrPacketVO;


/**
 * VO hodnoty atributu - packet.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
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
}