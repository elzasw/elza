package cz.tacr.elza.repository;

import cz.tacr.elza.api.AipType;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaLocalCache;
import cz.tacr.elza.domain.DaSyncQueueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DaLocalCacheRepository extends JpaRepository<DaLocalCache, Integer> {

    @Query("select l from da_local_cache l join l.syncQueueItem q where l.aipState = :aipState and l.aipType in :aipTypes and q.state = :queueItemState")
    DaLocalCache findByAipStateAndAipTypeIn(@Param("aipState") DaAipState aipState,
                                            @Param("aipTypes") Collection<AipType> aipTypes,
                                            @Param("queueItemState") DaSyncQueueItem.QueueItemState queueItemState);

    DaLocalCache findByAipState(DaAipState aipState);

    List<DaLocalCache> findBySyncQueueItemIn(Collection<DaSyncQueueItem> syncQueueItems);
}
