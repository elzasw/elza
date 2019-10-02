package cz.tacr.elza.domain.vo;

import java.util.Collection;


/**
 * Rozšíření hodnoty atributů o seznam dopadů na uzel.
 *
 * @author Martin Šlapa
 * @since 27.11.2015
 */
public class RelatedNodeDirectionWithDescItems {

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
