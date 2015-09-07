package cz.tacr.elza.api;

import java.io.Serializable;


public interface ArrNode extends Versionable, Serializable {

    Integer getNodeId();

    void setNodeId(Integer nodeId);

}
