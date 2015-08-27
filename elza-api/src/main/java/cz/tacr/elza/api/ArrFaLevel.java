package cz.tacr.elza.api;

import java.io.Serializable;

public interface ArrFaLevel<FC extends ArrFaChange> extends Versionable, Serializable {

    Integer getFaLevelId();

    void setFaLevelId(Integer faLevelId);

    Integer getNodeId();

    void setNodeId(Integer nodeId);

    Integer getParentNodeId();

    void setParentNodeId(Integer parentNodeId);

    FC getCreateChange();

    void setCreateChange(FC createChange);

    FC getDeleteChange();

    void setDeleteChange(FC deleteChange);

    Integer getPosition();

    void setPosition(Integer position);
}
