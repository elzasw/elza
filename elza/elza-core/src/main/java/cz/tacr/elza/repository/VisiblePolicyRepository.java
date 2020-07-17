package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulPolicyType;
import cz.tacr.elza.domain.UIVisiblePolicy;

/**
 * Repozitory pro {@link UIVisiblePolicy}
 *
 * @author Martin Šlapa
 * @since 15.04.2016
 */
@Repository
public interface VisiblePolicyRepository extends JpaRepository<UIVisiblePolicy, Integer> {

    @Query("SELECT p FROM ui_visible_policy p JOIN p.node n JOIN n.fund f WHERE f = ?1")
    List<UIVisiblePolicy> findByFund(ArrFund fund);

    List<UIVisiblePolicy> findByNode(ArrNode node);

    void deleteByNode(ArrNode node);

    @Query("SELECT p FROM ui_visible_policy p JOIN p.node n JOIN p.policyType pt WHERE n.nodeId IN ?1 AND pt IN ?2")
    List<UIVisiblePolicy> findByNodeIds(Collection<Integer> nodeIds, Collection<RulPolicyType> policyTypes);

    void deleteByNodeIdIn(Collection<Integer> unusedNodes);
}
