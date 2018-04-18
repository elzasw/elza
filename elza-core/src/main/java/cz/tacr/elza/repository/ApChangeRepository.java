package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApChange;
import org.springframework.stereotype.Repository;

@Repository
public interface ApChangeRepository extends ElzaJpaRepository<ApChange, Integer> {

}
