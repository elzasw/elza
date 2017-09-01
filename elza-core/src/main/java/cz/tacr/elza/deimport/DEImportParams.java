package cz.tacr.elza.deimport;

import java.util.LinkedList;
import java.util.List;

import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.deimport.context.ImportPhaseChangeListener;

public class DEImportParams {

    private final List<ImportPhaseChangeListener> importPhaseChangeListeners = new LinkedList<>();

    private final int scopeId;

    private final int batchSize;

    private final long memoryScoreLimit;

    private final ImportPositionParams positionParams;

    public DEImportParams(int scopeId,
                          int batchSize,
                          long memoryScoreLimit,
                          ImportPositionParams positionParams) {
        this.scopeId = scopeId;
        this.batchSize = batchSize;
        this.memoryScoreLimit = memoryScoreLimit;
        this.positionParams = positionParams;
    }

    public int getScopeId() {
        return scopeId;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public long getMemoryScoreLimit() {
        return memoryScoreLimit;
    }

    public ImportPositionParams getPositionParams() {
        return positionParams;
    }

    public List<ImportPhaseChangeListener> getImportPhaseChangeListeners() {
        return importPhaseChangeListeners;
    }

    public void addImportPhaseChangeListeners(ImportPhaseChangeListener phaseChangeListener) {
        importPhaseChangeListeners.add(phaseChangeListener);
    }

    public enum ImportDirection {
        BEFORE, AFTER
    }

    public static class ImportPositionParams {

        private int fundVersionId;

        private ArrNodeVO parentNode;

        private ArrNodeVO targetNode;

        private ImportDirection direction;

        public int getFundVersionId() {
            return fundVersionId;
        }

        public void setFundVersionId(int fundVersionId) {
            this.fundVersionId = fundVersionId;
        }

        public ArrNodeVO getParentNode() {
            return parentNode;
        }

        public void setParentNode(ArrNodeVO parentNode) {
            this.parentNode = parentNode;
        }

        public ArrNodeVO getTargetNode() {
            return targetNode;
        }

        public void setTargetNode(ArrNodeVO targetNode) {
            this.targetNode = targetNode;
        }

        public ImportDirection getDirection() {
            return direction;
        }

        public void setDirection(ImportDirection direction) {
            this.direction = direction;
        }
    }
}
