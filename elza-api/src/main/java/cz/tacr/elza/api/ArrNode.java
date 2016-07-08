package cz.tacr.elza.api;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * Entita zajišťuje zámek pro uzel kvůli konkurentnímu přístupu.
 *
 * @author vavrejn
 */
public interface ArrNode<F extends ArrFund> extends Versionable, Serializable {

    Integer getNodeId();

    void setNodeId(Integer nodeId);

    LocalDateTime getLastUpdate();

    void setLastUpdate(LocalDateTime lastUpdate);

    String getUuid();

    void setUuid(String uuid);

    void setFund(F fund);

    F getFund();
}
