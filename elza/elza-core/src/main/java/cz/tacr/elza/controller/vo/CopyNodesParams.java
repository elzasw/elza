package cz.tacr.elza.controller.vo;

import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.service.FundLevelService;

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
    private FundLevelService.AddLevelDirection selectedDirection;

    private ConflictResolve filesConflictResolve;

    private ConflictResolve structuredsConflictResolve;

    private Integer templateId;

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

    public ConflictResolve getStructuredsConflictResolve() {
        return structuredsConflictResolve;
    }

    public void setStructuredsConflictResolve(final ConflictResolve structuredsConflictResolve) {
        this.structuredsConflictResolve = structuredsConflictResolve;
    }

    public FundLevelService.AddLevelDirection getSelectedDirection() {
        return selectedDirection;
    }

    public void setSelectedDirection(final FundLevelService.AddLevelDirection selectedDirection) {
        this.selectedDirection = selectedDirection;
    }

    public Integer getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Integer templateId) {
        this.templateId = templateId;
    }
}
