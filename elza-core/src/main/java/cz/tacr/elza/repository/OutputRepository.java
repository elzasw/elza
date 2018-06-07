package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputDefinition.OutputState;

/**
 * Respozitory pro verzi pojmenovaného výstupu z archivního výstupu.
 * 
 * @since 01.04.2016
 */
@Repository
public interface OutputRepository extends ElzaJpaRepository<ArrOutput, Integer> {

    @Query("SELECT o FROM arr_output o JOIN o.outputDefinition no JOIN no.fund f JOIN f.versions v WHERE v = :fundVersion AND no.deleted = false ORDER BY no.temporary ASC, no.name ASC")
    List<ArrOutput> findByFundVersion(@Param("fundVersion") ArrFundVersion fundVersion);

    @Query("SELECT o FROM arr_output o JOIN o.outputDefinition od JOIN od.fund f JOIN f.versions v LEFT JOIN od.outputResult res WHERE v = :fundVersion AND od.deleted = false ORDER BY res.change.changeId desc, od.temporary ASC, od.name ASC")
    List<ArrOutput> findByFundVersionSorted(@Param("fundVersion") ArrFundVersion fundVersion);

    @Query("SELECT o FROM arr_output o JOIN o.outputDefinition od JOIN od.fund f JOIN f.versions v LEFT JOIN od.outputResult res WHERE v = :fundVersion AND od.deleted = false AND od.state = :state ORDER BY res.change.changeId desc, od.temporary ASC, od.name ASC")
    List<ArrOutput> findByFundVersionAndStateSorted(@Param("fundVersion") ArrFundVersion fundVersion, @Param("state") OutputState state);

    ArrOutput findOneByOutputDefinitionAndLockChangeIsNull(ArrOutputDefinition outputDefinition);

    /**
     * Delete output by fund
     * 
     * Used by method deleteFund
     * 
     * @param fund
     */
    void deleteByOutputDefinitionFund(ArrFund fund);
}
