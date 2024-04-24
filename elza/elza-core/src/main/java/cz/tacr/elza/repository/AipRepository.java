package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrAip;
import org.springframework.stereotype.Repository;


@Repository
public interface AipRepository extends ElzaJpaRepository<ArrAip, Integer>, AipRepositoryCustom {
}
