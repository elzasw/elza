package cz.tacr.elza.controller.vo;

import cz.tacr.elza.api.ArrPacket;

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
    private ArrPacket.State state;

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

    public ArrPacket.State getState() {
        return state;
    }

    public void setState(final ArrPacket.State state) {
        this.state = state;
    }
}
