package cz.tacr.elza.api.vo;

import java.util.Collection;


/**
 * Rozšíření hodnoty atributů o seznam dopadů na uzel.
 *
 * @author Martin Šlapa
 * @since 27.11.2015
 */
public interface RelatedNodeDirectionWithDescItems<RND extends RelatedNodeDirection> {

    /**
     * @return seznam změn dopadů na uzel
     */
    Collection<RND> getRelatedNodeDirections();


    /**
     * @param relatedNodeDirections seznam změn dopadů na uzel
     */
    void setRelatedNodeDirections(Collection<RND> relatedNodeDirections);

}
