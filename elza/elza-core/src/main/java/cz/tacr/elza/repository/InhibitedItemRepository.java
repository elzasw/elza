package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrInhibitedItem;
import cz.tacr.elza.domain.ArrNode;

public interface InhibitedItemRepository extends JpaRepository<ArrInhibitedItem, Integer> {

    @Modifying
    @Query("DELETE FROM arr_inhibited_item i WHERE i.node IN (SELECT n FROM arr_node n WHERE n.fund = ?1)")
    void deleteByNodeFund(ArrFund fund);

    @Query("SELECT i FROM arr_inhibited_item i WHERE i.nodeId IN (?1) AND i.deleteChange IS NULL")
	List<ArrInhibitedItem> findByNodeIdsAndDeleteChangeIsNull(Collection<Integer> nodeIds);

    @Query("SELECT i FROM arr_inhibited_item i WHERE i.nodeId IN (?1) AND i.createChange < ?2 AND (i.deleteChange > ?2 OR i.deleteChange IS NULL)")
	List<ArrInhibitedItem> findByNodeIdsAndLockChange(Collection<Integer> nodeIds, ArrChange lockChange);
}
