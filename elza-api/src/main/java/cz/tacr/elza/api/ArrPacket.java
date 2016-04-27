package cz.tacr.elza.api;

import cz.tacr.elza.api.interfaces.IArrFund;

import java.io.Serializable;

public interface ArrPacket <PT extends RulPacketType, FA extends ArrFund> extends Serializable, IArrFund {

    /**
     * Stav obalu.
     */
    enum State {
        OPEN,
        CLOSED,
        CANCELED;
    }

    Integer getPacketId();

    void setPacketId(Integer packetId);

    PT getPacketType();

    void setPacketType(PT packetType);

    FA getFund();

    void setFund(FA fund);

    String getStorageNumber();

    void setStorageNumber(String storageNumber);

    State getState();

    void setState(State state);

}
