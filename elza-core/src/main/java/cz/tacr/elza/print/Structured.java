package cz.tacr.elza.print;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrStructuredObject;

/**
 * Structured
 * @Since  16.11.2017
 */
public class Structured {

    private final List<NodeId> nodeIds = new ArrayList<>();

    private final NodeLoader nodeLoader;

    private String value;

    private Structured(NodeLoader nodeLoader) {
        this.nodeLoader = nodeLoader;
    }

    /**
     * Metoda pro získání hodnoty do fieldu v Jasper.
     * Umožní na položce v detailu volat metody sám nad sebou (nejen implicitně zpřístupněné gettery).
     *
     * @return odkaz sám na sebe
     */
    public Structured getStructured() {
        return this;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public NodeIterator getNodes() {
        Iterator<NodeId> nodeIdIterator = nodeIds.iterator();
        return new NodeIterator(nodeLoader, nodeIdIterator);
    }

    void addNodeId(NodeId nodeId) {
        Validate.notNull(nodeId);
        nodeIds.add(nodeId);
    }

    /**
     * Create instance of structured object
     * 
     * @param structObj
     * @param nodeLoader
     * @return
     */
    public static Structured newInstance(ArrStructuredObject structObj, NodeLoader nodeLoader) {
        Structured result = new Structured(nodeLoader);
        result.setValue(structObj.getValue());
        return result;
    }

    public static Structured newInstance(String value, NodeLoader nodeLoader) {
        Structured result = new Structured(nodeLoader);
        result.setValue(value);
        return result;
    }
}
