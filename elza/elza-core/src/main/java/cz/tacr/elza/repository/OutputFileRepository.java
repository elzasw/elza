package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputFile;

/**
 * OutputFile repository
 */
@Repository
public interface OutputFileRepository extends ElzaJpaRepository<ArrOutputFile, Integer> {

    /*
     * @Modifying
     * @Query("DELETE FROM arr_output_file f WHERE f.outputResult IN (SELECT r FROM arr_output_result r WHERE r.output = :output)")
     * void deleteByOutput(@Param("output") ArrOutput output);
     */

    void deleteByOutputResultOutputFund(ArrFund fund);

	List<ArrOutputFile> findByOutputResultOutput(ArrOutput output);

    @Modifying
    @Query("DELETE FROM arr_output_file WHERE outputResult IN (SELECT ot FROM arr_output_result ot "
            +
            "JOIN ot.output o " +
            "WHERE o.deleteChange IS NOT NULL AND o.fund = :fund)")
    void deleteByFundAndDeleteChangeIsNotNull(@Param("fund") ArrFund fund);
}
