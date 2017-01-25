package cz.tacr.elza.domain.vo;

import java.util.Collection;


/**
 * Rozšíření balíčku uzlů o seznam dopadů na uzel.
 *
 * @author Martin Šlapa
 * @since 27.11.2015
 */
public class RelatedNodeDirectionWithLevelPack {

    private ArrLevelWithExtraNode arrLevelWithExtraNode;

    private Collection<Collection<RelatedNodeDirection>> relatedNodeDirections;

    /**
     * @return balíček uzlů
     */
    public ArrLevelWithExtraNode getArrLevelPack() {
        return arrLevelWithExtraNode;
    }

    /**
     * @param arrLevelPack balíček uzlů
     */
    public void setArrLevelPack(final ArrLevelWithExtraNode arrLevelWithExtraNode) {
        this.arrLevelWithExtraNode = arrLevelWithExtraNode;
    }

    /**
     * @return seznam seznamů změn dopadů na uzel
     */
    public Collection<Collection<RelatedNodeDirection>> getRelatedNodeDirections() {
        return relatedNodeDirections;
    }

    /**
     * @param relatedNodeDirections seznam seznamů změn dopadů na uzel
     */
    public void setRelatedNodeDirections(final Collection<Collection<RelatedNodeDirection>> relatedNodeDirections) {
        this.relatedNodeDirections = relatedNodeDirections;
    }
}
