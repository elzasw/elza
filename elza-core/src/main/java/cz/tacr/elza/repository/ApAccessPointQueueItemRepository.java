package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApAccessPointQueueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ApAccessPointQueueItemRepository extends JpaRepository<ApAccessPointQueueItem, Integer> {

    @Query("SELECT (COUNT(q) > 0) FROM ap_access_point_queue_item q")
    boolean isQueuePopulated();
}
