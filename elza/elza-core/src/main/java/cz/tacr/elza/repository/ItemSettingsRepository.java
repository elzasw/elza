package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrItemSettings;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.RulItemType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


/**
 * Repository pro {@link ArrItemSettings}
 *
 * @since 27.06.2016
 */
public interface ItemSettingsRepository extends JpaRepository<ArrItemSettings, Integer> {

    List<ArrItemSettings> findByOutput(ArrOutput output);

    ArrItemSettings findOneByOutputAndItemType(ArrOutput output, RulItemType itemType);

    // void deleteByOutput(ArrOutput output);

    void deleteByOutputFund(ArrFund fund);

    @Modifying
    @Query("DELETE FROM arr_item_settings WHERE itemSettingsId IN (SELECT s.itemSettingsId FROM arr_item_settings s " +
            "JOIN s.output o " +
            "WHERE o.deleteChange IS NOT NULL AND o.fund = :fund)")
    void deleteByFundAndDeleteChangeIsNotNull(@Param("fund") ArrFund fund);
}
