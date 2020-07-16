package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNodeConformity;
import cz.tacr.elza.domain.ArrNodeConformityMissing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;


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

    @Query("SELECT c FROM arr_node_conformity_missing c JOIN c.nodeConformity n WHERE n.nodeConformityId in (?1)")
    List<ArrNodeConformityMissing> findByConformityIds(Collection<Integer> conformityInfoIds);

    @Query("SELECT c FROM arr_node_conformity_missing c JOIN FETCH c.nodeConformity n WHERE n.fundVersion = ?1 AND n.state = 'ERR'")
    List<ArrNodeConformityMissing> findMissingsByFundVersion(ArrFundVersion fundVersion);

    @Modifying
    @Query("DELETE FROM arr_node_conformity_missing nc WHERE nc.nodeConformity IN (SELECT n FROM arr_node_conformity n WHERE n.node.fund = ?1)")
    void deleteByNodeConformityNodeFund(ArrFund fund);

    void deleteByNodeConformityNodeIdIn(Collection<Integer> nodeIds);
    
}
