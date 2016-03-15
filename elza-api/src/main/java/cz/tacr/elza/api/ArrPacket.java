package cz.tacr.elza.api;

import java.io.Serializable;

public interface ArrPacket <PT extends RulPacketType, FA extends ArrFund> extends Serializable {

    Integer getPacketId();

    void setPacketId(Integer packetId);

    PT getPacketType();

    void setPacketType(PT packetType);

    FA getFund();

    void setFund(FA fund);

    String getStorageNumber();

    void setStorageNumber(String storageNumber);

    Boolean getInvalidPacket();

    void setInvalidPacket(Boolean invalidPacket);

}
