package cz.tacr.elza.print;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.Validate;

/**
 * Basic node implementation for output tree with minimal memory footprint.
 */
public class NodeId {

    private final int arrNodeId;

    private final NodeId parentNodeId;

    private final int position;

    private final int depth;

    private List<NodeId> children;

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

    public int getPosition() {
        return position;
    }

    public int getDepth() {
        return depth;
    }

    public List<NodeId> getChildren() {
        if (children == null) {
            return Collections.emptyList();
        }
        return children;
    }

    /**
     * Creates DFS tree iterator from this node.
     */
    public Iterator<NodeId> getIteratorDFS() {
        return new NodeIdIterator(this);
    }

    /**
     * @param child Expecting higher position than current children.
     */
    void addChild(NodeId child) {
        if (children == null) {
            children = new ArrayList<>();
        } else {
            // check last child position
            NodeId lastChild = children.get(children.size() - 1);
            Validate.isTrue(lastChild.getPosition() < child.getPosition());
        }
        children.add(child);
    }
}
