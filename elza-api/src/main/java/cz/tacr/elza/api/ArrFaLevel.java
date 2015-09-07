package cz.tacr.elza.api;

import java.io.Serializable;

public interface ArrFaLevel<FC extends ArrFaChange, N extends ArrNode> extends Versionable, Serializable {

    Integer getFaLevelId();

    void setFaLevelId(Integer faLevelId);

    N getNode();

    void setNode(N node);

    N getParentNode();

    void setParentNode(N parentNode);

    FC getCreateChange();

    void setCreateChange(FC createChange);

    FC getDeleteChange();

    void setDeleteChange(FC deleteChange);

    Integer getPosition();

    void setPosition(Integer position);
}
