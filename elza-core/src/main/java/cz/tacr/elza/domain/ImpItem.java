package cz.tacr.elza.domain;

import java.time.OffsetDateTime;

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
 * One step of an import batch, normally one input file.
 */
@Entity(name = "imp_item")
public class ImpItem {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer itemId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ImpBatch.class)
    @JoinColumn(name = "batch_id", nullable = false)
    private ImpBatch batch;

    // The DMS row is nullable so the source file can be dropped when the batch is finished and
    // the retention window closes; the item is kept for its outcome.
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DmsFile.class)
    @JoinColumn(name = "dms_id")
    private DmsFile dmsFile;

    @Column(length = StringLength.LENGTH_250)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private ItemState state;

    @Column(nullable = false)
    private int execOrder;

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

    @Column
    private OffsetDateTime finishedAt;

    @Column
    private String error;

    @Column
    private String errorDetail;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fund_id")
    private ArrFund fund;

    @Column(nullable = false)
    private int nodesCreated;

    @Column(nullable = false)
    private int nodesUpdated;

    @Column(nullable = false)
    private int apsCreated;

    @Column(nullable = false)
    private int apsPaired;

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }

    public ImpBatch getBatch() { return batch; }
    public void setBatch(ImpBatch batch) { this.batch = batch; }

    public DmsFile getDmsFile() { return dmsFile; }
    public void setDmsFile(DmsFile dmsFile) { this.dmsFile = dmsFile; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public ItemState getState() { return state; }
    public void setState(ItemState state) { this.state = state; }

    public int getExecOrder() { return execOrder; }
    public void setExecOrder(int execOrder) { this.execOrder = execOrder; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public UsrUser getCreatedByUser() { return createdByUser; }
    public void setCreatedByUser(UsrUser createdByUser) { this.createdByUser = createdByUser; }

    public OffsetDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(OffsetDateTime executedAt) { this.executedAt = executedAt; }

    public UsrUser getExecutedByUser() { return executedByUser; }
    public void setExecutedByUser(UsrUser executedByUser) { this.executedByUser = executedByUser; }

    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(OffsetDateTime finishedAt) { this.finishedAt = finishedAt; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getErrorDetail() { return errorDetail; }
    public void setErrorDetail(String errorDetail) { this.errorDetail = errorDetail; }

    public ArrFund getFund() { return fund; }
    public void setFund(ArrFund fund) { this.fund = fund; }

    public int getNodesCreated() { return nodesCreated; }
    public void setNodesCreated(int nodesCreated) { this.nodesCreated = nodesCreated; }

    public int getNodesUpdated() { return nodesUpdated; }
    public void setNodesUpdated(int nodesUpdated) { this.nodesUpdated = nodesUpdated; }

    public int getApsCreated() { return apsCreated; }
    public void setApsCreated(int apsCreated) { this.apsCreated = apsCreated; }

    public int getApsPaired() { return apsPaired; }
    public void setApsPaired(int apsPaired) { this.apsPaired = apsPaired; }
}
