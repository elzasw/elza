package cz.tacr.elza.domain.vo;

import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrFaLevel;


/**
 * Zapouzdření {@link ArrFaLevel} a {@link ArrNode}.
 * @author Martin Šlapa
 * @since 28.8.2015
 */
public class ArrFaLevelWithExtraNode implements cz.tacr.elza.api.vo.ArrFaLevelPack<ArrFaLevel, ArrNode> {

    private ArrFaLevel faLevel;

    private ArrNode extraNode;

    private ArrNode rootNode;

    private ArrFaLevel faLevelTarget;

    public ArrFaLevelWithExtraNode() {
    }

    public ArrFaLevelWithExtraNode(final ArrFaLevel faLevel, final ArrNode extraNode, final ArrNode rootNode) {
        this.faLevel = faLevel;
        this.extraNode = extraNode;
        this.rootNode = rootNode;
    }

    @Override
    public ArrFaLevel getFaLevel() {
        return faLevel;
    }

    @Override
    public void setFaLevel(ArrFaLevel faLevel) {
        this.faLevel = faLevel;
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
    public void setFaLevelTarget(ArrFaLevel faLevelTarget) {
        this.faLevelTarget = faLevelTarget;
    }

    @Override
    public ArrFaLevel getFaLevelTarget() {
        return faLevelTarget;
    }
}
