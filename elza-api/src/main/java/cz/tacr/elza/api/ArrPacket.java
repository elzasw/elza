package cz.tacr.elza.api;

import java.io.Serializable;

public interface ArrPacket <PT extends ArrPacketType, FA extends ArrFindingAid> extends Serializable {

    Integer getPacketId();

    void setPacketId(Integer packetId);

    PT getPacketType();

    void setPacketType(PT packetType);

    FA getFindingAid();

    void setFindingAid(FA findingAid);

    String getStorageNumber();

    void setStorageNumber(String storageNumber);

    Boolean getInvalidPacket();

    void setInvalidPacket(Boolean invalidPacket);

}
