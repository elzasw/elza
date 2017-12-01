package cz.tacr.elza.print;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

/**
 * Basic node implementation for output tree with minimal memory footprint.
 */
public class NodeId {

    private final int arrNodeId;

    private final NodeId parentNodeId;

    private final ArrayList<NodeId> children = new ArrayList<>();

    private final int position;

    private final int depth;

    /**
     * Creates internal or leaf node.
     */
    NodeId(int arrNodeId, NodeId parentNodeId, int position) {
        this.arrNodeId = arrNodeId;
        this.parentNodeId = parentNodeId;
        this.position = position;
        this.depth = parentNodeId.getDepth() + 1;
    }

    /**
     * Creates root node.
     */
    NodeId(int arrNodeId, int position) {
        this.arrNodeId = arrNodeId;
        this.parentNodeId = null;
        this.position = position;
        this.depth = 1;
    }

    public int getArrNodeId() {
        return arrNodeId;
    }

    /**
     * @return Returns NodeId for parent node or null if current is root.
     */
    public NodeId getParent() {
        return parentNodeId;
    }

    public List<NodeId> getChildren() {
        return children;
    }

    public int getPosition() {
        return position;
    }

    public int getDepth() {
        return depth;
    }

    /**
     * @param child Expecting higher position than current children.
     */
    void addChild(NodeId child) {
        NodeId lastChild = children.get(children.size() - 1);
        Validate.isTrue(lastChild.getPosition() < child.getPosition());
        children.add(child);
    }
}
