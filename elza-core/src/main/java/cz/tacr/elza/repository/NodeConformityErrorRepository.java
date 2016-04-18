package cz.tacr.elza.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cz.tacr.elza.domain.ArrFundVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrNodeConformity;
import cz.tacr.elza.domain.ArrNodeConformityError;


/**
 * Repozitory pro {@link ArrNodeConformityError}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.11.2015
 */
@Repository
public interface NodeConformityErrorRepository extends JpaRepository<ArrNodeConformityError, Integer> {

    /**
     * Najde seznam chybně vyplněných atributů daného ArrNodeConformity.
     *
     * @param info informace o chybě
     * @return seznam chybně vyplněných atributů
     */
    List<ArrNodeConformityError> findByNodeConformity(ArrNodeConformity info);


    /**
     * Najde všechny záznamy pro dané stavy.
     *
     * @param infos hledané stavy záznamů
     * @return všechny záznamy s danými stavy
     */
    @Query("SELECT c FROM arr_node_conformity_error c WHERE c.nodeConformity in (?1)")
    List<ArrNodeConformityError> findByNodeConformityInfos(Collection<ArrNodeConformity> infos);

    @Query("SELECT c FROM arr_node_conformity_error c JOIN c.nodeConformity n WHERE n.nodeConformityId in (?1)")
    List<ArrNodeConformityError> findByConformityIds(Collection<Integer> conformityInfoIds);

    @Query("SELECT c FROM arr_node_conformity_error c JOIN FETCH c.nodeConformity n WHERE n.fundVersion = ?1 AND n.state = 'ERR'")
    List<ArrNodeConformityError> findErrorsByFundVersion(ArrFundVersion fundVersion);
}
