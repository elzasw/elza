package cz.tacr.elza.core.tree;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.repository.LevelRepositoryCustom.TreeLevelConsumer;

class TreeNodeLoader implements TreeLevelConsumer {

    private Map<Integer, TreeNodeImpl> parentMap;

    private Map<Integer, TreeNodeImpl> childMap;

    private TreeNodeImpl currentParent;

    private int currentDepth;

    public TreeNodeLoader(TreeNodeImpl root) {
        this.currentParent = root;
        this.childMap = new HashMap<>();
    }

    @Override
    public void accept(ArrLevel level, int depth) {
        // check if moved to next depth
        if (depth > currentDepth) {
            currentDepth++;
            Validate.isTrue(depth == currentDepth);
            parentMap = childMap;
            childMap = new HashMap<>();
        }

        // check if parent changed
        Integer parentNodeId = level.getNodeIdParent();
        if (currentParent.getNodeId() != parentNodeId.intValue()) {
            onTreeNodeLoaded(currentParent);
            currentParent = parentMap.get(parentNodeId);
            Validate.notNull(currentParent);
        }

        // create child
        Integer nodeId = level.getNodeId();
        TreeNodeImpl child = new TreeNodeImpl(nodeId.intValue());
        child.setPosition(level.getPosition());
        child.setParent(currentParent);
        currentParent.addChild(child);

        // add to node lookup
        childMap.put(nodeId, child);
    }

    public void processed() {
        if (currentParent != null) {
            onTreeNodeLoaded(currentParent);
        }
    }

    protected void onTreeNodeLoaded(TreeNodeImpl treeNode) {
        treeNode.onInitialized();
    }
}