package cz.tacr.elza.print;

import java.util.List;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.print.item.Item;
import net.bytebuddy.implementation.bytecode.Throw;

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

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public List<Item> getItems() {
        load();

        return items;
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

    public boolean hasItemWithSpec(String itemTypeCode, String itemSpecCode) {
        load();

        boolean exists = items.stream().anyMatch(
                                                 item -> (item.getType().getCode().equals(itemTypeCode) &&
                                                         item.getSpecification().getCode().equals(itemSpecCode)));

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

    public Integer getId() {
        return id;
    }

    /**
     * Return single item
     * 
     * @param itemTypeCode
     *            Code of item
     * @return Return single item if exists. Return null if item does not exists.
     * @throws Throw
     *             exception if there are multiple items with same type.
     */
    Item getSingleItem(final String typeCode) {
        load();

        Validate.notEmpty(typeCode);

        cz.tacr.elza.print.item.Item found = null;
        for (cz.tacr.elza.print.item.Item item : items) {
            if (typeCode.equals(item.getType().getCode())) {
                // check if item already found
                if (found != null) {
                    throw new IllegalStateException("Multiple items with same code exists: " + typeCode);
                }
                found = item;
            }
        }
        return found;

    }
}
