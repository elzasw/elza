package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * Repozitory pro {@link ParRelation}
 */
@Repository
public interface PartyRelationRepository extends JpaRepository<ParRelation, Integer> {

    /**
     * Vrátí vazby osoby.
     * @param party osoba
     * @return  vazby
     */
    @Query("SELECT pr FROM par_relation pr WHERE pr.party = ?1")
    List<ParRelation> findByParty(ParParty party);

}
