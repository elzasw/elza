package cz.tacr.elza.api;

import java.io.Serializable;

public interface ArrDataPacketRef<P extends ArrPacket> extends Serializable {

    P getPacket();

    void setPacket(P packet);
}
