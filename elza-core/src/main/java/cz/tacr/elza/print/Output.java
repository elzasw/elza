package cz.tacr.elza.print;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemFile;
import cz.tacr.elza.print.item.ItemPacketRef;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.output.OutputFactoryService;
import cz.tacr.elza.utils.AppContext;

/**
 * Základní objekt pro generování výstupu, při tisku se vytváří 1 instance.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 21.6.16
 */
public class Output implements RecordProvider, NodesOrder {

    public static final int MAX_CACHED_NODES = 100; // maximální počet nodů v cache

    private final int outputId; // ID pro vazbu do DB na entitu arr_output

    private OutputFactoryService outputFactoryService = AppContext.getBean(OutputFactoryService.class);

    private String internal_code;
    private String name;
    private String type;
    private String typeCode;
    private Fund fund;

    private int page = 0;

    /**
     * Vnitřní iterátor - cache.
     */
    private IteratorNodes iteratorNodes = null;

    // seznam všech atributů outputu
    private List<Item> items = new ArrayList<>();

    // seznam všech node outputu (přímo přiřazené + jejich potomci + nadřízení až do root);
    // mapa má jako klíč ID Nodu odpovídající ArrNode.arrNodeId
    private Map<Integer, NodeId> nodeIdsMap = new HashMap<>();

    // seznam rejstříkových hesel všech nodes outputu odkazovaných přes arr_node_register
    private List<Record> records = null;

    private Map<String, RecordType> recordTypes = new HashMap<>(); // seznam rejstříků podle code

    private Set<Integer> directNodeIds = new HashSet<>();

    /**
     * Vytvoření instance s povinnými údaji
     *
     * @param output arr_output s definicí zpracovávaného výstupu
     */
    public Output(final ArrOutput output) {
        this.outputId = output.getOutputId();
    }

    /**
     * Přidá {@link NodeId} do výstupu.
     */
    public NodeId addNodeId(final NodeId nodeId) {
        Assert.notNull(nodeId);

        NodeId nodeIdOrig = nodeIdsMap.get(nodeId.getArrNodeId());
        if (nodeIdOrig == null) {
            nodeIdsMap.put(nodeId.getArrNodeId(), nodeId);
            return nodeId;
        } else {
            return nodeIdOrig;
        }
    }

    public Node getNode(final NodeId nodeId) {
        if (nodeIdsMap.size() > 0) {
            if (iteratorNodes == null) {
                iteratorNodes = getNodesBFS();
            }
            return iteratorNodes.moveTo(nodeId);
        } else {
            Map<Integer, Node> nodeMap = outputFactoryService.loadNodes(this, Arrays.asList(nodeId));
            return nodeMap.get(nodeId.getArrNodeId());
        }
    }

    public NodeId getNodeId(final Integer nodeIdentifier) {
        return nodeIdsMap.get(nodeIdentifier);
    }

    public void linkNodeIds(final Integer parentNodeIdentifier, final Integer childNodeIdentifier) {
        final NodeId nodeIdParent = getNodeId(parentNodeIdentifier);
        final NodeId nodeIdChild = getNodeId(childNodeIdentifier);

        nodeIdChild.setParentNodeId(parentNodeIdentifier);
        nodeIdParent.getChildren().add(nodeIdChild);
    }

    /**
     * Externí počítadlo stránek pro Jasper.
     * Obchází chybu, kdy jasper nezvládá interně počítat stránky pokud detail přeteče na více stránek.
     *
     * @param increment má se při volání provést increment
     * @return aktuální hodnota (po případné inkrementaci)
     */
    public Integer getPage(final boolean increment) {
        if (increment) {
            page += 1;
        }
        return page;
    }

    /**
     * @return sečtená hodnota počtu stránek příloh pdf připojených k nodům v output.
     */
    public Integer getAttachedPages() {
        return getItemFilePdfs().stream()
                .mapToInt(ItemFile::getPagesCount)
                .sum();
    }

    /**
     * @return seznam PDF příloh připojených k nodům v output.
     */
    public List<ItemFile> getAttachements() {
        return getItemFilePdfs();
    }

    private List<ItemFile> getItemFilePdfs() {
        IteratorNodes iterator = getNodesBFS();

        List<ItemFile> result = new ArrayList<>();
        while (iterator.hasNext()) {
            Node node = iterator.next();
            List<Item> items = node.getItems();
            for (Item item : items) {
                if (item instanceof ItemFile) {
                    ItemFile itemFile = (ItemFile) item;
                    if (itemFile.getMimeType().equals(DmsService.MIME_TYPE_APPLICATION_PDF)) {
                        result.add(itemFile);
                    }
                }
            }
        }

        return result;
    }

    /**
     * @param recordProvider entita poskytující seznam recordů
     * @param code           požadovaný kód recordu, pokud je vyplněno code, bude filtrovat
     * @return seznam všech recordů
     */
    private static List<Record> getRecordsInternal(final RecordProvider recordProvider, final String code) {
        // za samotný recordProvider
        final List<Record> records = recordProvider.getRecords().stream()
                .filter(record -> (StringUtils.isBlank(code) || code.equals(record.getType().getCode()))) // pokud je vyplněno code, pak filtrovat
                .collect(Collectors.toList());

        // rekurzivně za jednotlivé podřízené recordProvider

        IteratorNodes iteratorNodes = recordProvider.getRecordProviderChildren();

        while (iteratorNodes.hasNext()) {

            Node next = iteratorNodes.next();
            List<Record> subRecords = next.getRecords().stream()
                    .filter(record -> (StringUtils.isBlank(code) || code.equals(record.getType().getCode()))) // pokud je vyplněno code, pak filtrovat
                    .collect(Collectors.toList());

            records.addAll(subRecords);
        }

        // seřadit podle názvu (record)
        return records.stream()
                .sorted((o1, o2) -> o1.getRecord().compareTo(o1.getRecord()))
                .collect(Collectors.toList());
    }

    /**
     * Metoda sahá pomocí service do DB a zafiltruje seznam přímo přiřazených nodes.
     *
     * @return seznam nodes, které jsou přímo přiřazené outputu (arr_node_output), řazeno dle pořadí ve stromu
     */
    public List<NodeId> getDirectNodes() {
        IteratorNodes iterator = getNodesDFS();
        List<NodeId> result = new ArrayList<>();
        while (iterator.hasNext()) {
            Node node = iterator.next();
            if (directNodeIds.contains(node.getArrNodeId())) {
                result.add(iterator.getActualNodeId());
            }
        }
        return result;
    }

    /** Přidá id uzlu přímo přiřazeného k výstupu. */
    public void addDirectNodeIdentifier(final Integer nodeId) {
        directNodeIds.add(nodeId);
    }

    /**
     * vstupem je seznam kódu typů atributů a vrací se seznam hodnot těchto atributů
     * řazených dle rul_desc_item.view_order + arr_item.position
     *
     * @param codes seznam požadovaných kódů itemů
     * @return seznam items s odpovídajícími kódy
     */
    public List<Item> getItems(@NotNull final Collection<String> codes) {
        Assert.notNull(codes);
        return items.stream()
                .filter(item -> codes.contains(item.getType().getCode()))
                .sorted(Item::compareToItemViewOrderPosition)
                .collect(Collectors.toList());
    }

    /**
     * Return single item
     * @param itemTypeCode Code of item
     * @return Return single item if exists. Return null if item does not exists.
     * @throws Throw exception if there are multiple items with same type.
     */
    public Item getSingleItem(final String itemTypeCode) {
        Item found = null;
        for(Item item: items)
        {
            if(itemTypeCode.equals(item.getType().getCode())) {
                // Check if item already found
                if(found!=null) {
                    throw new IllegalStateException("Multiple items with same code exists: "+itemTypeCode);
                }
                found = item;
            }
        }
        return found;
    }

    /**
     * Return value of single item
     * @param itemTypeCode Code of item
     * @return Return value of single item. Return null if item does not exists.
     * @throws Throw exception if there are multiple items with same type.
     */
    public String getSingleItemValue(final String itemTypeCode) {
        Item found = getSingleItem(itemTypeCode);
        if(found!=null) {
            return found.getSerializedValue();
        } else {
            return null;
        }
    }

    /**
     * Vstupem je seznam kódu typů atributů a vrací se seznam všech hodnot atributů výstupu kromě hodnot typů uvedených ve vstupu metody;
     * řazeno dle rul_desc_item.view_order + arr_item.position.
     *
     * @param codes seznam ignorovaných kódů itemů
     * @return seznam všech items výstupu kromě hodnot typů uvedených ve vstupu metody
     */
    public List<Item> getAllItems(@NotNull final Collection<String> codes) {
        Assert.notNull(codes);
        return items.stream()
                .filter(item -> !codes.contains(item.getType().getCode()))
                .sorted(Item::compareToItemViewOrderPosition)
                .collect(Collectors.toList());
    }


    /**
     * vrací seznam typů rejstříku, pro každý počet záznamů v něm přímo zařazených a počet záznamů včetně podřízených typů;
     * řazeno dle pořadí ve stromu typů rejstříku (zjevně dle názvu typu)
     *
     * @param withCount pouze s countRecords > 0
     * @return seznam typů rejstříku
     */
    public List<RecordType> getRecordTypes(final boolean withCount) {
        final List<Record> records = getRecordsInternal(this, null); // všechny záznamy rekurzivně
        return records.stream()
                .filter(record -> (!withCount || record.getType().getCountDirectRecords() > 0)) // zafiltrovat dle count
                .map(Record::getType) // převést na typ záznamu
                .distinct() // každý typ jen jednou
                .sorted((o1, o2) -> o1.getName().compareTo(o2.getName())) // seřadit dle zadání -> dle názvu typu
                .collect(Collectors.toList());
    }

    /**
     * vstupem je kód typu rejstříku a vrací se seznam rejstříkových hesel řazených podle názvu (record).
     *
     * @param code požadovaný kód recordu, pokud je vyplněno code, bude filtrovat
     * @return seznam všech recordů
     */
    public List<Record> getRecordsByType(final String code) {
        final List<Record> recordsInternal = getRecordsInternal(this, code);
        final List<Record> collect = recordsInternal.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return collect;
    }

    /**
     * Podobné chování jako getItems, ale pro itemy nodů, nikoliv itemy samotného output.
     * vstupem je seznam kódu typů atributů a vrací se seznam hodnot těchto atributů.
     *
     * @param codes seznam požadovaných kódů itemů
     * @return seznam všech items z nodů výstupu kromě hodnot typů uvedených ve vstupu metody
     */
    public List<Item> getNodeItems(final Collection<String> codes) {

        IteratorNodes iterator = getNodesBFS();

        List<Item> result = new ArrayList<>();
        while (iterator.hasNext()) {
            Node node = iterator.next();
            result.addAll(node.getItems(codes));
        }

        return result;
    }

    /**
     * Podobné chování jako getItems, ale pro itemy nodů, nikoliv itemy samotného output.
     * vstupem je seznam kódu typů atributů a vrací se seznam hodnot těchto atributů.
     * vrací se seznam unikátních hodnot těchto atributů
     * řazených dle rul_desc_item.view_order + arr_item.position.
     *
     * @param codes seznam požadovaných kódů itemů
     * @return seznam všech items z nodů výstupu kromě hodnot typů uvedených ve vstupu metody
     */
    public List<Item> getNodeItemsDistinct(final Collection<String> codes) {
        return getNodeItems(codes).stream()
                .distinct()
                .sorted(Item::compareToItemViewOrderPosition)
                .collect(Collectors.toList());
    }

    /**
     * @return distinct seznam Packet navázaný přes nodes
     */
    public List<Packet> getPacketItemsDistinct() {
        IteratorNodes iterator = getNodesBFS();
        Set<Packet> resultsSet = new HashSet<>();

        while (iterator.hasNext()) {
            Node node = iterator.next();
            List<Item> items = node.getItems();
            for (Item item : items) {
                if (item instanceof ItemPacketRef) {
                    resultsSet.add(item.getValue(Packet.class));
                }
            }
        }

        List<Packet> results = new ArrayList<>(resultsSet);
        results.sort(Packet::compareTo);
        return results;
    }


    /**
     * @param packet packet pro který se budou dohledávat příslušné nodes
     * @param codes kódy itemů, které se mají použít
     * @return ře1
     */
    public String getNodeItemsByPacketAsString(final Packet packet, final Collection<String> codes) {
        IteratorNodes iterator = getNodesBFS();

        Set<NodeId> nodeIds = new HashSet<>();
        while (iterator.hasNext()) {
            Node node = iterator.next();
            List<Item> items = node.getItems();
            for (Item item : items) {
                if (item instanceof ItemPacketRef) {
                    if (item.getValue(Packet.class).equals(packet) && item.getNodeId() != null) {
                        nodeIds.add(item.getNodeId());
                    }
                }
            }
        }

        Map<Integer, Node> nodes = outputFactoryService.loadNodes(this, nodeIds);

        return nodes.values().stream()
                .map(node -> node.getAllItemsAsString(codes))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(";"));
    }

    /**
     * Getter položky items
     *
     * @return seznam items
     */
    public List<Item> getItems() {
        return items;
    }

    /**
     *  @return plochý seznam Nodů seřazený dle depth, parent, position
     */
    @Override
    public IteratorNodes getNodesBFS() {
        List<NodeId> nodeIds = nodeIdsMap.values().stream()
                .sorted((o1, o2) -> new CompareToBuilder()
                        .append(o1.getDepth(), o2.getDepth())  // nejprve nejvyšší nody
                        .append(o1.getParent(), o2.getParent()) // pak sezkupit dle parenta
                        .append(o1.getPosition(), o2.getPosition()) // pak dle pořadí
                        .toComparison())
                .collect(Collectors.toList());
        return new IteratorNodes(this, nodeIds, outputFactoryService, MAX_CACHED_NODES);
    }

    /**
     * @param parent výchozí parent
     * @return plochý seznam Nodů seřazený dle prohledávání stromu nodů od root node do hloubky, vč. předaných parentů
     */
    public List<NodeId> getNodesChildsModel(final NodeId parent) {
        List<NodeId> result = new ArrayList<>();
        result.add(parent); // zařadit vlastní parent

        for (NodeId child : parent.getChildren()) {
            result.addAll(getNodesChildsModel(child));
        }

        return result;
    }

    /**
     * Jako výchozí bod vezme root node
     * @return plochý seznam Nodů seřazený dle prohledávání stromu nodů od root node do hloubky
     */
    @Override
    public IteratorNodes getNodesDFS() {
        final NodeId rootNodeId = getFund().getRootNodeId();
        final List<NodeId> nodesChildsModel = new ArrayList<>();
        nodesChildsModel.addAll(getNodesChildsModel(rootNodeId));

        return new IteratorNodes(this, nodesChildsModel, outputFactoryService, MAX_CACHED_NODES);
    }

    @Override
    public List<Record> getRecords() {
        IteratorNodes iteratorNodes = new IteratorNodes(this, new ArrayList<>(nodeIdsMap.values()), outputFactoryService, MAX_CACHED_NODES);
        if (records == null) {
            records = new ArrayList<>();
            while (iteratorNodes.hasNext()) {
                Node node = iteratorNodes.next();
                records.addAll(node.getNodeRecords());
            }
        }
        return records;
    }

    public Map<String, RecordType> getRecordTypes() {
        return recordTypes;
    }

    @Override
    public IteratorNodes getRecordProviderChildren() {
        return getNodesBFS();
    }

    public Fund getFund() {
        return fund;
    }

    public void setFund(final Fund fund) {
        this.fund = fund;
    }

    public String getInternal_code() {
        return internal_code;
    }

    public void setInternal_code(final String internal_code) {
        this.internal_code = internal_code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public int getOutputId() {
        return outputId;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(final String typeCode) {
        this.typeCode = typeCode;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(o, this);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }

    public ArrFundVersion getArrFundVersion() {
        return getFund().getArrFundVersion();
    }
}
