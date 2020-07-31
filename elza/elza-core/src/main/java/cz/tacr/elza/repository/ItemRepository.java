package cz.tacr.elza.repository;

import cz.tacr.elza.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

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

    @Query("SELECT i FROM arr_item i LEFT JOIN FETCH i.data WHERE i.descItemObjectId = :descItemObjectId AND i.createChange < :lockChange AND (i.deleteChange > :lockChange OR i.deleteChange IS NULL)")
    ArrItem findByItemObjectIdAndChangeFetchData(@Param("descItemObjectId") int descItemObjectId, @Param("lockChange") ArrChange lockChange);

    @Query("SELECT COUNT(i) FROM arr_item i JOIN i.itemType t WHERE i.itemType = ?1")
    long getCountByType(RulItemType itemType);

    @Query("SELECT i FROM arr_item i "
           + "inner join i.deleteChange c "
           + "inner join c.primaryNode n "
           + "inner join n.fund f "
           + "where f = ?1")
    List<ArrItem> findHistoricalByFund(ArrFund fund);

    @Query("SELECT i FROM arr_item i "
            + "JOIN i.createChange c "
            + "JOIN c.primaryNode n "
            + "JOIN n.fund f "
            + "WHERE f = :fund")
    List<ArrItem> findByFund(@Param("fund") ArrFund fund);

    List<ArrItem> findByData(ArrData arrData);

    @Modifying
    @Query("UPDATE arr_item SET createChange = :change WHERE itemId IN :itemIds")
    void updateCreateChange(@Param("itemIds") Collection<Integer> itemIds, @Param("change") ArrChange change);
}
