package cz.tacr.elza.api.vo;

import java.util.Collection;


/**
 * Rozšíření hodnoty atributu o seznam dopadů na uzel.
 *
 * @author Martin Šlapa
 * @since 27.11.2015
 */
public interface RelatedNodeDirectionWithDescItem<RND extends RelatedNodeDirection> {

    /**
     * @return seznam změn dopadů na uzel
     */
    Collection<RND> getRelatedNodeDirections();


    /**
     * @param relatedNodeDirections seznam změn dopadů na uzel
     */
    void setRelatedNodeDirections(Collection<RND> relatedNodeDirections);

}
