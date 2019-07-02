package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApState;
import org.springframework.stereotype.Repository;

@Repository
public interface ApStateRepository extends ElzaJpaRepository<ApState, Integer> {

}
