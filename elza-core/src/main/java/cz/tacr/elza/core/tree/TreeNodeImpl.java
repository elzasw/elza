package cz.tacr.elza.core.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;

class TreeNodeImpl implements TreeNode {

    private final int nodeId;

    private final ArrayList<TreeNode> children = new ArrayList<>();

    private TreeNodeImpl parent;

    private int position;

    public TreeNodeImpl(int nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public boolean isRoot() {
        return parent == null;
    }

    @Override
    public int getNodeId() {
        return nodeId;
    }

    @Override
    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public TreeNodeImpl getParent() {
        return parent;
    }

    public void setParent(TreeNodeImpl parent) {
        this.parent = parent;
    }

    @Override
    public List<TreeNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public boolean traverseDF(TreeNode.Visitor visitor) {
        if (!visitor.visit(this)) {
            return false;
        }
        for (TreeNode child : children) {
            if (!child.traverseDF(visitor)) {
                return false;
            }
        }
        return true;
    }

    public void onInitialized() {
        children.trimToSize();
    }

    public void addChild(TreeNode child) {
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
            TreeNode prev = children.set(pos - 1, child); // set child at position
            Validate.isTrue(prev == null); // previous value must be null
        }
    }
}
