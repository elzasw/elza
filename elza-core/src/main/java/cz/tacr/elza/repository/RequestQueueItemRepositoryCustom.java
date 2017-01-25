package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrRequestQueueItem;
import org.springframework.stereotype.Repository;


/**
 * @author Martin Šlapa
 * @since 12.12.2016
 */
@Repository
public interface RequestQueueItemRepositoryCustom {

    ArrRequestQueueItem findNext(final Integer externalSystemId);

}
