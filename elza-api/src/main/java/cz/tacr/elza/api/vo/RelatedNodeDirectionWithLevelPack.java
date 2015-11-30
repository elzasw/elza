package cz.tacr.elza.api.vo;

import java.util.Collection;

/**
 * Rozšíření balíčku uzlů o seznam dopadů na uzel.
 *
 * @author Martin Šlapa
 * @since 27.11.2015
 */
public interface RelatedNodeDirectionWithLevelPack<FLP extends ArrLevelPack, RND extends RelatedNodeDirection> {

    /**
     * @return balíček uzlů
     */
    FLP getArrLevelPack();


    /**
     * @param arrLevelPack balíček uzlů
     */
    void setArrLevelPack(FLP arrLevelPack);


    /**
     * @return seznam seznamů změn dopadů na uzel
     */
    Collection<Collection<RND>> getRelatedNodeDirections();


    /**
     * @param relatedNodeDirections seznam seznamů změn dopadů na uzel
     */
    void setRelatedNodeDirections(Collection<Collection<RND>> relatedNodeDirections);

}
