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

    /**
     * @param child Expecting higher position than current children.
     */
    public void addChild(TreeNode child) {
        TreeNode lastChild = children.get(children.size() - 1);
        Validate.isTrue(lastChild.getPosition() < child.getPosition());
        children.add(child);
    }
}
