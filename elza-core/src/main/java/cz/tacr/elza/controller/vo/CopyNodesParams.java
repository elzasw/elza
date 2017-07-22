package cz.tacr.elza.controller.vo;

import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.service.ArrMoveLevelService;

/**
 * @since 17.7.2017
 */
public class CopyNodesParams extends CopyNodesValidate {

    /**
     * Statický uzel (pod který přidáváme).
     */
    private ArrNodeVO targetStaticNode;

    /**
     * Rodič statického uzlu (pod který přidáváme)
     */
    private ArrNodeVO targetStaticNodeParent;

    /**
     * Směr založení levelu.
     */
    private ArrMoveLevelService.AddLevelDirection selectedDirection;

    private ConflictResolve filesConflictResolve;

    private ConflictResolve packetsConflictResolve;

    public ArrNodeVO getTargetStaticNode() {
        return targetStaticNode;
    }

    public void setTargetStaticNode(final ArrNodeVO targetStaticNode) {
        this.targetStaticNode = targetStaticNode;
    }

    public ArrNodeVO getTargetStaticNodeParent() {
        return targetStaticNodeParent;
    }

    public void setTargetStaticNodeParent(final ArrNodeVO targetStaticNodeParent) {
        this.targetStaticNodeParent = targetStaticNodeParent;
    }

    public ConflictResolve getFilesConflictResolve() {
        return filesConflictResolve;
    }

    public void setFilesConflictResolve(final ConflictResolve filesConflictResolve) {
        this.filesConflictResolve = filesConflictResolve;
    }

    public ConflictResolve getPacketsConflictResolve() {
        return packetsConflictResolve;
    }

    public void setPacketsConflictResolve(final ConflictResolve packetsConflictResolve) {
        this.packetsConflictResolve = packetsConflictResolve;
    }

    public ArrMoveLevelService.AddLevelDirection getSelectedDirection() {
        return selectedDirection;
    }

    public void setSelectedDirection(final ArrMoveLevelService.AddLevelDirection selectedDirection) {
        this.selectedDirection = selectedDirection;
    }
}
