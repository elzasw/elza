package cz.tacr.elza.domain.vo;

import java.util.Collection;

import cz.tacr.elza.api.vo.RelatedNodeDirection;


/**
 * Implementace rozšíření balíčku uzlů o seznam dopadů na uzel.
 *
 * @author Martin Šlapa
 * @since 27.11.2015
 */
public class RelatedNodeDirectionWithLevelPack
        implements cz.tacr.elza.api.vo.RelatedNodeDirectionWithLevelPack<ArrLevelWithExtraNode, RelatedNodeDirection> {

    private ArrLevelWithExtraNode arrLevelWithExtraNode;

    private Collection<Collection<RelatedNodeDirection>> relatedNodeDirections;

    @Override
    public ArrLevelWithExtraNode getArrLevelPack() {
        return arrLevelWithExtraNode;
    }

    @Override
    public void setArrLevelPack(final ArrLevelWithExtraNode arrLevelWithExtraNode) {
        this.arrLevelWithExtraNode = arrLevelWithExtraNode;
    }

    @Override
    public Collection<Collection<RelatedNodeDirection>> getRelatedNodeDirections() {
        return relatedNodeDirections;
    }

    @Override
    public void setRelatedNodeDirections(final Collection<Collection<RelatedNodeDirection>> relatedNodeDirections) {
        this.relatedNodeDirections = relatedNodeDirections;
    }
}
