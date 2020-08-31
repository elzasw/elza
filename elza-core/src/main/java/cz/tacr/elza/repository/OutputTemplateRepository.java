package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutput.OutputState;
import cz.tacr.elza.domain.ArrOutputTemplate;
import cz.tacr.elza.domain.RulTemplate;

/**
 * Respozitory pro pro výstupní šablonu.
 */
public interface OutputTemplateRepository extends ElzaJpaRepository<ArrOutputTemplate, Integer> {

    @Query("SELECT ot FROM arr_output_template ot JOIN FETCH ot.template t WHERE ot.output=?1")
    List<ArrOutputTemplate> findAllByOutputFetchTemplate(ArrOutput output);

    @Query("SELECT ot FROM arr_output_template ot JOIN FETCH ot.output o WHERE ot.template IN ?1 AND o.state IN ?2 AND o.deleteChange IS NULL")
    List<ArrOutputTemplate> findNonDeletedByTemplatesAndStates(List<RulTemplate> rulTemplateToDelete,
                                                               List<OutputState> states);

    @Modifying
    @Query("UPDATE arr_output_template o SET o.template = :value WHERE o.template = :key")
    void updateTemplateByTemplate(@Param(value = "key") RulTemplate key, @Param(value = "value") RulTemplate value);

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
