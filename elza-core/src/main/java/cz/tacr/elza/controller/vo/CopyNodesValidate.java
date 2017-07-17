package cz.tacr.elza.controller.vo;

import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;

import java.util.Collection;

/**
 * @since 17.7.2017
 */
public class CopyNodesValidate {

    /**
     * Id verze stromu.
     */
    private Integer targetFundVersionId;

    /**
     * Statický uzel (pod který přidáváme).
     */
    private ArrNodeVO targetStaticNode;

    /**
     * Rodič statického uzlu (pod který přidáváme)
     */
    private ArrNodeVO targetStaticNodeParent;

    /**
     * Id verze stromu.
     */
    private Integer sourceFundVersionId;

    /**
     * Seznam JP, od kterých kopírujeme podstromy.
     */
    private Collection<ArrNodeVO> sourceNodes;

    /**
     * Ignoruje vybrané JP a kopíruje pouze jejich potomky.
     */
    private boolean ignoreRootNodes;

    public Integer getTargetFundVersionId() {
        return targetFundVersionId;
    }

    public void setTargetFundVersionId(final Integer targetFundVersionId) {
        this.targetFundVersionId = targetFundVersionId;
    }

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

    public Integer getSourceFundVersionId() {
        return sourceFundVersionId;
    }

    public void setSourceFundVersionId(final Integer sourceFundVersionId) {
        this.sourceFundVersionId = sourceFundVersionId;
    }

    public Collection<ArrNodeVO> getSourceNodes() {
        return sourceNodes;
    }

    public void setSourceNodes(final Collection<ArrNodeVO> sourceNodes) {
        this.sourceNodes = sourceNodes;
    }

    public boolean isIgnoreRootNodes() {
        return ignoreRootNodes;
    }

    public void setIgnoreRootNodes(final boolean ignoreRootNodes) {
        this.ignoreRootNodes = ignoreRootNodes;
    }
}
