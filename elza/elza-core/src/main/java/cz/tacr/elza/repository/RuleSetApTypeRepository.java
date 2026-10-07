package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSetApType;

/**
 * Repository of {@link RulRuleSetApType}.
 */
@Repository
public interface RuleSetApTypeRepository extends JpaRepository<RulRuleSetApType, Integer> {

    List<RulRuleSetApType> findByRulPackage(RulPackage rulPackage);

    void deleteByRulPackage(RulPackage rulPackage);

    /** Members declared by other packages than the given one for the classes. */
    @Query("SELECT m FROM rul_rule_set_ap_type m WHERE m.apType IN :apTypes AND m.rulPackage <> :rulPackage")
    List<RulRuleSetApType> findForeignByApTypes(@Param("apTypes") Collection<ApType> apTypes,
                                                @Param("rulPackage") RulPackage rulPackage);
}
