package cz.tacr.elza.api.vo;

import java.util.Collection;

import cz.tacr.elza.api.ArrDescItem;


/**
 * Rozšíření hodnoty atributu o seznam dopadů na uzel.
 *
 * @author Martin Šlapa
 * @since 27.11.2015
 */
public interface RelatedNodeDirectionWithDescItem<DI extends ArrDescItem, RND extends RelatedNodeDirection> {

    /**
     * @return hodnota atributu
     */
    DI getArrDescItem();


    /**
     * @param arrDescItem hodnota atributu
     */
    void setArrDescItem(DI arrDescItem);


    /**
     * @return seznam změn dopadů na uzel
     */
    Collection<RND> getRelatedNodeDirections();


    /**
     * @param relatedNodeDirections seznam změn dopadů na uzel
     */
    void setRelatedNodeDirections(Collection<RND> relatedNodeDirections);

}
