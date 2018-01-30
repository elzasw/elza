package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputResult;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


/**
 * OutputResult repository
 *
 */
@Repository
public interface OutputResultRepository extends ElzaJpaRepository<ArrOutputResult, Integer> {

    ArrOutputResult findByOutputDefinition(final ArrOutputDefinition outputDefinition);

    void deleteByOutputDefinition(final ArrOutputDefinition outputDefinition);

    /**
     * Return output result with fetched outputDefinition
     * @param outputResultId
     * @return
     */
    @Query(value = "select o from arr_output_result o join fetch o.outputDefinition where o.outputResultId = :outputResultId")
	ArrOutputResult findOneByOutputResultId(@Param(value = "outputResultId") Integer outputResultId);
}
