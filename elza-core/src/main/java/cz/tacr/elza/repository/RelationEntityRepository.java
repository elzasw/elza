package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.domain.RegRecord;


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

    /**
     * Najde vazby které jsou vázané na předaný rejstřík
     * @param record
     * @return
     */
    @Query("SELECT re FROM par_relation_entity re JOIN FETCH re.relation r JOIN FETCH r.party p WHERE re.record = ?1")
	List<ParRelationEntity> findByRecord(RegRecord record);
}
