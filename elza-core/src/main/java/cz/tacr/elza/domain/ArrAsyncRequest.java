package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.Validate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;

import jakarta.persistence.*;
import jakarta.persistence.criteria.Fetch;

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
    @Column(name="type", length = StringLength.LENGTH_10)
    private AsyncTypeEnum type;

    @Basic
    @Column(name="priority")
    private int priority;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = ArrFundVersion.class)
    @JoinColumn(name="fund_version_id", nullable = true)
    private ArrFundVersion fundVersion;

    @Column(name = "fund_version_id", updatable = false, insertable = false)
    private Integer fundVersionId;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name="node_id", nullable = true)
    private ArrNode node;

    @Column(name = "node_id", updatable = false, insertable = false)
    private Integer nodeId;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = ArrOutput.class)
    @JoinColumn(name="output_id", nullable = true)
    private ArrOutput output;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = ArrBulkActionRun.class)
    @JoinColumn(name="bulk_action_id", nullable = true)
    private ArrBulkActionRun bulkAction;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = ArrStructuredObject.class)
    @JoinColumn(name="structured_object_id", nullable = true)
    private ArrStructuredObject structuredObject;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = "access_point_id", nullable = true)
    private ApAccessPoint accessPoint;

    @Column(name="user_id")
    private Integer userId;

    protected ArrAsyncRequest() {
    }

    public static ArrAsyncRequest create(final ArrFundVersion fundVersion,
                                         final ArrBulkActionRun bulkActionRun,
                                         final Integer priority) {
        Validate.notNull(fundVersion);
        Validate.notNull(bulkActionRun);
        Validate.notNull(priority);
        return new ArrAsyncRequest(fundVersion, bulkActionRun, priority);
    }

    public static ArrAsyncRequest create(final ArrFundVersion fundVersion,
                                         final ArrNode node,
                                         final Integer priority) {
        Validate.notNull(fundVersion);
        Validate.notNull(node);
        Validate.notNull(priority);
        return new ArrAsyncRequest(fundVersion, node, priority);
    }

    public static ArrAsyncRequest create(final ArrFundVersion fundVersion,
                                         final ArrOutput output,
                                         final Integer priority,
                                         final Integer userId) {
        Validate.notNull(fundVersion);
        Validate.notNull(output);
        Validate.notNull(priority);
        return new ArrAsyncRequest(fundVersion, output, priority, userId);
    }

    public static ArrAsyncRequest create(final ApAccessPoint accessPoint,
                                       final Integer priority) {
        Validate.notNull(accessPoint);
        Validate.notNull(priority);
        return new ArrAsyncRequest(accessPoint, priority);
    }

    protected ArrAsyncRequest(final ArrFundVersion fundVersion,
                              final ArrBulkActionRun bulkAction,
                              final Integer priority) {
        this.type = AsyncTypeEnum.BULK;
        this.priority = priority;
        this.fundVersion = fundVersion;
        this.fundVersionId = (fundVersion != null) ? fundVersion.getFundVersionId() : null;
        this.bulkAction = bulkAction;
    }

    protected ArrAsyncRequest(final ArrFundVersion fundVersion,
                              final ArrNode node,
                              final Integer priority) {
        this.type = AsyncTypeEnum.NODE;
        this.priority = priority;
        this.fundVersion = fundVersion;
        this.fundVersionId = (fundVersion != null) ? fundVersion.getFundVersionId() : null;
        this.node = node;
        this.nodeId = (node != null) ? node.getNodeId() : null;
    }

    protected ArrAsyncRequest(final ArrFundVersion fundVersion,
                              final ArrOutput output,
                              final Integer priority,
                              final Integer userId) {
        this.type = AsyncTypeEnum.OUTPUT;
        this.priority = priority;
        this.fundVersion = fundVersion;
        this.fundVersionId = (fundVersion != null) ? fundVersion.getFundVersionId() : null;
        this.output = output;
        this.userId = userId;
    }

    protected ArrAsyncRequest(final ApAccessPoint accessPoint,
                              final Integer priority) {
        this.type = AsyncTypeEnum.AP;
        this.priority = priority;
        this.accessPoint = accessPoint;
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

    public Integer getFundVersionId() {
        return fundVersionId;
    }

    public void setFundVersion(ArrFundVersion fundVersion) {
        this.fundVersion = fundVersion;
        this.fundVersionId = (fundVersion != null) ? fundVersion.getFundVersionId() : null;
    }

    public void setFundVersionId(Integer fundVersionId) {
        this.fundVersionId = fundVersionId;
    }

    public ArrNode getNode() {
        return node;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNode(ArrNode node) {
        this.node = node;
        this.nodeId = (node != null) ? node.getNodeId() : null;
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

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

    public ApAccessPoint getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(ApAccessPoint accessPoint) {
        this.accessPoint = accessPoint;
    }
}
