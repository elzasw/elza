package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.RulItemType;

/**
 * Repository for ArrItem
 * 
 * @since 17.06.2016
 */
@Repository
public interface ItemRepository extends JpaRepository<ArrItem, Integer> {

    @Query(value = "SELECT coalesce(max(i.descItemObjectId), 0) FROM arr_item i")
    Integer findMaxItemObjectId();

    @Query("SELECT i FROM arr_item i LEFT JOIN FETCH i.data WHERE i.descItemObjectId = ?1 AND i.deleteChange IS NULL")
    ArrItem findByItemObjectIdAndDeleteChangeIsNullFetchData(int descItemObjectId);

    @Query("SELECT i FROM arr_item i LEFT JOIN FETCH i.data WHERE i.descItemObjectId = ?1 AND i.createChange < :lockChange AND (i.deleteChange > :lockChange OR i.deleteChange IS NULL)")
    ArrItem findByItemObjectIdAndChangeFetchData(int descItemObjectId, ArrChange lockChange);

    @Query("SELECT COUNT(i) FROM arr_item i JOIN i.itemType t WHERE i.itemType = ?1")
    long getCountByType(RulItemType itemType);
}