package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationEntity;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


/**
 * Repozitory pro {@link ParRelationEntity}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 04.01.2016
 */
public interface RelationEntityRepository extends ElzaJpaRepository<ParRelationEntity, Integer> {


    /**
     * Najde všechny vazby dané osoby.
     *
     * @param party osoba
     * @return seznam vazeb osoby
     */
    @Query("SELECT re FROM par_relation_entity re JOIN re.relation r WHERE r.party = ?1")
    List<ParRelationEntity> findByParty(ParParty party);


    /**
     * Najde vazby podle vztahu.
     *
     * @param relation vztah
     * @return seznam vazeb vztahu
     */
    List<ParRelationEntity> findByRelation(ParRelation relation);
}
