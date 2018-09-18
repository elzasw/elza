package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ArrDigitizationRequest;

import java.util.List;

/**
 * Value objekt {@link ArrDigitizationRequest}
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
public class ArrDigitizationRequestVO extends ArrRequestVO {

    private String description;

    private Integer nodesCount;

    private Integer digitizationFrontdeskId;

    private List<TreeNodeVO> nodes;

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Integer getNodesCount() {
        return nodesCount;
    }

    public void setNodesCount(final Integer nodesCount) {
        this.nodesCount = nodesCount;
    }

    public List<TreeNodeVO> getNodes() {
        return nodes;
    }

    public void setNodes(final List<TreeNodeVO> nodes) {
        this.nodes = nodes;
    }

    public Integer getDigitizationFrontdeskId() {
        return digitizationFrontdeskId;
    }

    public void setDigitizationFrontdeskId(final Integer digitizationFrontdeskId) {
        this.digitizationFrontdeskId = digitizationFrontdeskId;
    }
}
