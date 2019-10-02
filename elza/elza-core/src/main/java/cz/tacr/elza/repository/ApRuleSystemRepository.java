package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApRuleSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApRuleSystemRepository extends JpaRepository<ApRuleSystem, Integer>, Packaging<ApRuleSystem> {

}
