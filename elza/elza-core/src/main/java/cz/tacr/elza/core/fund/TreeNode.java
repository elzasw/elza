package cz.tacr.elza.core.fund;

import java.util.List;

public interface TreeNode {

    boolean isRoot();

    int getNodeId();

    int getPosition();

    TreeNode getParent();

    List<TreeNode> getChildren();
}
