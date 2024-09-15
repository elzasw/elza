package cz.tacr.elza.repository;

import cz.tacr.elza.api.AipType;
import cz.tacr.elza.domain.DaAip;
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

    @Query("select l from da_local_cache l join l.syncQueueItem q where l.aipState = :aipState and l.aipType in :aipTypes and q.state in :queueItemStates")
    DaLocalCache findByAipStateAndAipTypeIn(@Param("aipState") DaAipState aipState,
                                            @Param("aipTypes") Collection<AipType> aipTypes,
                                            @Param("queueItemStates") Collection<DaSyncQueueItem.QueueItemState> queueItemStates);

    @Query("select l from da_local_cache l join l.syncQueueItem q join l.aipState s where s.daAip = :aip and q.state in :queueItemStates")
    DaLocalCache findByAipAndQueueItemStatesIn(@Param("aip") DaAip aip,
                                               @Param("queueItemStates") Collection<DaSyncQueueItem.QueueItemState> queueItemStates);

    @Query("select l from da_local_cache l join l.syncQueueItem q join l.aipState s where s.daAip in :aips and q.state in :queueItemStates")
    List<DaLocalCache> findByAipInAndQueueItemStatesIn(@Param("aips") List<DaAip> aips,
                                                       @Param("queueItemStates") Collection<DaSyncQueueItem.QueueItemState> queueItemStates);

    List<DaLocalCache> findBySyncQueueItemIn(Collection<DaSyncQueueItem> syncQueueItems);
}
