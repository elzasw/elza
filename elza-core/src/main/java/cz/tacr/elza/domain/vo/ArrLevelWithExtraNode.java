package cz.tacr.elza.domain.vo;

import cz.tacr.elza.api.vo.ArrLevelPack;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;


/**
 * Zapouzdření {@link ArrLevel} a {@link ArrNode}.
 * @author Martin Šlapa
 * @since 28.8.2015
 */
public class ArrLevelWithExtraNode implements ArrLevelPack<ArrLevel, ArrNode> {

    private ArrLevel level;

    private ArrNode extraNode;

    private ArrNode rootNode;

    private ArrLevel levelTarget;

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
}
