package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.RulTemplate;


/**
 * OutputResult repository
 *
 */
@Repository
public interface OutputResultRepository extends ElzaJpaRepository<ArrOutputResult, Integer> {

    ArrOutputResult findByOutputDefinition(final ArrOutputDefinition outputDefinition);

    // void deleteByOutputDefinition(final ArrOutputDefinition outputDefinition);

    /**
     * Return output result with fetched outputDefinition
     * @param outputResultId
     * @return
     */
    @Query(value = "select o from arr_output_result o join fetch o.outputDefinition where o.outputResultId = :outputResultId")
	ArrOutputResult findOneByOutputResultId(@Param(value = "outputResultId") Integer outputResultId);

    void deleteByOutputDefinitionFund(ArrFund fund);

    @Modifying
    @Query("UPDATE arr_output_result r SET r.template = :value WHERE r.template = :key")
    void updateTemplateByTemplate(@Param(value = "key") RulTemplate key,
                                  @Param(value = "value") RulTemplate value);
}
