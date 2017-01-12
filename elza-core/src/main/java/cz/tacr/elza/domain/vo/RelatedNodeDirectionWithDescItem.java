package cz.tacr.elza.domain.vo;

import java.util.Collection;

import cz.tacr.elza.api.vo.RelatedNodeDirection;
import cz.tacr.elza.domain.ArrDescItem;


/**
 * Implementace rozšíření hodnoty atributu o seznam dopadů na uzel.
 *
 * @author Martin Šlapa
 * @since 27.11.2015
 */
public class RelatedNodeDirectionWithDescItem
        implements cz.tacr.elza.api.vo.RelatedNodeDirectionWithDescItem<RelatedNodeDirection> {

    private ArrDescItem arrDescItem;

    private Collection<RelatedNodeDirection> relatedNodeDirections;

    /**
     * @return hodnota atributu
     */
    public ArrDescItem getArrDescItem() {
        return arrDescItem;
    }

    /**
     * @param arrDescItem hodnota atributu
     */
    public void setArrDescItem(final ArrDescItem arrDescItem) {
        this.arrDescItem = arrDescItem;
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
