package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrOutput;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Respozitory pro verzi pojmenovaného výstupu z archivního výstupu.
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
@Repository
public interface OutputRepository extends ElzaJpaRepository<ArrOutput, Integer> {

    @Query("SELECT o FROM arr_output o JOIN o.outputDefinition no JOIN no.fund f JOIN f.versions v WHERE v = :fundVersion AND no.deleted = false ORDER BY no.temporary ASC, no.name ASC")
    List<ArrOutput> findByFundVersion(@Param("fundVersion") ArrFundVersion fundVersion);

    @Override
    default String getClassName() {
        return ArrOutput.class.getSimpleName();
    }
}
