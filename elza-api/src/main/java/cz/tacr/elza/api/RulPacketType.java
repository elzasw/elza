package cz.tacr.elza.api;

import java.io.Serializable;


public interface RulPacketType<P extends RulPackage> extends Serializable {

    Integer getPacketTypeId();


    void setPacketTypeId(Integer packetTypeId);


    String getCode();


    void setCode(String code);


    String getName();


    void setName(String name);


    String getShortcut();


    void setShortcut(String shortcut);


    /**
     * @return balíček
     */
    P getPackage();


    /**
     * @param rulPackage balíček
     */
    void setPackage(P rulPackage);
}
