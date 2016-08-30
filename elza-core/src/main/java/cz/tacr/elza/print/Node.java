package cz.tacr.elza.print;

import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemRecordRef;
import cz.tacr.elza.service.output.OutputFactoryService;
import cz.tacr.elza.utils.AppContext;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.util.Assert;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class Node implements RecordProvider, Comparable<Node>, NodesOrder {

    private final NodeId nodeId; // vazba na node
    private final Output output;

    private List<Item> items = new ArrayList<>();
    private List<Record> records = new ArrayList<>();

    private OutputFactoryService outputFactoryService = AppContext.getBean(OutputFactoryService.class);

    /**
     * Konstruktor s povinnými hodnotami
     * @param nodeId vazba na nodeId
     * @param output vazba na output
     */
    public Node(final NodeId nodeId, final Output output) {
        this.nodeId = nodeId;
        this.output = output;
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
    public Set<NodeId> getChildren() {
        return nodeId.getChildren();
    }

    /**
     * @param codes seznam kódů typů atributů.
     * @return vrací se seznam hodnot těchto atributů, řazeno dle rul_desc_item.view_order + arr_item.position
     */
    public List<Item> getItems(@NotNull final Collection<String> codes) {
        Assert.notNull(codes);
        return getItems().stream()
                .filter(item -> {
                    final String code = item.getType().getCode();
                    return codes.contains(code);
                })
                .collect(Collectors.toList());
    }

    /**
     * Metoda pro získání hodnoty do fieldu v Jasper.
     *
     * @param code požadovaný kód položky
     * @return vrací seznam hodnot položek s odpovídajícím kódem oddělený čárkou (typicky 1 položka = její serializeValue)
     */
    public String getItemsValueByCode(@NotNull final String code) {
        return getItems(Collections.singletonList(code)).stream()
                .map(Item::serializeValue)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(", "));
    }

    /**
     * Metoda pro získání hodnoty do fieldu v Jasper.
     * Umožní na položce v detailu volat metody sám nad sebou (nejen implicitně zpřístupněné gettery).
     *
     * @return odkaz sám na sebe
     */
    public Node getNode() {
        return this;
    }

    /**
     * Vstupem je seznam kódu typů atributů a vrací se seznam všech hodnot atributů výstupu kromě hodnot
     * typů uvedených ve vstupu metody, řazeno dle rul_desc_item.view_order + arr_item.position.
     *
     * @param codes     seznam kódu typů atributů
     * @return   seznam všech hodnot atributů kromě hodnot typů uvedených ve vstupu metody
     */
    public List<Item> getAllItems(@NotNull final Collection<String> codes) {
        Assert.notNull(codes);
        return getItems().stream()
                .filter(item -> !codes.contains(item.getType().getCode()))
                .collect(Collectors.toList());
    }

    /**
     * Metoda pro potřeby jasperu
     * @return položky jako getItems, serializované a spojené čárkou
     */
    public String getAllItemsAsString(@NotNull final Collection<String> codes) {
       return getItems(codes).stream()
//               .map((item) -> item.serialize() + "[" + item.getType().getCode() + "]") // pro potřeby identifikace políčka při ladění šablony
               .map(Item::serialize)
               .filter(StringUtils::isNotBlank)
               .collect(Collectors.joining(", "));
    }

    public Integer getArrNodeId() {
        return nodeId.getArrNodeId();
    }

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

    @Override
    public List<Record> getRecords() {
        final List<Record> recordList = new ArrayList<>(records); // interně navázané recordy jako první

        // recordy z itemů
        for (Item item : getItems()) {
            if (item instanceof ItemRecordRef) {
                ItemRecordRef itemRecordRef = (ItemRecordRef) item;
                recordList.add(itemRecordRef.getValue());
            }
        }

        return recordList;
    }

    public List<Record> getNodeRecords() {
        return records;
    }

    @Override
    public IteratorNodes getRecordProviderChildren() {
        return new IteratorNodes(output, new ArrayList<>(getChildren()), outputFactoryService, Output.MAX_CACHED_NODES);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof Node) {
            final Node o1 = (Node) o;
            return new EqualsBuilder().append(getArrNodeId(), o1.getArrNodeId()).isEquals();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        // podstatný je zdrojový arrNode
        return new HashCodeBuilder().append(getArrNodeId()).toHashCode();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ").add(getDepth().toString()).add(getPosition().toString()).toString();
    }

    @Override
    public int compareTo(final Node o) {
        return CompareToBuilder.reflectionCompare(this, o);
    }

    @Override
    public IteratorNodes getNodesDFS() {
        return new IteratorNodes(output, output.getNodesChildsModel(nodeId), outputFactoryService, Output.MAX_CACHED_NODES);
    }

    @Override
    public IteratorNodes getNodesBFS() {
        List<NodeId> nodeIds = output.getNodesChildsModel(nodeId);
        nodeIds.sort((o1, o2) -> new CompareToBuilder()
                .append(o1.getDepth(), o2.getDepth())  // nejprve nejvyšší nody
                .append(o1.getParent(), o2.getParent()) // pak sezkupit dle parenta
                .append(o1.getPosition(), o2.getPosition()) // pak dle pořadí
                .toComparison());
        return new IteratorNodes(output, nodeIds, outputFactoryService, Output.MAX_CACHED_NODES);
    }

    public void setItems(final List<Item> items) {
        this.items = items;
    }

    public void setRecords(final List<Record> records) {
        this.records = records;
    }
}
