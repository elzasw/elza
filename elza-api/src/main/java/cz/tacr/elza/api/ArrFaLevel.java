package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Úroveň hierarchického popisu. Úroveň sama o sobě není nositelem hodnoty. Vlastní hodnoty prvků
 * popisu jsou zapsány v atributech archivního popisu {@link ArrDescItem}.
 *
 * @author vavrejn
 *
 * @param <FC> {@link ArrFaChange}
 * @param <N> {@link ArrNode}
 */
public interface ArrFaLevel<FC extends ArrFaChange, N extends ArrNode> extends Serializable {

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
