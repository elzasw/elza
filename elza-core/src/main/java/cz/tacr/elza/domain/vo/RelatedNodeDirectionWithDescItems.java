package cz.tacr.elza.domain.vo;

import java.util.Collection;

import cz.tacr.elza.api.vo.RelatedNodeDirection;


/**
 * Implementace rozšíření hodnoty atributů o seznam dopadů na uzel.
 *
 * @author Martin Šlapa
 * @since 27.11.2015
 */
public class RelatedNodeDirectionWithDescItems
        implements cz.tacr.elza.api.vo.RelatedNodeDirectionWithDescItems<RelatedNodeDirection> {

    private ArrDescItems arrDescItems;

    private Collection<RelatedNodeDirection> relatedNodeDirections;

    /**
     * @return hodnoty atributů
     */
    public ArrDescItems getArrDescItems() {
        return arrDescItems;
    }

    /**
     * @param arrDescItems hodnoty atributů
     */
    public void setArrDescItems(final ArrDescItems arrDescItems) {
        this.arrDescItems = arrDescItems;
    }

    @Override
    public Collection<RelatedNodeDirection> getRelatedNodeDirections() {
        return relatedNodeDirections;
    }

    @Override
    public void setRelatedNodeDirections(final Collection<RelatedNodeDirection> relatedNodeDirections) {
        this.relatedNodeDirections = relatedNodeDirections;
    }
}
