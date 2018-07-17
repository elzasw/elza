package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApRuleRepository extends JpaRepository<ApRule, Integer> {

}
