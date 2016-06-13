package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulDefaultItemType;
import cz.tacr.elza.domain.RulRuleSet;
import org.springframework.data.jpa.repository.JpaRepository;
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

    void deleteByRuleSet(RulRuleSet rulRuleSet);
}
