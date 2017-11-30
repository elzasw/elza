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

    public Integer getPosition() {
        return position;
    }

    public int getDepth() {
        return depth;
    }

    void addChild(NodeId child) {
        int pos = child.getPosition(); // position greater than 0 otherwise IndexOutOfBoundsException is thrown
        int size = children.size();
        if (pos > size) {
            children.ensureCapacity(pos);
            int fillCount = pos - size - 1; // minus one for child position
            for (int i = 0; i < fillCount; i++) {
                children.add(null); // fill gap with null
            }
            children.add(child);
        } else {
            NodeId prev = children.set(pos - 1, child); // set child at position
            Validate.isTrue(prev == null); // previous value must be null
        }
    }
}
