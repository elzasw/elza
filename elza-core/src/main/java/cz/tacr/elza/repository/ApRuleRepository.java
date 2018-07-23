package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApRule;
import cz.tacr.elza.domain.ApRuleSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ApRuleRepository extends JpaRepository<ApRule, Integer> {

    List<ApRule> findByRuleSystemIn(Collection<ApRuleSystem> ruleSystems);
}
