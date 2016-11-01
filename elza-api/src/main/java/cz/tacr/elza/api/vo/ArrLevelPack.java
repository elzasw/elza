package cz.tacr.elza.api.vo;

import cz.tacr.elza.api.ArrDescItem;
import cz.tacr.elza.api.ArrLevel;
import cz.tacr.elza.api.ArrNode;

import java.io.Serializable;
import java.util.List;


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
public interface ArrLevelPack<FL extends ArrLevel, N extends ArrNode, DI extends ArrDescItem> extends Serializable {

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
     * Dodatečný uzel pro zámek. Většinou parent.
     * @return  dodatečný uzel pro zámek, většinou parent
     */
    N getExtraNode();

    /**
     * Dodatečný uzel pro zámek. Většinou parent.
     * @param parentNode dodatečný uzel pro zámek, většinou parent
     */
    void setExtraNode(N parentNode);

    /**
     * Kořenový uzel archivní pomůcky.
     * @return kořenový uzel archivní pomůcky
     */
    N getRootNode();

    /**
     * Kořenový uzel archivní pomůcky.
     * @param rootNode kořenový uzel archivní pomůcky
     */
    void setRootNode(N rootNode);

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


    /**
     * Seznam hodnot atrubutů.
     * @return seznam hodnot atrubutů
     */
    List<DI> getDescItems();


    /**
     * Nastaví seznam hodnot atrubutů.
     * @param descItems seznam hodnot atrubutů
     */
    void setDescItems(List<DI> descItems);

}
