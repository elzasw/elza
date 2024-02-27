package cz.tacr.elza.print;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.lang3.Validate;

/**
 * Implementace iterátoru s cache pro načítání bloku dat.
 */
public class NodeIterator implements Iterator<Node>, NodeProvider {

    private final NodeLoader nodeLoader;

    // source node ids
    private final Iterator<NodeId> nodeIdIterator;

    private final int windowSize;

    // loaded nodes for current window
    private List<Node> nodes;

    /**
     * Map of active Nodes
     * 
     * Map contain nodes with their parents to the root
     */
    private Map<NodeId, Node> activeNodesMap = new HashMap<>();

    // position in current window
    private int windowIndex;

    public NodeIterator(NodeLoader nodeLoader, Iterator<NodeId> nodeIdIterator, int windowSize) {
        this.nodeLoader = nodeLoader;
        this.nodeIdIterator = nodeIdIterator;
        this.windowSize = windowSize;
        this.windowIndex = windowSize; // force to load first window
    }

    /**
     * Creates node iterator with default window size of 1000.
     */
    public NodeIterator(NodeLoader nodeLoader, Iterator<NodeId> nodeIdIterator) {
        this(nodeLoader, nodeIdIterator, 1000);
    }

    @Override
    public boolean hasNext() {
        if (nodes != null) {
            if (windowIndex < nodes.size()) {
                return true;
            }
            nodes = null; // release node references for last window
        } else {
            if (nodeIdIterator.hasNext()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Node next() {

    	if (!hasNext()) {
            throw new NoSuchElementException();
        }

        if (windowIndex >= windowSize) {
            nodes = nodeLoader.loadNodes(getNextIds());
            updateActiveNodesMap();
            windowIndex = 0;
        }

        Node node = nodes.get(windowIndex);
        Validate.notNull(node);

        windowIndex++;
        return node;
    }

    private void updateActiveNodesMap() {
        Set<NodeId> oldNodes = new HashSet<>(activeNodesMap.keySet());

        for (Node node : nodes) {
            node.setNodeProvider(this);
            // place this node
            activeNodesMap.put(node.getNodeId(), node);
            // try to locate parents
            NodeId parentNodeId = node.getParent();
            while (parentNodeId != null) {
                // if exists in oldNodes -> remove it                
                if (oldNodes.remove(parentNodeId)) {
                    // try another parent
                    parentNodeId = parentNodeId.getParent();
                } else {
                    parentNodeId = null;
                }
            }
        }

        // remove untouched old nodes  
        for (NodeId oldNode : oldNodes) {
            activeNodesMap.remove(oldNode);
        }

    }

    /**
     * Loads node ids for new window. Position of source iterator will be changed.
     */
    private List<NodeId> getNextIds() {
        List<NodeId> nodeIds = new ArrayList<>(windowSize);
        int i = 0;
        while (i < windowSize && nodeIdIterator.hasNext()) {
            nodeIds.add(nodeIdIterator.next());
            i++;
        }
        return nodeIds;
    }

    @Override
    public Node getNode(NodeId nodeId) {
        Node node = activeNodesMap.get(nodeId);
        if (node == null) {
            // node not found, try to use load
            List<Node> tmpNodes = this.nodeLoader.loadNodes(Collections.singletonList(nodeId));
            if (tmpNodes.size() > 0) {
                node = tmpNodes.get(0);
            }
        }
        return node;
    }
}
