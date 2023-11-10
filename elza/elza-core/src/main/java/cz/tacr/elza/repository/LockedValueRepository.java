package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.repository.vo.ItemChange;
import cz.tacr.elza.service.arrangement.DeleteFundHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrLockedValue;
import cz.tacr.elza.domain.RulItemType;

public interface LockedValueRepository extends JpaRepository<ArrLockedValue, Integer>, DeleteFundHistory {

    @Query("select lv from arr_locked_value lv " +
            "join fetch lv.item it " +
            "join fetch it.itemType ity " +
            "join fetch ity.dataType " +
            "where lv.fund = :fund and it.itemType = :itemType")
    List<ArrLockedValue> findByFundAndItemType(@Param("fund") ArrFund fund,
                                             @Param("itemType") RulItemType itemType);

    @Modifying
    @Query("DELETE FROM arr_locked_value lv " +
            "WHERE lv.fund = :fund AND lv.createChange.changeId >= :changeId")
    void deleteToChange(@Param("fund") ArrFund fund, @Param("changeId") Integer changeId);

    /**
     * Find if given value is locked
     *
     * @param fund
     * @param itemType
     * @param value
     * @return
     */
    @Query("select lv from arr_locked_value lv " +
            "join lv.item it " +
            "join it.data dt " +
            "where lv.fund = :fund and it.itemType= :itemType " +
            "and treat(dt as cz.tacr.elza.domain.ArrDataUnitid).unitId= :unitId")
    ArrLockedValue findByFundAndItemTypeAndValue(@Param("fund") ArrFund fund,
                                                 @Param("itemType") RulItemType itemType,
                                                 @Param("unitId") String value);

    @Modifying
    int deleteByFund(ArrFund fund);

    @Override
    @Query("SELECT new cz.tacr.elza.repository.vo.ItemChange(lv.lockedValueId, lv.createChange.changeId) FROM arr_locked_value lv "
            + "WHERE lv.fund = :fund")
    List<ItemChange> findByFund(@Param("fund") ArrFund fund);

    @Override
    @Modifying
    @Query("UPDATE arr_locked_value SET createChange = :change WHERE lockedValueId IN :ids")
    void updateCreateChange(@Param("ids") Collection<Integer> ids, @Param("change") ArrChange change);
}
