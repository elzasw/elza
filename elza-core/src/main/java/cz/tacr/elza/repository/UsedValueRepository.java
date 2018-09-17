package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrUsedValue;
import cz.tacr.elza.domain.RulItemType;

public interface UsedValueRepository extends JpaRepository<ArrUsedValue, Integer> {
    ArrUsedValue findByFundAndItemTypeAndValue(ArrFund fund, RulItemType itemType, String value);

    @Query("select uv.value from arr_used_value uv where uv.fund = :fund and uv.itemType = :itemType")
    List<String> findByFundAndItemType(@Param("fund") ArrFund fund, @Param("itemType") RulItemType itemType);

    @Query("select uv.value from arr_used_value uv where uv.fund = :fund "
            + "and uv.itemType = :itemType and value like :prefix")
    List<String> findByFundAndItemTypeAndPrefix(@Param("fund") ArrFund fund, @Param("itemType") RulItemType itemType,
                                                @Param("prefix") String prefix);
}
