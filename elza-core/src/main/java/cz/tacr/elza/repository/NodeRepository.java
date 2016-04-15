package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrNode;

import java.util.List;
import java.util.Set;


/**
 * Respozitory pro nody.
 * @author Martin Šlapa
 * @since 4. 9. 2015
 */
@Repository
public interface NodeRepository extends JpaRepository<ArrNode, Integer>, NodeRepositoryCustom {

    @Query("SELECT distinct n.nodeId FROM arr_node n JOIN n.policies p JOIN n.levels l WHERE l.deleteChange IS NULL AND n.fund = ?1")
    Set<Integer> findNodeIdsForFondWithPolicy(ArrFund fund);

}
