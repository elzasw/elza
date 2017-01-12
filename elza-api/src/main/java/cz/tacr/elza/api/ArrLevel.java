package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Úroveň hierarchického popisu. Úroveň sama o sobě není nositelem hodnoty. Vlastní hodnoty prvků
 * popisu jsou zapsány v atributech archivního popisu {@link ArrDescItem}.
 *
 * @author vavrejn
 *
 * @param <FC> {@link ArrChange}
 * @param <N> {@link ArrNode}
 */
public interface ArrLevel<FC extends ArrChange> extends Serializable {

    Integer getLevelId();

    void setLevelId(Integer levelId);

    /**
     * @return číslo změny vytvoření uzlu.
     */
    FC getCreateChange();

    /**
     * @param createChange číslo změny vytvoření uzlu.
     */
    void setCreateChange(FC createChange);

    /**
     * @return číslo změny smazání uzlu.
     */
    FC getDeleteChange();

    /**
     * @param deleteChange číslo změny smazání uzlu.
     */
    void setDeleteChange(FC deleteChange);

    /**
     * @return pozice uzlu mezi sourozenci.
     */
    Integer getPosition();

    /**
     * @param position pozice uzlu mezi sourozenci.
     */
    void setPosition(Integer position);
}
