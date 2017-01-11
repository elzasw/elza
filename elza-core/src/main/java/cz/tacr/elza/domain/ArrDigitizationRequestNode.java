package cz.tacr.elza.domain;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Implementace {@link cz.tacr.elza.api.ArrDigitizationRequestNode}
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Entity(name = "arr_digitization_request_node")
@Table
public class ArrDigitizationRequestNode {

    @Id
    @GeneratedValue
    private Integer digitizationRequestNodeId;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrDigitizationRequest.class)
    @JoinColumn(name = "digitizationRequestId", nullable = false)
    private ArrDigitizationRequest digitizationRequest;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrNode.class)
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
    }

    public ArrNode getNode() {
        return node;
    }

    public void setNode(final ArrNode node) {
        this.node = node;
    }
}
