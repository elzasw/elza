package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ExtSyncsQueueItem;
import org.springframework.stereotype.Repository;

@Repository
public interface ExtSyncsQueueItemRepository extends ElzaJpaRepository<ExtSyncsQueueItem, Integer>, ExtSyncsQueueItemRepositoryCustom{

}
