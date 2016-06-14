package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulDefaultItemType;
import cz.tacr.elza.domain.RulRuleSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pro vazební tabulku mezi pravidly a atributy.
 *
 * @author Pavel Stánek
 * @since 10.06.2016
 */
@Repository
public interface DefaultItemTypeRepository extends JpaRepository<RulDefaultItemType, Integer> {
    List<RulDefaultItemType> findByRuleSet(RulRuleSet rulRuleSet);

    /**
     * Načtení implicitního seřazeného seznamu kódů atributů pro zobrazení v tabulce.
     *
     * @param rulRuleSet
     * @return seřazený seznam kódů atributů tak, jak mají být v tabulce
     */
    @Query("select it.code from rul_default_item_type dit inner join dit.itemType it where dit.ruleSet = :ruleSet order by it.viewOrder")
    List<String> findItemTypeCodes(@Param("ruleSet") RulRuleSet rulRuleSet);

    void deleteByRuleSet(RulRuleSet rulRuleSet);
}
