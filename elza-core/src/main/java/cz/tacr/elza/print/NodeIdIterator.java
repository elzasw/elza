package cz.tacr.elza.print;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
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
        // add children to stack
        List<NodeId> children = node.getChildren();
        if (!children.isEmpty()) {
            ListIterator<NodeId> clit = children.listIterator(children.size());
            while (clit.hasPrevious()) {
                nodeIdStack.addFirst(clit.previous());
            }
        }
        return node;
    }
}
