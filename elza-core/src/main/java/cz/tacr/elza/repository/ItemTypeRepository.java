package cz.tacr.elza.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;


/**
 * Repository for RulItemType
 *
 */
@Repository
public interface ItemTypeRepository extends ElzaJpaRepository<RulItemType, Integer> {

    List<RulItemType> findByRulPackage(RulPackage rulPackage);

    List<RulItemType> findByRuleSet(RulRuleSet ruleSet);

    List<RulItemType> findByRulPackageOrderByViewOrderAsc(RulPackage rulPackage);

    /**
     * Return item type with the highest view-order
     * @return return item with highest view_order
     */
    RulItemType findFirstByOrderByViewOrderDesc();


    void deleteByRulPackage(RulPackage rulPackage);


    RulItemType findOneByCode(String code);

    @Query(value = "SELECT t FROM rul_item_type t  WHERE t.code in (?1)")
    Set<RulItemType> findByCode(Set<String> descItemTypeCodes);

    List<RulItemType> findByRulPackageAndRuleSet(RulPackage rulPackage, RulRuleSet rulRuleSet);
}
