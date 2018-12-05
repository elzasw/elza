package cz.tacr.elza.print;

import java.util.List;

import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.print.item.Item;

/**
 * Structured
 * @Since  16.11.2017
 */
public class Structured {

    private List<Item> items;

    //private final List<NodeId> nodeIds = new ArrayList<>();

    private final NodeLoader nodeLoader;

    final private Integer id;

    private String value;

    private Structured(Integer id, NodeLoader nodeLoader) {
        this.id = id;
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

    /*
    public NodeIterator getNodes() {
        Iterator<NodeId> nodeIdIterator = nodeIds.iterator();
        return new NodeIterator(nodeLoader, nodeIdIterator);
    }
    
    void addNodeId(NodeId nodeId) {
        Validate.notNull(nodeId);
        nodeIds.add(nodeId);
    }*/

    public boolean hasItem(String itemTypeCode) {
        load();

        boolean exists = items.stream().anyMatch(item -> item.getType().getCode().equals(itemTypeCode));

        return exists;
    }

    private void load() {
        if (items != null) {
            return;
        }
        items = nodeLoader.loadStructItems(id);
    }

    /**
     * Create instance of structured object
     * 
     * @param structObj
     * @param nodeLoader
     * @return
     */
    public static Structured newInstance(ArrStructuredObject structObj, NodeLoader nodeLoader) {
        Structured result = new Structured(structObj.getStructuredObjectId(), nodeLoader);
        result.setValue(structObj.getValue());
        return result;
    }
}
