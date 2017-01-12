package cz.tacr.elza.api;

import java.io.Serializable;

public interface ArrPacket <PT extends RulPacketType> extends Serializable {

    Integer getPacketId();

    void setPacketId(Integer packetId);

    PT getPacketType();

    void setPacketType(PT packetType);

    String getStorageNumber();

    void setStorageNumber(String storageNumber);

}
