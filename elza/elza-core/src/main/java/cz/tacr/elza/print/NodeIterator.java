package cz.tacr.elza.print;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.Validate;

/**
 * Implementace iterátoru s cache pro načítání bloku dat.
 */
public class NodeIterator implements Iterator<Node> {

    private final NodeLoader nodeLoader;

    // source node ids
    private final Iterator<NodeId> nodeIdIterator;

    private final int windowSize;

    // loaded nodes for current window
    private List<Node> nodes;

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
        if (nodeIdIterator.hasNext()) {
            return true;
        }
        if (nodes != null) {
            if (windowIndex < nodes.size()) {
                return true;
            }
            nodes = null; // release node references for last window
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
            windowIndex = 0;
        }

        Node node = nodes.get(windowIndex);
        Validate.notNull(node);

        windowIndex++;
        return node;
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
}
