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
     * identifikátor archivní pomůcky
     */
    private Integer findingAidId;

    /**
     * ukládací číslo
     */
    private String storageNumber;

    /**
     * je obal nevalidní?
     */
    private Boolean invalidPacket;

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

    public Integer getFindingAidId() {
        return findingAidId;
    }

    public void setFindingAidId(final Integer findingAidId) {
        this.findingAidId = findingAidId;
    }

    public String getStorageNumber() {
        return storageNumber;
    }

    public void setStorageNumber(final String storageNumber) {
        this.storageNumber = storageNumber;
    }

    public Boolean getInvalidPacket() {
        return invalidPacket;
    }

    public void setInvalidPacket(final Boolean invalidPacket) {
        this.invalidPacket = invalidPacket;
    }
}
