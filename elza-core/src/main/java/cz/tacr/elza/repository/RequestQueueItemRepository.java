package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.domain.ArrRequestQueueItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


/**
 * @author Martin Šlapa
 * @since 09.12.2016
 */
@Repository
public interface RequestQueueItemRepository extends ElzaJpaRepository<ArrRequestQueueItem, Integer>, RequestQueueItemRepositoryCustom {

    ArrRequestQueueItem findByRequestAndSend(ArrRequest request, boolean send);
}
