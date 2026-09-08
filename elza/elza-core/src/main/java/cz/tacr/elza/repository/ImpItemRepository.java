package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import cz.tacr.elza.domain.ImpBatch;
import cz.tacr.elza.domain.ImpItem;
import cz.tacr.elza.domain.ItemState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ImpItemRepository extends JpaRepository<ImpItem, Integer> {

    /**
     * Marks every item still recorded as running as ready to be picked up again. Called during
     * startup recovery, so no in-flight worker will touch the same rows.
     */
    @Modifying
    @Query("UPDATE cz.tacr.elza.domain.ImpItem i "
         + "   SET i.state = cz.tacr.elza.domain.ItemState.READY "
         + " WHERE i.state = cz.tacr.elza.domain.ItemState.RUNNING")
    int resetRunningToReady();

    /**
     * Returns the DMS file ids of every item in the batch that has one - used before the batch
     * (and its items) are dropped so the physical files can be removed.
     */
    @Query("SELECT i.dmsFile.fileId FROM cz.tacr.elza.domain.ImpItem i "
         + " WHERE i.batch = :batch AND i.dmsFile IS NOT NULL")
    List<Integer> findDmsFileIdsByBatch(cz.tacr.elza.domain.ImpBatch batch);

    /**
     * Clears the DMS reference on every item of the batch. Bulk update so a subsequent batch
     * delete does not trigger a Hibernate save-cascade over the item graph.
     */
    @Modifying
    @Query("UPDATE cz.tacr.elza.domain.ImpItem i SET i.dmsFile = null WHERE i.batch = :batch")
    int clearDmsRefsForBatch(cz.tacr.elza.domain.ImpBatch batch);

    List<ImpItem> findByBatchAndStateInOrderByExecOrderAsc(ImpBatch batch, Collection<ItemState> states);

    List<ImpItem> findByBatchOrderByExecOrderAsc(ImpBatch batch);

    @Query("SELECT COALESCE(MAX(i.execOrder), 0) FROM cz.tacr.elza.domain.ImpItem i WHERE i.batch = :batch")
    int findMaxExecOrder(ImpBatch batch);
}
