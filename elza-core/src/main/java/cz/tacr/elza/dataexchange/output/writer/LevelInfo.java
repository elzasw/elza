package cz.tacr.elza.dataexchange.output.writer;

import java.util.Collection;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrItem;

public interface LevelInfo {

    int getNodeId();

    Integer getParentNodeId();

    String getNodeUuid();

    Collection<ArrItem> getItems();

    /**
     * List of connected DAOs
     * 
     * @return List of connected daos
     */
    Collection<ArrDao> getDaos();

}
