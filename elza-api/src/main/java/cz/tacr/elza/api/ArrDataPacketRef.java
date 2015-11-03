package cz.tacr.elza.api;

import java.io.Serializable;

public interface ArrDataPacketRef extends Serializable {

    Integer getPacketId();

    void setPacketId(Integer packetId);
}
