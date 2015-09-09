package cz.tacr.elza.api.vo;

import java.io.Serializable;

import cz.tacr.elza.api.ArrFaLevel;
import cz.tacr.elza.api.ArrNode;


/**
 * Zapouzdření {@link ArrFaLevel} a {@link ArrNode}.
 * @author vavrejn
 *
 * @param <FL> {@link ArrFaLevel}
 * @param <N> {@link ArrNode}
 */
public interface ArrFaLevelPack<FL extends ArrFaLevel, N extends ArrNode> extends Serializable {

    FL getFaLevel();

    void setFaLevel(FL faLevel);

    N getExtraNode();

    void setExtraNode(N parentNode);

    N getRootNode();

    void setRootNode(N rootNode);

    FL getFaLevelTarget();

    void setFaLevelTarget(FL faLevel);

}
