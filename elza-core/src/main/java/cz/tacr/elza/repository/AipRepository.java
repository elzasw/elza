package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaAip;
import org.springframework.stereotype.Repository;


@Repository
public interface AipRepository extends ElzaJpaRepository<DaAip, Integer>, AipRepositoryCustom {

    DaAip findByCode(String code);
}
