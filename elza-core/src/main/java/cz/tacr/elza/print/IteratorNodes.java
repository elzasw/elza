package cz.tacr.elza.print;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.lang.Validate;

/**
 * Implementace iterátoru s cache pro načítání bloku dat.
 */
public class IteratorNodes implements Iterator<Node> {

    private final NodeLoader nodeLoader;

    /**
     * Seznam NodeId seřazených podle iterace.
     */
    private final Iterator<NodeId> nodeIdIterator;

    /**
     * Velikost cache.
     */
    private final int windowSize;

    /**
     * Uložiště pro načtené uzly.
     */
    private List<Node> nodes;

    /**
     * Pozice další iterované položky.
     */
    private int windowIndex;

    public IteratorNodes(NodeLoader nodeLoader, Iterator<NodeId> nodeIdIterator, int windowSize) {
        this.nodeLoader = nodeLoader;
        this.nodeIdIterator = nodeIdIterator;
        this.windowSize = windowSize;
        this.windowIndex = windowSize; // force to load first window
    }

    /**
     * Creates node iterator with default window size of 1000.
     */
    public IteratorNodes(NodeLoader nodeLoader, Iterator<NodeId> nodeIdIterator) {
        this(nodeLoader, nodeIdIterator, 1000);
    }

    @Override
    public boolean hasNext() {
        return nodeIdIterator.hasNext() || (nodes != null && windowIndex < nodes.size());
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
     * Seznam dalšího bloku pro načtení.
     *
     * @return seznam NodeId
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
