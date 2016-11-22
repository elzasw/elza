package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParPartyType;


/**
 * Repozitory pro {@link ParComplementType}
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 22.12.2015
 */
@Repository
public interface ComplementTypeRepository extends JpaRepository<ParComplementType, Integer>, Packaging<ParComplementType> {

    /**
     * Najde všechy typy doplňků pro typ osoby.
     *
     * @param parPartyType typ osoby
     * @return typy doplňků
     */
    @Query("SELECT pc.complementType FROM par_party_type_complement_type pc WHERE pc.partyType = ?1")
    List<ParComplementType> findComplementTypesByPartyType(ParPartyType parPartyType);

    ParComplementType findByCode(String partyNameComplementTypeCode);

}
