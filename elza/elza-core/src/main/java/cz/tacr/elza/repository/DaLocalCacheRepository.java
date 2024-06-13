package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaLocalCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DaLocalCacheRepository extends JpaRepository<DaLocalCache, Integer> {
}
