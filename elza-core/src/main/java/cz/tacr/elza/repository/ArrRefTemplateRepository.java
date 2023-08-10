package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrRefTemplate;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArrRefTemplateRepository extends ElzaJpaRepository<ArrRefTemplate, Integer> {

    @Query("SELECT rt FROM arr_ref_template rt WHERE rt.fund = :fund")
    List<ArrRefTemplate> findByFund(@Param("fund") ArrFund fund);

    @Modifying
    @Query("DELETE FROM arr_ref_template rt WHERE rt.fund = :fund")
    void deleteByFund(@Param("fund") ArrFund fund);
}
