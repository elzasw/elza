package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;


/**
 * Repository for item type specifications
 */
@Repository
public interface ItemSpecRepository extends ElzaJpaRepository<RulItemSpec, Integer> {

    List<RulItemSpec> findByItemType(RulItemType itemType);

    List<RulItemSpec> findByRulPackage(RulPackage rulPackage);

    @Query("SELECT s FROM rul_item_spec s WHERE s.code IN :codes")
    List<RulItemSpec> findOneByCodes(@Param("codes") Collection<String> codes);

    @Query("SELECT s FROM rul_item_spec s JOIN FETCH s.itemType t WHERE s.rulPackage = :package AND t.ruleSet = :ruleSet")
    List<RulItemSpec> findByRulPackageAndRuleSet(@Param("package") RulPackage rulPackage,
                                                 @Param("ruleSet") RulRuleSet ruleSet);

    RulItemSpec findOneByCode(String code);
}
