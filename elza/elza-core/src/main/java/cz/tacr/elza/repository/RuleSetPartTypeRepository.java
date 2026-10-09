package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.RulRuleSetPartType;

/**
 * Repository of {@link RulRuleSetPartType}.
 */
@Repository
public interface RuleSetPartTypeRepository extends JpaRepository<RulRuleSetPartType, Integer> {

    List<RulRuleSetPartType> findByRulPackage(RulPackage rulPackage);

    void deleteByRulPackage(RulPackage rulPackage);

    /** Members declared by other packages than the given one for the part types. */
    @Query("SELECT m FROM rul_rule_set_part_type m WHERE m.partType IN :partTypes AND m.rulPackage <> :rulPackage")
    List<RulRuleSetPartType> findForeignByPartTypes(@Param("partTypes") Collection<RulPartType> partTypes,
                                                    @Param("rulPackage") RulPackage rulPackage);
}
