package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrExport;

@Repository
public interface ExportRepository extends ElzaJpaRepository<ArrExport, Integer> {

    boolean existsByExportTypeExportTypeId(Integer exportTypeId);

    /**
     * Page of exports for a fund (newest first), optionally filtered by
     * publication type. {@code typeId} may be {@code null} to disable the
     * filter.
     */
    @Query("SELECT e FROM arr_export e"
            + " JOIN FETCH e.exportType t"
            + " JOIN e.fundVersion v"
            + " JOIN v.fund f"
            + " WHERE f.fundId = :fundId"
            + " AND (:typeId IS NULL OR t.exportTypeId = :typeId)"
            + " ORDER BY e.exportId DESC")
    List<ArrExport> findFundExports(@Param("fundId") Integer fundId,
                                    @Param("typeId") Integer typeId,
                                    Pageable pageable);

    @Query("SELECT COUNT(e) FROM arr_export e"
            + " JOIN e.fundVersion v"
            + " JOIN v.fund f"
            + " WHERE f.fundId = :fundId"
            + " AND (:typeId IS NULL OR e.exportType.exportTypeId = :typeId)")
    long countFundExports(@Param("fundId") Integer fundId,
                          @Param("typeId") Integer typeId);

    /**
     * Detects an outstanding NEW / PREPARED export of the same fund-version
     * and publication type. Used to reject duplicate {@code create} requests.
     */
    @Query("SELECT COUNT(e) FROM arr_export e"
            + " WHERE e.fundVersion.fundVersionId = :fundVersionId"
            + " AND e.exportType.exportTypeId = :typeId"
            + " AND e.state IN (cz.tacr.elza.domain.ArrExport$State.NEW,"
            + "                  cz.tacr.elza.domain.ArrExport$State.PREPARED)")
    long countOutstanding(@Param("fundVersionId") Integer fundVersionId,
                          @Param("typeId") Integer typeId);
}