package cz.tacr.elza.domain.vo;

import cz.tacr.elza.api.controller.ArrangementManager;
import cz.tacr.elza.api.vo.ArrLevelPack;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * Zapouzdření {@link cz.tacr.elza.api.ArrLevel}, {@link cz.tacr.elza.api.ArrNode} a id archivní pomůcky.
 * Pro operace ve stromu archivního popisu. Pro různé operace se předpokládá rúzné naplnění/užití tohoto objektu.
 * Popis naplnění je vždy u API metod, viz {@link ArrangementManager}
 *
 * @author Martin Šlapa
 * @since 28.8.2015
 */
public class ArrLevelWithExtraNode implements ArrLevelPack<ArrLevel, ArrNode> {

    /** Úroveň  - předmět operace. */
    private ArrLevel level;

    /** Dodatečný uzel pro zámek. Většinou parent. */
    private ArrNode extraNode;

    /** Kořenový uzel archivní pomůcky. */
    private ArrNode rootNode;

    /** Cílová úroveň. */
    private ArrLevel levelTarget;

    /** ID archivní pomůcky. */
    private Integer faVersionId;


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
    public void setLevel(ArrLevel faLevel) {
        this.level = faLevel;
    }

    @Override
    public ArrNode getExtraNode() {
        return extraNode;
    }

    @Override
    public void setExtraNode(ArrNode extraNode) {
        this.extraNode = extraNode;
    }

    @Override
    public ArrNode getRootNode() {
        return rootNode;
    }

    @Override
    public void setRootNode(final ArrNode rootNode) {
        this.rootNode = rootNode;
    }

    @Override
    public void setLevelTarget(ArrLevel levelTarget) {
        this.levelTarget = levelTarget;
    }

    @Override
    public Integer getFaVersionId() {
        return faVersionId;
    }

    @Override
    public void setFaVersionId(Integer faVersionId) {
        this.faVersionId = faVersionId;
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
