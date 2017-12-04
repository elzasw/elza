package cz.tacr.elza.print;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * NodeId subtree iterator. Can be initialized as breadth-first or depth-first iterator.
 */
public class NodeIdIterator implements Iterator<NodeId> {

    private final LinkedList<NodeId> nodeIdQueue = new LinkedList<>();

    private final boolean depthFirst;

    public NodeIdIterator(NodeId root, boolean depthFirst) {
        this.nodeIdQueue.add(root);
        this.depthFirst = depthFirst;
    }

    @Override
    public boolean hasNext() {
        return nodeIdQueue.size() > 0;
    }

    @Override
    public NodeId next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        NodeId node = nodeIdQueue.removeFirst();
        if (node.children != null) {
            if (depthFirst) {
                node.children.forEach(nodeIdQueue::addFirst);
            } else {
                node.children.forEach(nodeIdQueue::addLast);
            }
        }
        return node;
    }
}
