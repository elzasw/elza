package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.domain.enumeration.StringLength;

import javax.persistence.*;
import javax.persistence.criteria.Fetch;

@Entity(name="arr_async_request")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrAsyncRequest {

    @Id
    @GeneratedValue
    @Column(name= "async_request_id")
    @Access(AccessType.PROPERTY)
    private Long asyncRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name="type", length = StringLength.LENGTH_ENUM)
    private AsyncTypeEnum type;

    @Basic
    @Column(name="priority")
    private int priority;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = ArrFundVersion.class)
    @JoinColumn(name="fund_version_id", nullable = false)
    private ArrFundVersion fundVersion;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name="node_id", nullable = true)
    private ArrNode node;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = ArrOutput.class)
    @JoinColumn(name="output_id", nullable = true)
    private ArrOutput output;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = ArrBulkActionRun.class)
    @JoinColumn(name="bulk_action_id", nullable = true)
    private ArrBulkActionRun bulkAction;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = ArrStructuredObject.class)
    @JoinColumn(name="structured_object_id", nullable = true)
    private ArrStructuredObject structuredObject;

    public ArrAsyncRequest() {
    }

    public ArrAsyncRequest(AsyncTypeEnum type, int priority, ArrFundVersion fundVersion) {
        this.type = type;
        this.priority = priority;
        this.fundVersion = fundVersion;
    }

    public Long getAsyncRequestId() {
        return asyncRequestId;
    }

    public void setAsyncRequestId(Long asyncRequestId) {
        this.asyncRequestId = asyncRequestId;
    }

    public AsyncTypeEnum getType() {
        return type;
    }

    public void setType(AsyncTypeEnum type) {
        this.type = type;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public ArrFundVersion getFundVersion() {
        return fundVersion;
    }

    public void setFundVersion(ArrFundVersion fundVersion) {
        this.fundVersion = fundVersion;
    }

    public ArrNode getNode() {
        return node;
    }

    public void setNode(ArrNode node) {
        this.node = node;
    }

    public ArrOutput getOutput() {
        return output;
    }

    public void setOutput(ArrOutput output) {
        this.output = output;
    }

    public ArrBulkActionRun getBulkAction() {
        return bulkAction;
    }

    public void setBulkAction(ArrBulkActionRun bulkAction) {
        this.bulkAction = bulkAction;
    }

    public ArrStructuredObject getStructuredObject() {
        return structuredObject;
    }

    public void setStructuredObject(ArrStructuredObject structuredObject) {
        this.structuredObject = structuredObject;
    }
}
