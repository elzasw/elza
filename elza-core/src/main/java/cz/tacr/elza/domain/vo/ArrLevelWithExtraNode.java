package cz.tacr.elza.domain.vo;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;

import cz.tacr.elza.api.vo.ArrLevelPack;
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
public class ArrLevelWithExtraNode implements ArrLevelPack<ArrLevel>, Serializable {

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

    @Override
    public ArrLevel getLevel() {
        return level;
    }

    @Override
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

    @Override
    public void setLevelTarget(final ArrLevel levelTarget) {
        this.levelTarget = levelTarget;
    }

    @Override
    public Integer getFundVersionId() {
        return fundVersionId;
    }

    @Override
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

    @Override
    public ArrLevel getLevelTarget() {
        return levelTarget;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
