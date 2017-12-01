package cz.tacr.elza.print;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.springframework.util.Assert;

import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemRecordRef;
import cz.tacr.elza.print.item.ItemSpec;

/**
 * Node with data
 */
public class Node {

    private final NodeId nodeId;

    private final OutputModelNew model;

    private final List<Item> items;

    private final List<Record> records;

    /**
     * Konstruktor s povinnými hodnotami
     * @param nodeId vazba na nodeId
     * @param nodel vazba na output
     */
    public Node(NodeId nodeId, OutputModelNew nodel, List<Item> items, List<Record> records) {
        this.nodeId = nodeId;
        this.model = nodel;
        this.items = items;
        this.records = records;
    }

    /**
     * @return dohledá v output.modes node, který je nadřazený tomuto. Pokud není nalezen nebo neexistuje vrací null.
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

    /**
     * @param codes seznam kódů typů atributů.
     * @return vrací se seznam hodnot těchto atributů, řazeno dle rul_desc_item.view_order + arr_item.position
     */
    public List<Item> getItems(@NotNull final Collection<String> codes) {
        Assert.notNull(codes, "Kódy musí být vyplněny");
        return getItems().stream()
                .filter(item -> {
                    final String code = item.getType().getCode();
                    return codes.contains(code);
                })
                .collect(Collectors.toList());
    }

    /**
     * Return list of items with given specification
     *
     * @param code 		Code of the item
     * @param specCode	Code of specificaion
     * @return
     */
    public List<Item> getItemsWithSpec(@NotNull final String code, @NotNull final String specCode) {
    	Assert.notNull(code, "Kód musí být vyplněn");
    	Assert.notNull(specCode, "Kód specifikace musí být vyplněn");

    	List<Item> result = new ArrayList<>();
    	items.forEach(item -> {
    		if(code.equals(item.getType().getCode())) {
    			// Check specification
    			ItemSpec is = item.getSpecification();
    			if(is!=null) {
    				if(specCode.equals(is.getCode())) {
    					result.add(item);
    				}
    			}
    		}
    	});

    	return result;
    }

    /**
     * Metoda pro získání hodnoty do fieldu v Jasper.
     *
     * @param code požadovaný kód položky
     * @return vrací seznam hodnot položek s odpovídajícím kódem oddělený čárkou (typicky 1 položka = její serializeValue)
     */
    /*
     * XXX: Not needed -> remove in future.
     *
     * public String getItemsValueByCode(@NotNull final String code) {
        return getItems(Collections.singletonList(code)).stream()
                .map(Item::serializeValue)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(", "));
    } */

    /**
     * Vstupem je seznam kódu typů atributů a vrací se seznam všech hodnot atributů výstupu kromě hodnot
     * typů uvedených ve vstupu metody, řazeno dle rul_desc_item.view_order + arr_item.position.
     *
     * @param codes     seznam kódu typů atributů
     * @return   seznam všech hodnot atributů kromě hodnot typů uvedených ve vstupu metody
     */
    public List<Item> getItemsWithout(@NotNull final Collection<String> codes) {
        Assert.notNull(codes, "Kódy musí být vyplněny");
        return getItems().stream()
                .filter(item -> !codes.contains(item.getType().getCode()))
                .collect(Collectors.toList());
    }

    /**
     * Metoda pro potřeby jasperu
     * @return položky jako getItems, serializované a spojené čárkou
     */
    /*
     * XXX: Not needed -> remove in future.
     *
     * public String getAllItemsAsString(@NotNull final Collection<String> codes) {
       return getItems(codes).stream()
                // .map((item) -> item.serialize() + "[" + item.getType().getCode() + "]") // pro potřeby identifikace políčka při ladění šablony
               .map(Item::serialize)
               .filter(StringUtils::isNotBlank)
               .collect(Collectors.joining(", "));
    } */

    /**
     * @return všechny Items přiřazené na node.
     */
    public List<Item> getItems() {
        return items;
    }

    public Integer getDepth() {
        return getNodeId().getDepth();
    }

    public Integer getPosition() {
        return getNodeId().getPosition();
    }

    public NodeId getNodeId() {
        return nodeId;
    }

    /**
     * Return list of records connected to the node or to description item
     * @return
     */
    public List<Record> getRecords() {
    	// interně navázané recordy jako první
        final List<Record> recordList = new ArrayList<>(records);

        // recordy z itemů
        for (Item item : getItems()) {
            if (item instanceof ItemRecordRef) {
                recordList.add(item.getValue(Record.class));
            }
        }

        return recordList;
    }

    public List<Record> getNodeRecords() {
        return records;
    }

    /**
     * @return instance iterátoru, který prochází jednotky popisu do hloubky
     */
    public IteratorNodes getNodesDFS() {
        return new IteratorNodes(model, model.getNodesChildsModel(nodeId), outputFactoryService, OutputModel.MAX_CACHED_NODES);
    }
}
