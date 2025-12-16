package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulArrangementExtension;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;


/**
 * Repository pro {@link RulArrangementExtension}.
 *
 * @since 20.10.2017
 */
@Repository
public interface ArrangementExtensionRepository extends JpaRepository<RulArrangementExtension, Integer> {

    List<RulArrangementExtension> findByRulPackage(RulPackage rulPackage);

    List<RulArrangementExtension> findByRuleSet(RulRuleSet ruleSet);

    List<RulArrangementExtension> findByRulPackageAndRuleSet(RulPackage rulPackage, RulRuleSet ruleSet);

    void deleteByRulPackage(RulPackage rulPackage);

    @Query("SELECT ae FROM arr_node_extension ne JOIN ne.arrangementExtension ae WHERE ne.node = :node AND ne.deleteChange IS NULL ORDER BY ae.name")
    List<RulArrangementExtension> findByNode(@Param("node") ArrNode node);

    @Query("SELECT ae FROM arr_node_extension ne JOIN ne.arrangementExtension ae WHERE ne.nodeId IN (:nodeIds) AND ne.deleteChange IS NULL GROUP BY ae ORDER BY ae.name")
    List<RulArrangementExtension> findByNodeIds(@Param("nodeIds") Collection<Integer> nodeIds);

    List<RulArrangementExtension> findByCodeIn(List<String> codes);

}
