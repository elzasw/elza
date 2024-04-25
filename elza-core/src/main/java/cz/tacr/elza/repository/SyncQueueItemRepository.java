package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaSyncQueueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface SyncQueueItemRepository extends JpaRepository<DaSyncQueueItem, Integer> {

}
