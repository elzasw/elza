package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrNodeConformityInfo;
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
     * Najde všechny záznamy pro dané stavy.
     *
     * @param infos hledané stavy záznamů
     * @return všechny záznamy s danými stavy
     */
    @Query("SELECT c FROM arr_node_conformity_missing c WHERE c.nodeConformityInfo in (?1)")
    List<ArrNodeConformityMissing> findByNodeConformityInfos(Collection<ArrNodeConformityInfo> infos);

}
