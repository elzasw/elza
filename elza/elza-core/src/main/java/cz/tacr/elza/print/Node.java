package cz.tacr.elza.print;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrInhibitedItem;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemRecordRef;
import cz.tacr.elza.print.item.ItemSpec;
import cz.tacr.elza.print.item.convertors.OutputItemConvertor;
import cz.tacr.elza.service.cache.RestoredNode;

/**
 * Node with data
 */
public class Node {

    private final static Logger log = LoggerFactory.getLogger(Node.class);

    private final Fund fund;

    private final NodeId nodeId;

    private List<Item> items;

    private List<Dao> daos;

    /**
     * UUID of the node
     */
    private String uuid;

    /**
     * Provide parent nodes
     */
    NodeProvider nodeProvider;
    
    /**
     * Set of inhibited item
     * 
     * Set contains descItemObjectId of ArrInhibitedItem
     */
    private Set<Integer> inhibitedItemIds;

    /**
     * Konstruktor s povinnými hodnotami
     *
     * @param nodeId
     *            vazba na nodeId
     * @param nodel
     *            vazba na output
     */
    public Node(final Fund fund, final NodeId nodeId) {
        this.fund = fund;
        this.nodeId = nodeId;
    }

    public NodeId getNodeId() {
        return nodeId;
    }

    public String getUuid() {
        return uuid;
    }

    /**
     * @return dohledá v output.modes node, který je nadřazený tomuto. Pokud není nalezen nebo
     *         neexistuje vrací null.
     */
    public NodeId getParent() {
        return nodeId.getParent();
    }

    public Node getParentNode() {
        NodeId parentNodeId = nodeId.getParent();
        if (parentNodeId == null) {
            return null;
        }
        if (nodeProvider == null) {
            return null;
        }
        return nodeProvider.getNode(parentNodeId);
    }

    /**
     * Return collection of all parent nodes
     * 
     * @return
     */
    public List<Node> getParentNodes() {
        NodeId parentNodeId = nodeId.getParent();
        if (parentNodeId == null) {
            return Collections.emptyList();
        }
        if (nodeProvider == null) {
            return null;
        }
        List<Node> result = new ArrayList<>();
        while (parentNodeId != null) {
            Node parentNode = nodeProvider.getNode(parentNodeId);
            result.add(parentNode);
            parentNodeId = parentNode.getParent();
        }
        return result;
    }

    public NodeProvider getNodeProvider() {
        return nodeProvider;
    }

    void setNodeProvider(NodeProvider nodeProvider) {
        this.nodeProvider = nodeProvider;
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
        Objects.requireNonNull(typeCodes);

        if (CollectionUtils.isEmpty(items) || CollectionUtils.isEmpty(typeCodes)) {
            return Collections.emptyList();
        }

        return items.stream().filter(item -> {
            String tc = item.getType().getCode();
            return typeCodes.contains(tc);
        }).collect(Collectors.toList());
    }

    public List<Item> getItems(final Collection<String> typeCodes, final Collection<String> specCodes) {
    	Objects.requireNonNull(typeCodes);

        if (CollectionUtils.isEmpty(items) || CollectionUtils.isEmpty(typeCodes)) {
            return Collections.emptyList();
        }

        return items.stream().filter(item -> {
            String tc = item.getType().getCode();
            if (!typeCodes.contains(tc)) {
                return false;
            }
            // check specification
            if (item.getSpecification() == null) {
                // add items without spec
                return true;
            }
            String specCode = item.getSpecification().getCode();
            return specCodes.contains(specCode);
        }).collect(Collectors.toList());
    }

    /**
     * Return list of items with given spec
     * 
     * If item is without spec it is also returned.
     * First are items from top most parent.
     * 
     * @param typeCodes
     * @param specCodes
     * @return
     */
    public List<List<Item>> getItemsFromParent(final Collection<String> typeCodes, final Collection<String> specCodes) {
        NodeId parentNodeId = this.getParent();
        List<List<Item>> parentItems;
        if (parentNodeId != null) {
            Node parentNode = nodeProvider.getNode(parentNodeId);
            parentItems = parentNode.getItemsFromParent(typeCodes, specCodes);
        } else {
            parentItems = Collections.emptyList();
        }
        List<Item> localItems = getItems(typeCodes, specCodes);
        if (localItems.isEmpty()) {
            return parentItems;
        } else {
            List<List<Item>> result = new ArrayList<>(parentItems.size() + 1);
            result.addAll(parentItems);
            result.add(localItems);
            return result;
        }
    }
    
    /**
     * Check if item is own item 
     * @param item
     * @return
     */
    public boolean isOwnItem(Item item) {
        if(items==null) {
            return false;
        }
        return items.contains(item);
    }
    
    /**
     * Return list of own and inherited items
     * @param typeCodes
     * @return
     */
    public List<Item> getOwnAndInheritedItems(final String typeCode) {
    	// read own items
    	List<Item> ownItems = this.getItems(Collections.singleton(typeCode));
    	
    	NodeId parentNodeId = this.getParent();
        if (parentNodeId == null) {
        	return ownItems;
        }
        if(nodeProvider==null) {
            throw new IllegalStateException("Node provider not set");
        }
        Node parentNode = nodeProvider.getNode(parentNodeId);
        List<Item> inheritedItems = parentNode.getOwnAndInheritedItems(typeCode);
        if(CollectionUtils.isEmpty(inheritedItems)) {
        	// if no inherited items, return only own
        	return ownItems;
        }
        // Combined items
        List<Item> combinedItems = new ArrayList<>(ownItems.size() + inheritedItems.size());
        // Append inherited items if not previously inhibited
        for(Item inheritedItem : inheritedItems) {
            if(inheritedItem.getDescItemObjectId()!=null && inhibitedItemIds!=null && 
            		inhibitedItemIds.contains(inheritedItem.getDescItemObjectId())) {
                continue;
            }
            combinedItems.add(inheritedItem);
        }
        combinedItems.addAll(ownItems);
        return combinedItems;
    }

    /**
     * Return list of inherited items
     * @param typeCodes
     * @return
     */
    public List<Item> getInheritedItems(final String typeCode) {    	
    	NodeId parentNodeId = this.getParent();
        if (parentNodeId == null) {
        	return Collections.emptyList();
        }
        Node parentNode = nodeProvider.getNode(parentNodeId);
        if(parentNode==null) {
            log.error("Parent node for node {} not found, parent node id: {}", nodeId, parentNodeId);
            throw new IllegalStateException("Parent node for node " + nodeId + " not found, parent node id: " + parentNodeId);
        }
        List<Item> inheritedItems = parentNode.getOwnAndInheritedItems(typeCode);
        if(CollectionUtils.isEmpty(inheritedItems)) {
        	// if no inherited items, return only own
        	return Collections.emptyList();
        }
        // Combined items
        List<Item> result = new ArrayList<>(inheritedItems.size());
        // Append inherited items if not previously inhibited
        for(Item inheritedItem : inheritedItems) {
            if(inheritedItem.getDescItemObjectId()!=null && 
            		inhibitedItemIds!=null &&
            		inhibitedItemIds.contains(inheritedItem.getDescItemObjectId())) {
                continue;
            }
            result.add(inheritedItem);
        }
        return result;
    }

    /**
     * Return list of items
     * 
     * First are items from top most parent.
     * 
     * @param typeCodes
     * @return
     */
    public List<List<Item>> getItemsFromParent(final Collection<String> typeCodes) {
        Validate.notNull(typeCodes);

        if (CollectionUtils.isEmpty(items) || CollectionUtils.isEmpty(typeCodes)) {
            return Collections.emptyList();
        }

        NodeId parentNodeId = this.getParent();
        List<List<Item>> parentItems;
        if (parentNodeId != null) {
            Node parentNode = nodeProvider.getNode(parentNodeId);
            parentItems = parentNode.getItemsFromParent(typeCodes);
        } else {
            parentItems = Collections.emptyList();
        }
        List<Item> localItems = getItems(typeCodes);

        if (localItems.isEmpty()) {
            return parentItems;
        } else {
            List<List<Item>> result = new ArrayList<>(parentItems.size() + 1);
            result.addAll(parentItems);
            result.add(localItems);
            return result;
        }
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

        if (CollectionUtils.isEmpty(items)) {
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

    public boolean hasItemWithSpec(String typeCode, String specCode) {
        Validate.notNull(typeCode);
        Validate.notNull(specCode);

        List<Item> validItems = getItemsWithSpec(typeCode, specCode);
        return CollectionUtils.isNotEmpty(validItems);
    }

    public boolean hasItem(String typeCode) {
        Validate.notNull(typeCode);

        List<Item> validItems = getItems(Collections.singletonList(typeCode));
        return CollectionUtils.isNotEmpty(validItems);
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

        if (CollectionUtils.isEmpty(items)) {
            return Collections.emptyList();
        }

        return items.stream().filter(item -> {
            String tc = item.getType().getCode();
            return !typeCodes.contains(tc);
        }).collect(Collectors.toList());
    }

    public Item getSingleItem(String typeCode) {
        Validate.notEmpty(typeCode);

        if (CollectionUtils.isEmpty(items)) {
            return null;
        }

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
     * Return list of records connected to description item
     *
     * @return
     */
    public List<Record> getRecords() {

        if (CollectionUtils.isEmpty(items)) {
            return Collections.emptyList();
        }

        List<Record> allAPs = new ArrayList<>();
        for (Item item : items) {
            if (item instanceof ItemRecordRef) {
                    allAPs.add(item.getValue(Record.class));
            }
        }
        return allAPs;
    }

    /* internal methods */

    void setItems(List<Item> items) {
        this.items = items;
    }

    /**
     * Init output node from node cache.
     */
    public void load(RestoredNode cachedNode, OutputItemConvertor conv) {
        uuid = cachedNode.getUuid();
        // set node items
        List<ArrDescItem> descItems = cachedNode.getDescItems();
        if (descItems != null) {
            this.items = OutputModel.convert(descItems, conv);
        }
    	
        List<ArrInhibitedItem> inhibitedItems = cachedNode.getInhibitedItems();
		if (CollectionUtils.isNotEmpty(inhibitedItems)) {
			inhibitedItemIds = inhibitedItems.stream().map(ArrInhibitedItem::getDescItemObjectId).collect(Collectors.toSet());
		}
    }

    public Fund getFund() {
        return fund;
    }

    public void addDao(Dao dao) {
        if (daos == null) {
            daos = new ArrayList<>();
        }
        daos.add(dao);
    }

    /**
     * Return collection of daos
     * 
     * @return
     */
    public List<Dao> getDaos() {
        if (daos == null) {
            return Collections.emptyList();
        } else {
            return daos;
        }
    }
}
