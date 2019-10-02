package cz.tacr.elza.domain.vo;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;


/**
 * Zapouzdření {@link cz.tacr.elza.api.ArrLevel}, {@link cz.tacr.elza.api.ArrNode} a id archivní pomůcky.
 * Pro operace ve stromu archivního popisu. Pro různé operace se předpokládá rúzné naplnění/užití tohoto objektu.
 * Popis naplnění je vždy u API metod
 *
 * @author Martin Šlapa
 * @since 28.8.2015
 */
public class ArrLevelWithExtraNode {

    /** Úroveň  - předmět operace. */
    private ArrLevel level;

    /** Dodatečný uzel pro zámek. Většinou parent. */
    private ArrNode extraNode;

    /** Kořenový uzel archivní pomůcky. */
    private ArrNode rootNode;

    /** Cílová úroveň. */
    private ArrLevel levelTarget;

    /** ID archivní pomůcky. */
    private Integer fundVersionId;

    /** Seznam hodnot atributu k vytvoreni */
    private List<ArrDescItem> descItems;

    public ArrLevelWithExtraNode() {
    }

    public ArrLevelWithExtraNode(final ArrLevel level, final ArrNode extraNode, final ArrNode rootNode) {
        this.level = level;
        this.extraNode = extraNode;
        this.rootNode = rootNode;
    }

    /**
     * Úroveň  - předmět operace.
     * @return  úroveň  - předmět operace
     */
    public ArrLevel getLevel() {
        return level;
    }

    /**
     * Úroveň  - předmět operace.
     * @param faLevel úroveň  - předmět operace
     */
    public void setLevel(final ArrLevel faLevel) {
        this.level = faLevel;
    }

    /**
     * Dodatečný uzel pro zámek. Většinou parent.
     * @return  dodatečný uzel pro zámek, většinou parent
     */
    public ArrNode getExtraNode() {
        return extraNode;
    }

    /**
     * Dodatečný uzel pro zámek. Většinou parent.
     * @param parentNode dodatečný uzel pro zámek, většinou parent
     */
    public void setExtraNode(final ArrNode extraNode) {
        this.extraNode = extraNode;
    }

    /**
     * Kořenový uzel archivní pomůcky.
     * @return kořenový uzel archivní pomůcky
     */
    public ArrNode getRootNode() {
        return rootNode;
    }

    /**
     * Kořenový uzel archivní pomůcky.
     * @param rootNode kořenový uzel archivní pomůcky
     */
    public void setRootNode(final ArrNode rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * Cílová úroveň.
     * @param parentLevel cílová úroveň
     */
    public void setLevelTarget(final ArrLevel levelTarget) {
        this.levelTarget = levelTarget;
    }

    /**
     * ID archivní pomůcky.
     * @return id archivní pomůcky
     */
    public Integer getFundVersionId() {
        return fundVersionId;
    }

    /**
     * ID archivní pomůcky.
     * @param fundVersionId id archivní pomůcky
     */
    public void setFundVersionId(final Integer fundVersionId) {
        this.fundVersionId = fundVersionId;
    }

    /**
     * Seznam hodnot atrubutů.
     * @return seznam hodnot atrubutů
     */
    public List<ArrDescItem> getDescItems() {
        return descItems;
    }

    /**
     * Nastaví seznam hodnot atrubutů.
     * @param descItems seznam hodnot atrubutů
     */
    public void setDescItems(final List<ArrDescItem> descItems) {
        this.descItems = descItems;
    }

    /**
     * Cílová úroveň.
     * @return cílová úroveň
     */
    public ArrLevel getLevelTarget() {
        return levelTarget;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
