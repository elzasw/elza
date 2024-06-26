package cz.tacr.elza.repository;

import cz.tacr.elza.api.AipType;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaLocalCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface DaLocalCacheRepository extends JpaRepository<DaLocalCache, Integer> {

    DaLocalCache findByAipStateAndAipTypeIn(DaAipState aipState, Collection<AipType> aipTypes);
}
