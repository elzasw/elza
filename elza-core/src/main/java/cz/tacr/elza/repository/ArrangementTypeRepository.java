package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulArrangementType;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Repository
public interface ArrangementTypeRepository extends JpaRepository<RulArrangementType, Integer> {

    @Query(value = "select rat from rul_arrangement_type rat join rat.ruleSet rrs where rrs.ruleSetId = ?1")
    List<RulArrangementType> findByRuleSetId(Integer ruleSetId);

}
