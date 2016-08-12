package cz.tacr.elza.print;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemRecordRef;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.output.OutputFactoryService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.transaction.annotation.Transactional;
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
@Scope("prototype")
public class Node implements RecordProvider, Comparable<Node> {
    private final NodeId nodeId; // vazba na node
    private final Output output;

    private List<Item> items = null; // seznam všech atributů node - cache plněná při prním přístupu
    private List<Record> records = null; // seznam všech registry node - cache plněná při prvním přístupu

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private OutputFactoryService outputFactoryService;

    @Autowired
    private ItemRepository itemRepository;

    /**
     * Konstruktor s povinnými hodnotami
     * @param nodeId vazba na nodeId
     * @param output vazba na output
     */
    public Node(NodeId nodeId, Output output) {
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
    public List<Item> getItems(@NotNull Collection<String> codes) {
        Assert.notNull(codes);
        return getItems().stream()
                .filter(item -> {
                    final String code = item.getType().getCode();
                    return codes.contains(code);
                })
                .sorted(Item::compareToItemViewOrderPosition)
                .collect(Collectors.toList());
    }

    /**
     * Metoda pro získání hodnoty do fieldu v Jasper.
     *
     * @param code požadovaný kód položky
     * @return vrací seznam hodnot položek s odpovídajícím kódem oddělený čárkou (typicky 1 položka = její serializeValue)
     */
    public String getItemsValueByCode(@NotNull String code) {
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
    public List<Item> getAllItems(@NotNull Collection<String> codes) {
        Assert.notNull(codes);
        return getItems().stream()
                .filter(item -> !codes.contains(item.getType().getCode()))
                .sorted(Item::compareToItemViewOrderPosition)
                .collect(Collectors.toList());
    }

    /**
     * Metoda pro potřeby jasperu
     * @return položky jako getItems, serializované a spojené čárkou
     */
    public String getAllItemsAsString(@NotNull Collection<String> codes) {
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
    @Transactional(readOnly = true)
    public List<Item> getItems() {
        final ArrFundVersion arrFundVersion = fundVersionRepository.getOneCheckExist(output.getArrFundVersionId());
        final ArrChange lockChange = arrFundVersion.getLockChange();
        final ArrNode arrNode = nodeRepository.getOneCheckExist(getArrNodeId());

        // zajistí naplnění cache, pokud není načteno
        if (items == null) {
            final List<ArrDescItem> descItems;
            if (lockChange != null) {
                descItems = descItemRepository.findByNodeAndChange(arrNode, lockChange);
            } else {
                descItems = descItemRepository.findByNodeAndDeleteChangeIsNull(arrNode);
            }
            items = new ArrayList<>(descItems.size());
            descItems.stream()
                    .sorted((o1, o2) -> o1.getPosition().compareTo(o2.getPosition()))
                    .forEach(arrDescItem -> {
                        final ArrItem arrItem = itemRepository.findOne(arrDescItem.getItemId());

                        final AbstractItem item = outputFactoryService.getItem(arrItem.getItemId(), output, this.getNodeId());
                        item.setPosition(arrDescItem.getPosition());

                        items.add(item);
                    });
        }

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

    public List<Record> getRecords() {
        // registers navázané k node - inicializovat
        if (records == null) {
            records = outputFactoryService.getRecordByNodeId(output, this.getNodeId());
        }

        final List<Record> recordList = new ArrayList<>(this.records); // interně navázané recordy jako první

        // recordy z itemů
        getItems().stream()
                .filter(item -> item instanceof ItemRecordRef)
                .forEach(item -> {
                    ItemRecordRef itemRecordRef = (ItemRecordRef) item;
                    recordList.add(itemRecordRef.getValue());
                });

        return recordList;
    }

    @Override
    public List<NodeId> getRecordProviderChildern() {
        return new ArrayList<>(getChildren());
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
    public int compareTo(Node o) {
        return CompareToBuilder.reflectionCompare(this, o);
    }
}
