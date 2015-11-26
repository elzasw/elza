package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrNodeConformityErrors;
import cz.tacr.elza.domain.ArrNodeConformityInfo;


/**
 * Repozitory pro {@link ArrNodeConformityErrors}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.11.2015
 */
@Repository
public interface NodeConformityErrorsRepository extends JpaRepository<ArrNodeConformityErrors, Integer> {

    /**
     * Najde všechny záznamy pro dané stavy.
     *
     * @param infos hledané stavy záznamů
     * @return všechny záznamy s danými stavy
     */
    @Query("SELECT c FROM arr_node_conformity_errors c WHERE c.nodeConformityInfo in (?1)")
    List<ArrNodeConformityErrors> findByNodeConformityInfos(Collection<ArrNodeConformityInfo> infos);

}
