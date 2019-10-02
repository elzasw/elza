package cz.tacr.elza.controller.vo;

import cz.tacr.elza.controller.ArrangementController;

public class SelectNodeResult {

    private ArrFundVO fund;
    private ArrangementController.NodeWithParent nodeWithParent;

    public ArrFundVO getFund() {
        return fund;
    }

    public void setFund(final ArrFundVO fund) {
        this.fund = fund;
    }

    public void setNodeWithParent(final ArrangementController.NodeWithParent nodeWithParent) {
        this.nodeWithParent = nodeWithParent;
    }

    public ArrangementController.NodeWithParent getNodeWithParent() {
        return nodeWithParent;
    }
}
