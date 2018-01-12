package cz.tacr.elza.print;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * NodeId subtree depth-first iterator.
 */
public class NodeIdIterator implements Iterator<NodeId> {

    private final LinkedList<NodeId> nodeIdStack = new LinkedList<>();

    public NodeIdIterator(NodeId rootNodeId) {
        this.nodeIdStack.add(rootNodeId);
    }

    @Override
    public boolean hasNext() {
        return nodeIdStack.size() > 0;
    }

    @Override
    public NodeId next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        NodeId node = nodeIdStack.removeFirst();
        if (node.children != null) {
            node.children.forEach(nodeIdStack::addFirst);
        }
        return node;
    }
}
