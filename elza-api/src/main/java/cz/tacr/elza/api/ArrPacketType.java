package cz.tacr.elza.api;

import java.io.Serializable;

public interface ArrPacketType extends Serializable {

    Integer getPacketTypeId();

    void setPacketTypeId(Integer packetTypeId);

    String getCode();

    void setCode(String code);

    String getName();

    void setName(String name);

    String getShortcut();

    void setShortcut(String shortcut);

}
