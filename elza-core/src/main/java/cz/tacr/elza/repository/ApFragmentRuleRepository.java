package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApFragmentRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApFragmentRuleRepository extends JpaRepository<ApFragmentRule, Integer> {

}
