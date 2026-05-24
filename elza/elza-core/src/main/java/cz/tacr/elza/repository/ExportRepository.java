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
    List<ArrExport> findFundExports(@Param("fundId") Integer fundId, @Param("typeId") Integer typeId, Pageable pageable);

    /**
     * Cursor-paged listing for the public publication API.
     *
     * Returns exports of a given target system with a downloadable file, in
     * states observable from the outside (PREPARED → FETCHED → PUBLISHED, plus
     * the recoverable PUBLISH_ERROR), ordered by {@code exportSeq} ascending.
     * NEW / PREPARE_ERROR / INVALIDATED are internal-only and filtered out.
     */
    @Query("SELECT e FROM arr_export e"
            + " JOIN FETCH e.exportType t"
            + " JOIN FETCH e.fundVersion v"
            + " JOIN FETCH v.fund f"
            + " WHERE t.code = :targetSystem"
            + " AND e.file IS NOT NULL"
            + " AND e.exportSeq IS NOT NULL"
            + " AND e.state IN (cz.tacr.elza.domain.ArrExport$State.PREPARED,"
            + "                 cz.tacr.elza.domain.ArrExport$State.FETCHED,"
            + "                 cz.tacr.elza.domain.ArrExport$State.PUBLISHED,"
            + "                 cz.tacr.elza.domain.ArrExport$State.PUBLISH_ERROR)"
            + " AND (:lastSeq IS NULL OR e.exportSeq > :lastSeq)"
            + " ORDER BY e.exportSeq ASC")
    List<ArrExport> findAvailable(@Param("targetSystem") String targetSystem, @Param("lastSeq") Long lastSeq, Pageable pageable);

    @Query("SELECT COUNT(e) FROM arr_export e"
            + " JOIN e.fundVersion v"
            + " JOIN v.fund f"
            + " WHERE f.fundId = :fundId"
            + " AND (:typeId IS NULL OR e.exportType.exportTypeId = :typeId)")
    long countFundExports(@Param("fundId") Integer fundId, @Param("typeId") Integer typeId);

    /**
     * Detects an outstanding NEW export of the same fund version and
     * publication type. Used to reject duplicate {@code create} requests
     * while the previous one is still queued for asynchronous preparation.
     *
     * PREPARED is NOT considered outstanding: the fund may have changed
     * since the previous file was generated, so re-publishing produces
     * different content. Blocking it would be over-restrictive.
     */
    @Query("SELECT COUNT(e) FROM arr_export e"
            + " WHERE e.fundVersion.fundVersionId = :fundVersionId"
            + " AND e.exportType.exportTypeId = :typeId"
            + " AND e.state = cz.tacr.elza.domain.ArrExport$State.NEW")
    long countOutstanding(@Param("fundVersionId") Integer fundVersionId, @Param("typeId") Integer typeId);

    /**
     * Allocate the next monotonic value from {@code arr_export_seq}. Called
     * when an export transitions to PREPARED so the public publication API
     * can use the value as its opaque cursor (lastTransaction).
     *
     * Works on both PostgreSQL and H2 (tests run H2 in PostgreSQL mode).
     */
    @Query(value = "SELECT nextval('arr_export_seq')", nativeQuery = true)
    long nextExportSeq();

    /**
     * Count exports referencing a given {@code dms_file} other than the
     * specified one. Used to decide whether the file can be physically
     * removed when its "owning" export is invalidated or swept by retention
     * — {@link cz.tacr.elza.service.PublicationService#copy(Integer, Integer, Integer)}
     * may have linked sibling exports to the same row.
     */
    @Query("SELECT COUNT(e) FROM arr_export e"
            + " WHERE e.fileId = :fileId"
            + " AND e.exportId <> :excludeExportId")
    long countOtherReferencingFile(@Param("fileId") Integer fileId,
                                   @Param("excludeExportId") Integer excludeExportId);

    /**
     * Exports of a given fund + publication type that currently have a
     * downloadable file, ordered newest first by {@code exportSeq}. Used by
     * the retention sweep: skip the first {@code retentionCount} rows, the
     * rest are candidates to lose their files.
     *
     * Retention is per fund + type — each fund's publication history is
     * independent of other funds' publications of the same type.
     */
    @Query("SELECT e FROM arr_export e"
            + " JOIN e.fundVersion v"
            + " JOIN v.fund f"
            + " WHERE f.fundId = :fundId"
            + " AND e.exportType.exportTypeId = :typeId"
            + " AND e.fileId IS NOT NULL"
            + " ORDER BY e.exportSeq DESC")
    List<ArrExport> findRetentionExportsForFundAndType(@Param("fundId") Integer fundId,
                                                      @Param("typeId") Integer typeId);
}