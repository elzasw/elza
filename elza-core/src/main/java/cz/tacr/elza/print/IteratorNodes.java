package cz.tacr.elza.print;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Implementace iterátoru s cache pro načítání bloku dat.
 *
 * @author Martin Šlapa
 * @since 29.08.2016
 */
public class IteratorNodes implements Iterator<Node> {

    /**
     * Seznam NodeId seřazených podle iterace.
     */
    private final List<NodeId> nodeIds;

    /**
     * Velikost cache.
     */
    private final int windowSize;

    /**
     * Třída, která zajišťuje načítání dat o uzlech.
     */
    private final NodeLoader loader;

    /**
     * Uložiště pro načtené uzly.
     */
    private Map<Integer, Node> nodes = null;

    /**
     * Pozice další iterované položky.
     */
    private int position = 0;

    /**
     * Svázaný výstup.
     */
    private final Output output;

    public IteratorNodes(final Output output, final List<NodeId> nodeIds, final NodeLoader loader, final int windowSize) {
        this.output = output;
        this.nodeIds = nodeIds;
        this.windowSize = windowSize;
        this.loader = loader;
    }

    @Override
    public boolean hasNext() {
        return nodeIds.size() > position;
    }

    @Override
    public Node next() {

        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        NodeId nodeId = nodeIds.get(position);

        if (nodes == null) {
            nodes = loader.loadNodes(output, nextIds());
        }

        Node node = nodes.get(nodeId.getArrNodeId());

        if (node == null) {
            nodes = loader.loadNodes(output, nextIds());
            node = nodes.get(nodeId.getArrNodeId());

            if (node == null) {
                throw new NoSuchElementException("Node " + nodeId + " se nepodařilo načíst do cache");
            }
        }

        position++;
        return node;
    }

    /**
     * Vrací aktuální pozici - NodeId.
     *
     * @return aktuální NodeId
     */
    public NodeId getActualNodeId() {
        return nodeIds.get(position - 1);
    }

    /**
     * Seznam dalšího bloku pro načtení.
     *
     * @return seznam NodeId
     */
    private List<NodeId> nextIds() {
        int from = position;
        int to = nodeIds.size() > from + windowSize ? from + windowSize : nodeIds.size();
        return nodeIds.subList(from, to);
    }

    /**
     * Vrací aktuální seznam NodeId.
     *
     * @return seznam NodeId
     */
    public List<NodeId> getNodeIds() {
        return nodeIds;
    }

    public Node moveTo(final NodeId nodeId) {
        if (!nodeIds.contains(nodeId)) {
            throw new NoSuchElementException();
        }
        position = nodeIds.indexOf(nodeId);
        return next();
    }
}
