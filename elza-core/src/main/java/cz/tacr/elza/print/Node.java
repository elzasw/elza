package cz.tacr.elza.print;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemRecordRef;
import cz.tacr.elza.print.item.ItemSpec;

/**
 * Node with data
 */
public class Node {

    private final NodeId nodeId;

    private List<Item> items;

    private List<Record> nodeAPs;

    /**
     * Konstruktor s povinnými hodnotami
     *
     * @param nodeId vazba na nodeId
     * @param nodel vazba na output
     */
    public Node(NodeId nodeId) {
        this.nodeId = nodeId;
    }

    public NodeId getNodeId() {
        return nodeId;
    }

    /**
     * @return dohledá v output.modes node, který je nadřazený tomuto. Pokud není nalezen nebo
     *         neexistuje vrací null.
     */
    public NodeId getParent() {
        return nodeId.getParent();
    }

    /**
     * @return vrací seznam dětí, omezeno jen na node v outputu
     */
    public List<NodeId> getChildren() {
        return nodeId.getChildren();
    }

    public Integer getDepth() {
        return nodeId.getDepth();
    }

    public Integer getPosition() {
        return nodeId.getPosition();
    }

    /**
     * @return všechny Items přiřazené na node.
     */
    public List<Item> getItems() {
        if (items == null) {
            return Collections.emptyList();
        }
        return items;
    }

    /**
     * @param typeCodes seznam kódů typů atributů.
     * @return vrací se seznam hodnot těchto atributů, řazeno dle rul_desc_item.view_order +
     *         arr_item.position
     */
    public List<Item> getItems(final Collection<String> typeCodes) {
        Validate.notNull(typeCodes);

        if (items == null || typeCodes.isEmpty()) {
            return Collections.emptyList();
        }

        return items.stream().filter(item -> {
            String tc = item.getType().getCode();
            return typeCodes.contains(tc);
        }).collect(Collectors.toList());
    }

    /**
     * Return list of items with given specification
     *
     * @param typeCode Code of the item
     * @param specCode Code of specificaion
     * @return
     */
    public List<Item> getItemsWithSpec(final String typeCode, final String specCode) {
        Validate.notNull(typeCode);
        Validate.notNull(specCode);

        if (items == null) {
            return Collections.emptyList();
        }

        return items.stream().filter(item -> {
            String tc = item.getType().getCode();
            if (!typeCode.equals(tc)) {
                return false;
            }
            ItemSpec is = item.getSpecification();
            return is != null && specCode.equals(is.getCode());
        }).collect(Collectors.toList());
    }

    /**
     * Vstupem je seznam kódu typů atributů a vrací se seznam všech hodnot atributů výstupu kromě
     * hodnot typů uvedených ve vstupu metody, řazeno dle rul_desc_item.view_order +
     * arr_item.position.
     *
     * @param codes seznam kódu typů atributů
     * @return seznam všech hodnot atributů kromě hodnot typů uvedených ve vstupu metody
     */
    public List<Item> getItemsWithout(final Collection<String> typeCodes) {
        Validate.notNull(typeCodes);

        if (items == null) {
            return Collections.emptyList();
        }

        return items.stream().filter(item -> {
            String tc = item.getType().getCode();
            return !typeCodes.contains(tc);
        }).collect(Collectors.toList());
    }

    public Item getSingleItem(String typeCode) {
        Validate.notEmpty(typeCode);

        Item found = null;
        for (Item item : items) {
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

    public String getSingleItemValue(String itemTypeCode) {
        Item found = getSingleItem(itemTypeCode);
        if (found != null) {
            return found.getSerializedValue();
        }
        return StringUtils.EMPTY;
    }

    /**
     * Return list of records connected to the node or to description item
     *
     * @return
     */
    public List<Record> getRecords() {
        List<Record> allAPs = new ArrayList<>();

        if (nodeAPs != null) {
            allAPs.addAll(nodeAPs);
        }
        if (items != null) {
            for (Item item : items) {
                if (item instanceof ItemRecordRef) {
                    allAPs.add(item.getValue(Record.class));
                }
            }
        }
        return allAPs;
    }

    public List<Record> getNodeRecords() {
        return nodeAPs;
    }

    /* internal methods */

    void setItems(List<Item> items) {
        this.items = items;
    }

    void setNodeAPs(List<Record> nodeAPs) {
        this.nodeAPs = nodeAPs;
    }
}
