package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrNodeConformity;
import cz.tacr.elza.domain.ArrNodeConformityMissing;


/**
 * Repozitář pro {@link ArrNodeConformityMissing}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.11.2015
 */
@Repository
public interface NodeConformityMissingRepository extends JpaRepository<ArrNodeConformityMissing, Integer> {

    /**
     * Najde seznam chybějících atributů daného ArrNodeConformity.
     *
     * @param info informace o chybě
     * @return seznam chybějících atributů
     */
    List<ArrNodeConformityMissing> findByNodeConformity(ArrNodeConformity info);


    /**
     * Najde všechny záznamy pro dané stavy.
     *
     * @param infos hledané stavy záznamů
     * @return všechny záznamy s danými stavy
     */
    @Query("SELECT c FROM arr_node_conformity_missing c WHERE c.nodeConformity in (?1)")
    List<ArrNodeConformityMissing> findByNodeConformityInfos(Collection<ArrNodeConformity> infos);

}
