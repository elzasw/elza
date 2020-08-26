package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputTemplate;
import org.springframework.data.repository.query.Param;

/**
 * Respozitory pro pro výstupní šablonu.
 */
public interface OutputTemplateRepository extends ElzaJpaRepository<ArrOutputTemplate, Integer> {

    @Query("SELECT ot FROM arr_output_template ot JOIN FETCH ot.template t WHERE ot.output=?1")
    List<ArrOutputTemplate> findAllByOutputFetchTemplate(ArrOutput output);

    @Modifying
    @Query("DELETE FROM arr_output_template t WHERE t.outputId IN (SELECT o.outputId FROM arr_output o WHERE o.fund=?1)")
    void deleteByFund(ArrFund fund);

    @Modifying
    void deleteByOutputIdAndTemplateId(int outputId, int templateId);

    @Modifying
    @Query("DELETE FROM arr_output_template WHERE outputTemplateId IN (SELECT ot.outputTemplateId FROM arr_output_template ot " +
            "JOIN ot.output o " +
            "WHERE o.deleteChange IS NOT NULL AND o.fund = :fund)")
    void deleteByFundAndDeleteChangeIsNotNull(@Param("fund") ArrFund fund);
}
