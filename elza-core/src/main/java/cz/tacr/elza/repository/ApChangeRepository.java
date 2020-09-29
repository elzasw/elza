package cz.tacr.elza.repository;

import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApChange;

@Repository
public interface ApChangeRepository extends ElzaJpaRepository<ApChange, Integer> {

    ApChange findTop1ByOrderByChangeIdDesc();
}
