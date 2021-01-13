package cz.tacr.elza.core.fund;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.repository.LevelRepositoryCustom.TreeLevelConsumer;

class TreeNodeLoader implements TreeLevelConsumer {

    private Map<Integer, TreeNodeImpl> parentMap;

    private Map<Integer, TreeNodeImpl> childMap;

    private TreeNodeImpl currentParent;

    private int currentDepth;

    public TreeNodeLoader(TreeNodeImpl root) {
        this.parentMap = new HashMap<>();
        this.childMap = new HashMap<>();

        this.parentMap.put(root.getNodeId(), root);
        this.currentParent = root;
    }

    @Override
    public void accept(ArrLevel level, int depth) {
        // check if moved to next depth
        if (depth > currentDepth) {
            currentDepth++;
            Validate.isTrue(depth == currentDepth);
            // fire loaded event for current parents
            parentMap.values().forEach(this::onTreeNodeLoaded);
            // make new parents from children
            parentMap = childMap;
            childMap = new HashMap<>();
        }

        // check if parent changed
        Integer parentNodeId = level.getNodeIdParent();
        if (currentParent.getNodeId() != parentNodeId.intValue()) {
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
        parentMap.values().forEach(this::onTreeNodeLoaded);
        childMap.values().forEach(this::onTreeNodeLoaded);
    }

    protected void onTreeNodeLoaded(TreeNodeImpl treeNode) {
        treeNode.onInitialized();
    }
}