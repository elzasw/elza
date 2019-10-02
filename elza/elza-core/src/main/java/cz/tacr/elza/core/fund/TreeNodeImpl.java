package cz.tacr.elza.core.fund;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;

class TreeNodeImpl implements TreeNode {

    private final int nodeId;

    private TreeNodeImpl parent;

    private ArrayList<TreeNode> children;

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
        if (children == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(children);
    }

    public void onInitialized() {
        if (children != null) {
            children.trimToSize();
        }
    }

    /**
     * @param child Expecting higher position than current children.
     */
    public void addChild(TreeNode child) {
        if (children == null) {
            children = new ArrayList<>();
        } else {
            // check last child position
            TreeNode lastChild = children.get(children.size() - 1);
            Validate.isTrue(lastChild.getPosition() < child.getPosition());
        }
        children.add(child);
    }
}
