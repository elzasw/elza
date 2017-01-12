package cz.tacr.elza.api.vo;

import java.io.Serializable;

import cz.tacr.elza.api.ArrLevel;


/**
 * Zapouzdření {@link cz.tacr.elza.api.ArrLevel}, {@link cz.tacr.elza.api.ArrNode} a id archivní pomůcky.
 * Pro operace ve stromu archivního popisu. Pro různé operace se předpokládá rúzné naplnění/užití tohoto objektu.
 * Popis naplnění je vždy u API metod
 *
 * @author vavrejn
 *
 * @param <FL> {@link ArrLevel}
 * @param <N> {@link ArrNode}
 */
public interface ArrLevelPack<FL extends ArrLevel> extends Serializable {

    /**
     * Úroveň  - předmět operace.
     * @return  úroveň  - předmět operace
     */
    FL getLevel();

    /**
     * Úroveň  - předmět operace.
     * @param faLevel úroveň  - předmět operace
     */
    void setLevel(FL faLevel);

    /**
     * Cílová úroveň.
     * @return cílová úroveň
     */
    FL getLevelTarget();

    /**
     * Cílová úroveň.
     * @param level cílová úroveň
     */
    void setLevelTarget(FL level);

    /**
     * ID archivní pomůcky.
     * @return id archivní pomůcky
     */
    Integer getFundVersionId();

    /**
     * ID archivní pomůcky.
     * @param fundVersionId id archivní pomůcky
     */
    void setFundVersionId(Integer fundVersionId);
}
