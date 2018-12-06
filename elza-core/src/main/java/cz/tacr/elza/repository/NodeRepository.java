package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;

/**
 * Respozitory pro nody.
 */
@Repository
public interface NodeRepository extends ElzaJpaRepository<ArrNode, Integer>, NodeRepositoryCustom {

    @Query("SELECT distinct n.nodeId FROM arr_node n JOIN n.policies p JOIN n.levels l " +
            "WHERE l.deleteChange IS NULL AND n.fund = ?1")
    List<Integer> findNodeIdsForFondWithPolicy(ArrFund fund);

    ArrNode findOneByUuid(String uuid);

    List<ArrNode> findByUuid(Collection<String> uuids);

    @Modifying
    void deleteByFund(ArrFund fund);
}
