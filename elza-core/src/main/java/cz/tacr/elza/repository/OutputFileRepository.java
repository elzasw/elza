package cz.tacr.elza.repository;

import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrOutputFile;

/**
 * OutputFile repository
 */
@Repository
public interface OutputFileRepository extends ElzaJpaRepository<ArrOutputFile, Integer>, OutputFileRepositoryCustom {

    /*
     * @Modifying
     * @Query("DELETE FROM arr_output_file f WHERE f.outputResult IN (SELECT r FROM arr_output_result r WHERE r.output = :output)")
     * void deleteByOutput(@Param("output") ArrOutput output);
     */

    void deleteByOutputResultOutputFund(ArrFund fund);
}
