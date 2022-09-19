package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.repository.vo.ItemChange;
import cz.tacr.elza.service.arrangement.DeleteFundHistory;
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
public interface ItemRepository extends JpaRepository<ArrItem, Integer>, DeleteFundHistory {

    @Query("SELECT coalesce(max(i.descItemObjectId), 0) FROM arr_item i")
    Integer findMaxItemObjectId();

    @Query("SELECT i FROM arr_item i WHERE i.deleteChange IS NULL")
    List<ArrItem> findByDeleteChangeIsNull();

    @Query("SELECT i FROM arr_item i LEFT JOIN FETCH i.data WHERE i.descItemObjectId = ?1 AND i.deleteChange IS NULL")
    ArrItem findByItemObjectIdAndDeleteChangeIsNullFetchData(int descItemObjectId);

    @Query("SELECT i FROM arr_item i LEFT JOIN FETCH i.data WHERE i.descItemObjectId = :descItemObjectId AND i.createChange < :lockChange AND (i.deleteChange > :lockChange OR i.deleteChange IS NULL)")
    ArrItem findByItemObjectIdAndChangeFetchData(@Param("descItemObjectId") int descItemObjectId, @Param("lockChange") ArrChange lockChange);

    @Query("SELECT COUNT(i) FROM arr_item i JOIN i.itemType t WHERE i.itemType = ?1")
    long getCountByType(RulItemType itemType);

    @Query("SELECT i FROM arr_item i "
            + "LEFT JOIN arr_desc_item di ON di.itemId = i.itemId "
            + "LEFT JOIN arr_output_item oi ON oi.itemId = i.itemId "
            + "LEFT JOIN arr_structured_item si ON si.itemId = i.itemId "
            + "LEFT JOIN di.node din "
            + "LEFT JOIN oi.output oio "
            + "LEFT JOIN si.structuredObject sis "
            + "LEFT JOIN din.fund dif "
            + "LEFT JOIN oio.fund oif "
            + "LEFT JOIN sis.fund sif "
            + "WHERE (i.deleteChange IS NOT NULL OR oio.deleteChange IS NOT NULL OR sis.deleteChange IS NOT NULL) AND (dif = :fund OR oif = :fund OR sif = :fund)")
    List<ArrItem> findHistoricalByFund(@Param("fund") ArrFund fund);

    @Query("SELECT data.recordId FROM arr_fund_version fv "
           + "LEFT JOIN arr_node n ON n.fund = fv.fund "
           + "LEFT JOIN arr_level l ON l.node = n "
           + "LEFT JOIN arr_desc_item d ON d.node = l.node "
           + "LEFT JOIN arr_item i ON i.itemId = d.itemId "
           + "LEFT JOIN i.data data "
           + "RIGHT JOIN arr_data_record_ref ref ON ref.dataId = data.dataId "
           + "WHERE fv.fundVersionId IN :fundVersionIds AND l.deleteChange IS NULL AND i.deleteChange IS NULL")
    List<Integer> findArrDataRecordRefRecordIdsByFundVersionIds(@Param("fundVersionIds") Collection<Integer> fundVersionIds);

    @Override
    @Query("SELECT new cz.tacr.elza.repository.vo.ItemChange(i.itemId, i.createChange.changeId) FROM arr_item i "
            + "LEFT JOIN arr_desc_item di ON di.itemId = i.itemId "
            + "LEFT JOIN arr_output_item oi ON oi.itemId = i.itemId "
            + "LEFT JOIN arr_structured_item si ON si.itemId = i.itemId "
            + "LEFT JOIN di.node din "
            + "LEFT JOIN oi.output oio "
            + "LEFT JOIN si.structuredObject sis "
            + "LEFT JOIN din.fund dif "
            + "LEFT JOIN oio.fund oif "
            + "LEFT JOIN sis.fund sif "
            + "WHERE dif = :fund OR oif = :fund OR sif = :fund")
    List<ItemChange> findByFund(@Param("fund") ArrFund fund);

    List<ArrItem> findByData(ArrData arrData);

    @Override
    @Modifying
    @Query("UPDATE arr_item SET createChange = :change WHERE itemId IN :itemIds")
    void updateCreateChange(@Param("itemIds") Collection<Integer> itemIds, @Param("change") ArrChange change);
}
