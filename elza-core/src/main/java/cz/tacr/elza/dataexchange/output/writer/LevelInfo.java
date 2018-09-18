package cz.tacr.elza.dataexchange.output.writer;

import java.util.Collection;

import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNodeRegister;

public interface LevelInfo {

    int getNodeId();

    Integer getParentNodeId();

    String getNodeUuid();

    Collection<ArrItem> getItems();

    Collection<ArrNodeRegister> getNodeAps();
}