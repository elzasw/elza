package cz.tacr.elza.controller.vo;

/**
 * VO obalu.
 *
 * @author Martin Šlapa
 * @since 12.1.2016
 */
public class ArrPacketVO {

    /**
     * identifikátor
     */
    private Integer id;

    /**
     * identifikátor typu obalu
     */
    private Integer packetTypeId;

    /**
     * ukládací číslo
     */
    private String storageNumber;

    /**
     * stav obalu
     */
    private cz.tacr.elza.domain.ArrPacket.State state;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getPacketTypeId() {
        return packetTypeId;
    }

    public void setPacketTypeId(final Integer packetTypeId) {
        this.packetTypeId = packetTypeId;
    }

    public String getStorageNumber() {
        return storageNumber;
    }

    public void setStorageNumber(final String storageNumber) {
        this.storageNumber = storageNumber;
    }

    public cz.tacr.elza.domain.ArrPacket.State getState() {
        return state;
    }

    public void setState(final cz.tacr.elza.domain.ArrPacket.State state) {
        this.state = state;
    }
}
