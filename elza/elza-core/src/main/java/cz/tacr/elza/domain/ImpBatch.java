package cz.tacr.elza.domain;

import java.time.OffsetDateTime;

import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * User-configured plan for a data import. The concrete parameters live in the imp_batch_&lt;kind&gt;
 * subtype tables; this row holds what every kind shares.
 */
@Entity(name = "imp_batch")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "import_type", discriminatorType = DiscriminatorType.STRING)
public abstract class ImpBatch {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer batchId;

    // Managed by the JPA discriminator; exposed so it can be used in queries and DTOs without
    // having to inspect the concrete class.
    @Enumerated(EnumType.STRING)
    @Column(name = "import_type", length = StringLength.LENGTH_ENUM,
            nullable = false, insertable = false, updatable = false)
    private ImportType importType;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "created_by_user_id")
    private UsrUser createdByUser;

    @Column
    private OffsetDateTime executedAt;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "executed_by_user_id")
    private UsrUser executedByUser;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private BatchState state;

    @Column(nullable = false)
    private OffsetDateTime lastStateChangeAt;

    @Column(nullable = false)
    private boolean skipError;

    @Column(nullable = false)
    private boolean keepFiles;

    @Column
    private OffsetDateTime filesDeletedAt;

    public Integer getBatchId() { return batchId; }
    public void setBatchId(Integer batchId) { this.batchId = batchId; }

    public ImportType getImportType() { return importType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public UsrUser getCreatedByUser() { return createdByUser; }
    public void setCreatedByUser(UsrUser createdByUser) { this.createdByUser = createdByUser; }

    public OffsetDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(OffsetDateTime executedAt) { this.executedAt = executedAt; }

    public UsrUser getExecutedByUser() { return executedByUser; }
    public void setExecutedByUser(UsrUser executedByUser) { this.executedByUser = executedByUser; }

    public BatchState getState() { return state; }
    public void setState(BatchState state) { this.state = state; }

    public OffsetDateTime getLastStateChangeAt() { return lastStateChangeAt; }
    public void setLastStateChangeAt(OffsetDateTime lastStateChangeAt) { this.lastStateChangeAt = lastStateChangeAt; }

    public boolean isSkipError() { return skipError; }
    public void setSkipError(boolean skipError) { this.skipError = skipError; }

    public boolean isKeepFiles() { return keepFiles; }
    public void setKeepFiles(boolean keepFiles) { this.keepFiles = keepFiles; }

    public OffsetDateTime getFilesDeletedAt() { return filesDeletedAt; }
    public void setFilesDeletedAt(OffsetDateTime filesDeletedAt) { this.filesDeletedAt = filesDeletedAt; }
}
