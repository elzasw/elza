package cz.tacr.elza.controller.vo;

import javax.annotation.Nullable;

/**
 * JP s připomínkou.
 */
public class IssueNodeItem {

    // --- fields ---

    /**
     * Počet JP s připomínkou v seznamu.
     */
    private final int nodeCount;

    /**
     * Index JP s připomínkou v seznamu.
     */
    private final Integer nodeIndex;

    /**
     * JP s připomínkou.
     */
    private final NodeItemWithParent node;

    // --- getters/setters ---

    /**
     * @return Počet JP s připomínkou v seznamu (0 - pokud neexistuje žádná otevřená připomínka)
     */
    public int getNodeCount() {
        return nodeCount;
    }

    /**
     * @return index JP s připomínkou v seznamu (null - pokud neexistuje žádná otevřená připomínka)
     */
    @Nullable
    public Integer getNodeIndex() {
        return nodeIndex;
    }

    /**
     * @return JP s připomínkou (null - pokud neexistuje žádná otevřená připomínka)
     */
    @Nullable
    public NodeItemWithParent getNode() {
        return node;
    }

    // --- constructor ---

    public IssueNodeItem(int nodeCount) {
        this.nodeCount = nodeCount;
        this.nodeIndex = null;
        this.node = null;
    }

    public IssueNodeItem(int nodeCount, int nodeIndex, NodeItemWithParent node) {
        this.nodeCount = nodeCount;
        this.nodeIndex = nodeIndex;
        this.node = node;
    }
}
