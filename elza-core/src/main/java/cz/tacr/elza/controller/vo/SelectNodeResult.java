package cz.tacr.elza.controller.vo;

import cz.tacr.elza.controller.ArrangementController;

public class SelectNodeResult {

    private ArrFundVO fund;
    private ArrangementController.NodesWithParent nodeWithParent;

    public ArrFundVO getFund() {
        return fund;
    }

    public void setFund(final ArrFundVO fund) {
        this.fund = fund;
    }

    public void setNodeWithParent(final ArrangementController.NodesWithParent nodeWithParent) {
        this.nodeWithParent = nodeWithParent;
    }

    public ArrangementController.NodesWithParent getNodeWithParent() {
        return nodeWithParent;
    }
}
