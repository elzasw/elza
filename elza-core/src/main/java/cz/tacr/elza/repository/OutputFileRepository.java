package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputFile;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


/**
 * OutputFile repository
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.2016
 */
@Repository
public interface OutputFileRepository extends ElzaJpaRepository<ArrOutputFile, Integer>, OutputFileRepositoryCustom {

    @Modifying
    @Query("DELETE FROM arr_output_file f WHERE f.outputResult IN (SELECT r FROM arr_output_result r WHERE r.outputDefinition = :outputDefinition)")
    void deleteByOutputDefinition(@Param("outputDefinition") ArrOutputDefinition outputDefinition);
}
