package cz.tacr.elza.core.fund;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.repository.LevelRepository;

public class FundTree {

    private final Map<Integer, TreeNode> nodeIdLookup = new HashMap<>();

    private final ArrFundVersion fundVersion;

    private TreeNodeImpl root;

    FundTree(ArrFundVersion fundVersion) {
        this.fundVersion = Validate.notNull(fundVersion);
    }

    public TreeNode getRoot() {
        return root;
    }

    public TreeNode getNode(Integer nodeId) {
        Validate.notNull(nodeId);
        return nodeIdLookup.get(nodeId);
    }

    void init(LevelRepository levelRepository) {
        Validate.isTrue(root == null);

        root = new TreeNodeImpl(fundVersion.getRootNodeId());
        root.setPosition(1);

        TreeNodeLoader loader = new TreeNodeLoader(root) {
            @Override
            protected void onTreeNodeLoaded(TreeNodeImpl treeNode) {
                super.onTreeNodeLoaded(treeNode);
                nodeIdLookup.put(treeNode.getNodeId(), treeNode);
            }
        };
        levelRepository.readLevelTree(fundVersion.getRootNodeId(), fundVersion.getLockChange(), true, loader);
        loader.processed();
    }
}
