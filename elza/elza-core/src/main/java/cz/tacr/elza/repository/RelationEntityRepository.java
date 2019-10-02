package cz.tacr.elza.repository;

import java.util.List;

import cz.tacr.elza.domain.ApAccessPoint;
import org.springframework.data.jpa.repository.Query;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationEntity;


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
     * @param record rejstřík
     * @return seznam aktivních vazeb
     */
    @Query("SELECT re FROM par_relation_entity re JOIN FETCH re.relation r JOIN FETCH r.party p WHERE re.accessPoint = ?1")
	List<ParRelationEntity> findByAccessPoint(ApAccessPoint record);


    /**
     * Najde počet vazeb které jsou vázané na předaný rejstřík
     * @param record rejstřík
     * @return počet vazeb
     */
    long countAllByAccessPoint(ApAccessPoint record);

    /**
     * Najde vazby které aktivní a jsou vázané na předaný rejstřík
     * @param record rejstřík
     * @return seznam aktivních vazeb
     */
    @Query("SELECT re" +
            " FROM par_relation_entity re" +
            " JOIN FETCH re.relation r" +
            " JOIN FETCH r.party p" +
            " JOIN ap_state s ON s.accessPoint = p.accessPoint" +
            " WHERE re.accessPoint = ?1" +
            " AND s.deleteChange IS NULL")
	List<ParRelationEntity> findActiveByRecord(ApAccessPoint record);
}
