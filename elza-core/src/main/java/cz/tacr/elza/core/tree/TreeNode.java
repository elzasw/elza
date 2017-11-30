package cz.tacr.elza.core.tree;

import java.util.List;

public interface TreeNode {

    boolean isRoot();

    int getNodeId();

    int getPosition();

    TreeNode getParent();

    List<TreeNode> getChildren();

    boolean traverseDF(Visitor visitor);

    interface Visitor {

        /**
         * @return If true then subtree of current note will be visited.
         */
        boolean visit(TreeNode treeNode);
    }
}
