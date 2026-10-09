package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulEntityRule;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPartType;

/**
 * Repository of {@link RulEntityRule}.
 */
@Repository
public interface EntityRuleRepository extends JpaRepository<RulEntityRule, Integer> {

    List<RulEntityRule> findByRulPackage(RulPackage rulPackage);

    void deleteByRulPackage(RulPackage rulPackage);

    /** Rules of other packages than the given one referring to the classes. */
    @Query("SELECT r FROM rul_entity_rule r WHERE r.apType IN :apTypes AND r.rulPackage <> :rulPackage")
    List<RulEntityRule> findForeignByApTypes(@Param("apTypes") Collection<ApType> apTypes,
                                             @Param("rulPackage") RulPackage rulPackage);

    /** Rules of other packages than the given one referring to the part types. */
    @Query("SELECT r FROM rul_entity_rule r WHERE r.partType IN :partTypes AND r.rulPackage <> :rulPackage")
    List<RulEntityRule> findForeignByPartTypes(@Param("partTypes") Collection<RulPartType> partTypes,
                                               @Param("rulPackage") RulPackage rulPackage);

    @Query("SELECT r FROM rul_entity_rule r JOIN FETCH r.component ORDER BY r.priority, r.entityRuleId")
    List<RulEntityRule> findAllFetchOrderByPriority();
}
