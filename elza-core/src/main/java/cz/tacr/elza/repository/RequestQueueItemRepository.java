package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.domain.ArrRequestQueueItem;
import cz.tacr.elza.repository.vo.ItemChange;
import cz.tacr.elza.service.arrangement.DeleteFundHistory;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;


/**
 * @author Martin Šlapa
 * @since 09.12.2016
 */
@Repository
public interface RequestQueueItemRepository extends ElzaJpaRepository<ArrRequestQueueItem, Integer>, RequestQueueItemRepositoryCustom, DeleteFundHistory {

    ArrRequestQueueItem findByRequestAndSend(ArrRequest request, Boolean send);

    List<ArrRequestQueueItem> findBySendOrderByCreateChangeAsc(boolean send);

    @Query("SELECT rqi FROM arr_request_queue_item rqi WHERE rqi.request IN (?1)")
    List<ArrRequestQueueItem> findByRequest(Collection<? extends ArrRequest> request);

    @Query("SELECT rqi FROM arr_request_queue_item rqi WHERE rqi.request = ?1")
    ArrRequestQueueItem findByRequest(ArrRequest request);

    @Modifying
    @Query("DELETE FROM arr_request_queue_item i WHERE i.requestId IN (SELECT d.requestId FROM arr_request d WHERE d.fund = ?1)")
    void deleteByFund(ArrFund fund);

    @Override
    @Query("SELECT new cz.tacr.elza.repository.vo.ItemChange(rqi.requestQueueItemId, rqi.createChange.changeId) FROM arr_request_queue_item rqi "
            + "JOIN rqi.request r "
            + "WHERE r.fund = :fund")
    List<ItemChange> findByFund(@Param("fund") ArrFund fund);

    @Override
    @Modifying
    @Query("UPDATE arr_request_queue_item SET createChange = :change WHERE requestQueueItemId IN :ids")
    void updateCreateChange(@Param("ids") Collection<Integer> ids, @Param("change") ArrChange change);

}
