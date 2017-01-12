package cz.tacr.elza.domain.vo;

import java.util.Collection;

import cz.tacr.elza.domain.ArrDescItem;


/**
 * Rozšíření hodnoty atributu o seznam dopadů na uzel.
 *
 * @author Martin Šlapa
 * @since 27.11.2015
 */
public class RelatedNodeDirectionWithDescItem {

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

    /**
     * @return seznam změn dopadů na uzel
     */
    public Collection<RelatedNodeDirection> getRelatedNodeDirections() {
        return relatedNodeDirections;
    }

    /**
     * @param relatedNodeDirections seznam změn dopadů na uzel
     */
    public void setRelatedNodeDirections(final Collection<RelatedNodeDirection> relatedNodeDirections) {
        this.relatedNodeDirections = relatedNodeDirections;
    }
}
