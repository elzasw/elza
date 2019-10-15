package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Vazební entita mezi nodem a požadavkem na digitalizaci.
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Entity(name = "arr_digitization_request_node")
@Table
public class ArrDigitizationRequestNode {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer digitizationRequestNodeId;

	@ManyToOne(fetch=FetchType.LAZY, targetEntity = ArrDigitizationRequest.class)
    @JoinColumn(name = "digitizationRequestId", nullable = false)
    private ArrDigitizationRequest digitizationRequest;

    @Column(name = "digitizationRequestId", insertable = false, updatable = false)
    private Integer digitizationRequestId;

	@ManyToOne(fetch=FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    public Integer getDigitizationRequestNodeId() {
        return digitizationRequestNodeId;
    }

    public void setDigitizationRequestNodeId(final Integer digitizationRequestNodeId) {
        this.digitizationRequestNodeId = digitizationRequestNodeId;
    }

    public ArrDigitizationRequest getDigitizationRequest() {
        return digitizationRequest;
    }

    public void setDigitizationRequest(final ArrDigitizationRequest digitizationRequest) {
        this.digitizationRequest = digitizationRequest;
        this.digitizationRequestId = digitizationRequest == null ? null : digitizationRequest.getRequestId();
    }

    public ArrNode getNode() {
        return node;
    }

    public void setNode(final ArrNode node) {
        this.node = node;
    }

    public Integer getDigitizationRequestId() {
        return digitizationRequestId;
    }
}
