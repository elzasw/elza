package cz.tacr.elza.api.vo;

import java.io.Serializable;

import cz.tacr.elza.api.ArrFaLevel;
import cz.tacr.elza.api.ArrNode;


public interface ArrFaLevelPack<FL extends ArrFaLevel, N extends ArrNode> extends Serializable {

    FL getFaLevel();

    void setFaLevel(FL faLevel);

    N getExtraNode();

    void setExtraNode(N parentNode);

}
