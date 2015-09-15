package cz.tacr.elza.api.vo;

import cz.tacr.elza.api.ArrLevel;
import cz.tacr.elza.api.ArrNode;

import java.io.Serializable;


/**
 * Zapouzdření {@link ArrLevel} a {@link ArrNode}.
 * @author vavrejn
 *
 * @param <FL> {@link ArrLevel}
 * @param <N> {@link ArrNode}
 */
public interface ArrLevelPack<FL extends ArrLevel, N extends ArrNode> extends Serializable {

    FL getLevel();

    void setLevel(FL faLevel);

    N getExtraNode();

    void setExtraNode(N parentNode);

    N getRootNode();

    void setRootNode(N rootNode);

    FL getLevelTarget();

    void setLevelTarget(FL level);

    Integer getFaVersionId();

    void  setFaVersionId(Integer faVersionId);

}
