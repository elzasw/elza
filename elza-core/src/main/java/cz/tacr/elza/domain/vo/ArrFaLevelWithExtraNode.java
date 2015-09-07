package cz.tacr.elza.domain.vo;

import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrFaLevel;


/**
 * @author Martin Šlapa
 * @since 28.8.2015
 */
public class ArrFaLevelWithExtraNode implements cz.tacr.elza.api.vo.ArrFaLevelPack<ArrFaLevel, ArrNode> {

    private ArrFaLevel faLevel;

    private ArrNode extraNode;

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
}
