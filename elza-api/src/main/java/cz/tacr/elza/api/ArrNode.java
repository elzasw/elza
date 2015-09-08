package cz.tacr.elza.api;

import java.io.Serializable;


/**
 * Entita zajišťuje zámek pro uzel kvůli konkurentnímu přístupu.
 *
 * @author vavrejn
 *
 */
public interface ArrNode extends Versionable, Serializable {

    Integer getNodeId();

    void setNodeId(Integer nodeId);

}
