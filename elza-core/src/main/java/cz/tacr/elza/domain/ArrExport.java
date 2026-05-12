package cz.tacr.elza.domain;

import java.time.OffsetDateTime;

import org.hibernate.Length;

import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * One archival-description export record consumed by the public publication
 * API.
 *
 * Each row tracks a single export request through its life cycle and links
 * to the generated XML in {@code dms_file}. State transitions are described
 * in the "Publikace archivního popisu" specification.
 */
@Entity(name = "arr_export")
public class ArrExport {

    /**
     * Internal life-cycle state of an export record.
     *
     * Only a subset is observable from the public API; see
     * {@code main.tsp} for the public projection.
     */
    public enum State {
        /** Record created, XML preparation has not started yet. */
        NEW,
        /** XML is prepared and downloadable. */
        PREPARED,
        /** Publication system has downloaded the XML at least once. */
        FETCHED,
        /** Publication system reported the publication succeeded. */
        PUBLISHED,
        /** XML preparation failed on the Elza side (terminal). */
        PREPARE_ERROR,
        /** Publication system reported a failure during publishing. */
        PUBLISH_ERROR,
        /** Record was invalidated by a user; the XML file has been removed. */
        INVALIDATED,
    }

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer exportId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrExportType.class)
    @JoinColumn(name = "exportTypeId", nullable = false)
    private ArrExportType exportType;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer exportTypeId;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private State state;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "userId", nullable = false)
    private UsrUser user;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer userId;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column
    private OffsetDateTime preparedAt;

    @Column
    private OffsetDateTime errorAt;

    @Column
    private OffsetDateTime lastFetchedAt;

    @Column
    private OffsetDateTime publishedAt;

    @Column
    private OffsetDateTime invalidatedAt;

    /**
     * Snapshot of the filter applied to this export. Decouples already-issued
     * exports from later changes to the publication type's configuration.
     */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulExportFilter.class)
    @JoinColumn(name = "exportFilterId", nullable = true)
    private RulExportFilter exportFilter;

    @Column(nullable = true, updatable = false, insertable = false)
    private Integer exportFilterId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFundVersion.class)
    @JoinColumn(name = "fundVersionId", nullable = false)
    private ArrFundVersion fundVersion;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer fundVersionId;

    /**
     * Last fund change included in the XML snapshot. The application must
     * null this field before the referenced arr_change row is removed —
     * the DB does not cascade. A null value here means the originating
     * change has been purged; the XML in {@code file} is unaffected.
     */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "lastChangeId", nullable = true)
    private ArrChange lastChange;

    @Column(nullable = true, updatable = false, insertable = false)
    private Integer lastChangeId;

    /**
     * Generated XML payload. The application must null this field before
     * the referenced dms_file row is deleted by the retention sweep — the
     * DB does not cascade.
     */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DmsFile.class)
    @JoinColumn(name = "fileId", nullable = true)
    private DmsFile file;

    @Column(nullable = true, updatable = false, insertable = false)
    private Integer fileId;

    /** Error description for PREPARE_ERROR and PUBLISH_ERROR states. */
    @Column(length = Length.LONG32)
    private String errorMessage;

    /**
     * Opaque monotonic cursor backing the {@code lastTransaction} parameter
     * of the public publication API. Assigned from the {@code arr_export_seq}
     * sequence on transition to PREPARED.
     */
    @Column
    private Long exportSeq;

    public Integer getExportId() {
        return exportId;
    }

    public void setExportId(Integer exportId) {
        this.exportId = exportId;
    }

    public ArrExportType getExportType() {
        return exportType;
    }

    public void setExportType(ArrExportType exportType) {
        this.exportType = exportType;
        this.exportTypeId = exportType != null ? exportType.getExportTypeId() : null;
    }

    public Integer getExportTypeId() {
        return exportTypeId;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public UsrUser getUser() {
        return user;
    }

    public void setUser(UsrUser user) {
        this.user = user;
        this.userId = user != null ? user.getUserId() : null;
    }

    public Integer getUserId() {
        return userId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getPreparedAt() {
        return preparedAt;
    }

    public void setPreparedAt(OffsetDateTime preparedAt) {
        this.preparedAt = preparedAt;
    }

    public OffsetDateTime getErrorAt() {
        return errorAt;
    }

    public void setErrorAt(OffsetDateTime errorAt) {
        this.errorAt = errorAt;
    }

    public OffsetDateTime getLastFetchedAt() {
        return lastFetchedAt;
    }

    public void setLastFetchedAt(OffsetDateTime lastFetchedAt) {
        this.lastFetchedAt = lastFetchedAt;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public OffsetDateTime getInvalidatedAt() {
        return invalidatedAt;
    }

    public void setInvalidatedAt(OffsetDateTime invalidatedAt) {
        this.invalidatedAt = invalidatedAt;
    }

    public RulExportFilter getExportFilter() {
        return exportFilter;
    }

    public void setExportFilter(RulExportFilter exportFilter) {
        this.exportFilter = exportFilter;
        this.exportFilterId = exportFilter != null ? exportFilter.getExportFilterId() : null;
    }

    public Integer getExportFilterId() {
        return exportFilterId;
    }

    public ArrFundVersion getFundVersion() {
        return fundVersion;
    }

    public void setFundVersion(ArrFundVersion fundVersion) {
        this.fundVersion = fundVersion;
        this.fundVersionId = fundVersion != null ? fundVersion.getFundVersionId() : null;
    }

    public Integer getFundVersionId() {
        return fundVersionId;
    }

    public ArrChange getLastChange() {
        return lastChange;
    }

    public void setLastChange(ArrChange lastChange) {
        this.lastChange = lastChange;
        this.lastChangeId = lastChange != null ? lastChange.getChangeId() : null;
    }

    public Integer getLastChangeId() {
        return lastChangeId;
    }

    public DmsFile getFile() {
        return file;
    }

    public void setFile(DmsFile file) {
        this.file = file;
        this.fileId = file != null ? file.getFileId() : null;
    }

    public Integer getFileId() {
        return fileId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getExportSeq() {
        return exportSeq;
    }

    public void setExportSeq(Long exportSeq) {
        this.exportSeq = exportSeq;
    }
}
