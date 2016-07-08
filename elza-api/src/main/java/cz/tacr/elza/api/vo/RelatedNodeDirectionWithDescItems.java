package cz.tacr.elza.api.vo;

import java.util.Collection;


/**
 * Rozšíření hodnoty atributů o seznam dopadů na uzel.
 *
 * @author Martin Šlapa
 * @since 27.11.2015
 */
public interface RelatedNodeDirectionWithDescItems<DIS extends ArrDescItems, RND extends RelatedNodeDirection> {

    /**
     * @return hodnoty atributů
     */
    DIS getArrDescItems();


    /**
     * @param arrDescItems hodnoty atributů
     */
    void setArrDescItems(DIS arrDescItems);


    /**
     * @return seznam změn dopadů na uzel
     */
    Collection<RND> getRelatedNodeDirections();


    /**
     * @param relatedNodeDirections seznam změn dopadů na uzel
     */
    void setRelatedNodeDirections(Collection<RND> relatedNodeDirections);

}
