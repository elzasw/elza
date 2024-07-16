package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrInhibitedItem;

public interface InhibitedItemRepository extends JpaRepository<ArrInhibitedItem, Integer> {

    @Modifying
    @Query("DELETE FROM arr_inhibited_item i WHERE i.node IN (SELECT n FROM arr_node n WHERE n.fund = ?1)")
    void deleteByNodeFund(ArrFund fund);

    @Query("SELECT i FROM arr_inhibited_item i WHERE i.descItemObjectId = ?1 AND i.deleteChange IS NULL")
    Optional<ArrInhibitedItem> findByDescItemObjectId(Integer descItemObjectId);

    @Query("SELECT i FROM arr_inhibited_item i WHERE i.nodeId IN (?1) AND i.deleteChange IS NULL")
	List<ArrInhibitedItem> findByNodeIdsAndDeleteChangeIsNull(Collection<Integer> nodeIds);

    @Query("SELECT DISTINCT di.itemId FROM arr_inhibited_item i JOIN arr_desc_item di ON di.descItemObjectId = i.descItemObjectId WHERE i.nodeId IN (?1) AND i.createChange < ?2 AND (i.deleteChange > ?2 OR i.deleteChange IS NULL)")
	Set<Integer> findItemIdsByNodeIdsAndLockChange(Collection<Integer> nodeIds, ArrChange lockChange);

    @Query("SELECT DISTINCT i.descItemObjectId FROM arr_inhibited_item i WHERE i.nodeId IN (?1) AND i.createChange < ?2 AND (i.deleteChange > ?2 OR i.deleteChange IS NULL)")
	Set<Integer> findDescItemObjectIdsByNodeIdsAndLockChange(Collection<Integer> nodeIds, ArrChange lockChange);
}
