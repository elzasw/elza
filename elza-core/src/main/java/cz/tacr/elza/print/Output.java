package cz.tacr.elza.print;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemFile;
import cz.tacr.elza.print.item.ItemPacketRef;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.OutputService;
import cz.tacr.elza.service.output.OutputFactoryService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.util.Assert;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Základní objekt pro generování výstupu, při tisku se vytváří 1 instance.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 21.6.16
 */
@Scope("prototype")
public class Output implements RecordProvider {

    private static final int MAX_CACHED_NODES = 100; // maximální počet nodů v cache

    private final ArrOutput arrOutput; // interní záležitost - vazba na původní objekt
    private final Integer outputId; // ID pro vazbu do DB na entitu arr_output

    @Autowired
    private OutputService outputService; // interní vazba na service

    @Autowired
    private OutputFactoryService outputFactoryService;

    private String internal_code;
    private String name;
    private String type;
    private String typeCode;
    private Fund fund;
    private LoadingCache<Integer, Node> nodeCache;

    private Integer page = 0;

    // seznam všech atributů outputu
    private List<Item> items = new ArrayList<>();

    // seznam všech node outputu (přímo přiřazené + jejich potomci + nadřízení až do root);
    // mapa má jako klíč ID Nodu odpovídající ArrNode.arrNodeId
    private Map<Integer, NodeId> nodesMap = new HashMap<>();

    // seznam rejstříkových hesel všech nodes outputu odkazovaných přes arr_node_register
    private List<Record> records = new ArrayList<>();

    private Map<String, RecordType> recordTypes = new HashMap<>(); // seznam rejstříků podle code

    /**
     * Vytvoření instance s povinnými údaji
     *
     * @param output arr_output s definicí zpracovávaného výstupu
     */
    public Output(ArrOutput output) {
        this.outputId = output.getOutputId();
        this.arrOutput = output;

        final Output out = this;

        nodeCache = CacheBuilder.newBuilder()
                .maximumSize(MAX_CACHED_NODES)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build(new CacheLoader<Integer, Node>() {
                    @Override
                    public Node load(Integer key) throws Exception {
                        return outputFactoryService.getNode(out.getNodesMap().get(key), out);
                    }
                });
    }

    public LoadingCache<Integer, Node> getNodeCache() {
        return nodeCache;
    }

    /**
     * Externí počítadlo stránek pro Jasper.
     * Obchází chybu, kdy jasper nezvládá interně počítat stránky pokud detail přeteče na více stránek.
     *
     * @param increment má se při volání provést increment
     * @return aktuální hodnota (po případné inkrementaci)
     */
    public Integer getPage(boolean increment) {
        if (increment) {
            page += 1;
        }
        return page;
    }

    /**
     * @return sečtená hodnota počtu stránek příloh pdf připojených k nodům v output.
     */
    public Integer getAttachedPages() {
        return getItemFilePdfsStream()
                .mapToInt(ItemFile::getPagesCount)
                .sum();
    }

    /**
     * @return seznam PDF příloh připojených k nodům v output.
     */
    public List<ItemFile> getAttachements() {
        return getItemFilePdfsStream()
//                .map(AbstractItem::getValue)
                .collect(Collectors.toList());
    }

    private Stream<ItemFile> getItemFilePdfsStream() {
        return getNodesFlatModel().stream()
                .flatMap(nodeId -> nodeId.getNode().getAllItems(new ArrayList<>()).stream())
                .filter(item -> item instanceof ItemFile)
                .map(item -> (ItemFile) item)
                .filter(itemFile -> itemFile.getMimeType().equals(DmsService.MIME_TYPE_APPLICATION_PDF));
    }

    /**
     * @param recordProvider entita poskytující seznam recordů
     * @param code           požadovaný kód recordu, pokud je vyplněno code, bude filtrovat
     * @return seznam všech recordů
     */
    private static List<Record> getRecordsInternal(final RecordProvider recordProvider, final String code) {
        // za samotný recordProvider
        final List<Record> records = recordProvider.getRecords().stream()
                .filter(record -> (!StringUtils.isNotBlank(code) || code.equals(record.getType().getCode()))) // pokud je vyplněno code, pak filtrovat
                .collect(Collectors.toList());

        // rekurzivně za jednotlivé podřízené recordProvider
        for (RecordProvider provider : recordProvider.getRecordProviderChildern()) {
            records.addAll(getRecordsInternal(provider, code));
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
        // Načíst seznam nodes z DB
        final List<ArrNode> nodesForOutput = outputService.getNodesForOutput(arrOutput);

        // zafiltrovat seznam všech vazeb dle seznamu se zachováním pořadí
        Set<Integer> nodesForOutputId = nodesForOutput.parallelStream().map(ArrNode::getNodeId).collect(Collectors.toSet());
        return getNodesChildsModel().stream().filter(node -> nodesForOutputId.contains(node.getArrNodeId())).collect(Collectors.toList());
    }

    /**
     * vstupem je seznam kódu typů atributů a vrací se seznam hodnot těchto atributů
     * řazených dle rul_desc_item.view_order + arr_item.position
     *
     * @param codes seznam požadovaných kódů itemů
     * @return seznam items s odpovídajícími kódy
     */
    public List<Item> getItems(@NotNull Collection<String> codes) {
        Assert.notNull(codes);
        return items.stream()
                .filter(item -> codes.contains(item.getType().getCode()))
                .sorted(Item::compareToItemViewOrderPosition)
                .collect(Collectors.toList());
    }

    /**
     * Vstupem je seznam kódu typů atributů a vrací se seznam všech hodnot atributů výstupu kromě hodnot typů uvedených ve vstupu metody;
     * řazeno dle rul_desc_item.view_order + arr_item.position.
     *
     * @param codes seznam ignorovaných kódů itemů
     * @return seznam všech items výstupu kromě hodnot typů uvedených ve vstupu metody
     */
    public List<Item> getAllItems(@NotNull Collection<String> codes) {
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
    public List<RecordType> getRecordTypes(boolean withCount) {
        final List<Record> records = getRecordsInternal(this, null); // všechny záznamy rekurzivně
        return records.stream()
                .filter(record -> (!withCount || ((record.getType().getCountDirectRecords() != null) && (record.getType().getCountDirectRecords() > 0)))) // zafiltrovat dle count
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
    public List<Record> getRecordsByType(String code) {
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
    public List<Item> getNodeItems(Collection<String> codes) {
        return getNodesFlatModel().stream()
                .flatMap(nodeId -> nodeId.getNode().getItems(codes).stream())
                .collect(Collectors.toList());
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
    public List<Item> getNodeItemsDistinct(Collection<String> codes) {
        return getNodeItems(codes).stream()
                .distinct()
                .sorted(Item::compareToItemViewOrderPosition)
                .collect(Collectors.toList());
    }

    /**
     * @return distinct seznam Packet navázaný přes nodes
     */
    public List<Packet> getPacketItemsDistinct() {
        return getNodesFlatModel().stream()
                .flatMap(nodeId -> nodeId.getNode().getAllItems(new ArrayList<>()).stream())
                .filter(item -> item instanceof ItemPacketRef)
                .map(item -> (ItemPacketRef) item)
                .map(ItemPacketRef::getValue)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }


    /**
     * @param packet packet pro který se budou dohledávat příslušné nodes
     * @param codes kódy itemů, které se mají použít
     * @return ře1
     */
    public String getNodeItemsByPacketAsString(Packet packet, Collection<String> codes) {
        return getNodesFlatModel().stream()
                .flatMap(nodeId -> nodeId.getNode().getAllItems(new ArrayList<>()).stream())
                .filter(item -> item instanceof ItemPacketRef)
                .map(item -> (ItemPacketRef) item)
                .filter(itemPacketRef -> itemPacketRef.getValue().equals(packet))
                .map(AbstractItem::getNodeId)
                .filter(node -> node != null)
                .distinct()
                .map(nodeId1 -> nodeId1.getNode().getAllItemsAsString(codes))
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
    public List<NodeId> getNodesFlatModel() {
        return nodesMap.values().stream()
                .sorted((o1, o2) -> new CompareToBuilder()
                        .append(o1.getDepth(), o2.getDepth())  // nejprve nejvyšší nody
                        .append(o1.getParent(), o2.getParent()) // pak sezkupit dle parenta
                        .append(o1.getPosition(), o2.getPosition()) // pak dle pořadí
                        .toComparison())
                .collect(Collectors.toList());
    }

    /**
     * @param parent výchozí parent
     * @return plochý seznam Nodů seřazený dle prohledávání stromu nodů od root node do hloubky, vč. předaných parentů
     */
    public List<NodeId> getNodesChildsModel(NodeId parent) {
        List<NodeId> result = new ArrayList<>();
        result.add(parent); // zařadit vlastní parent

        final Set<NodeId> children = parent.getChildren();
        children.stream()
                .sorted((o1, o2) -> new CompareToBuilder()
                        .append(o1.getPosition(), o2.getPosition())
                        .toComparison())
                .forEach(nodeId -> result.addAll(getNodesChildsModel(nodeId)));

        return result;
    }

    /**
     * Jako výchozí bod vezme root node
     * @return plochý seznam Nodů seřazený dle prohledávání stromu nodů od root node do hloubky
     */
    public List<NodeId> getNodesChildsModel() {
        final NodeId rootNodeId = getFund().getRootNodeId();
        final List<NodeId> nodesChildsModel = new ArrayList<>();
        nodesChildsModel.addAll(getNodesChildsModel(rootNodeId));
        return nodesChildsModel;
    }

    public Map<Integer, NodeId> getNodesMap() {
        return nodesMap;
    }

    public List<Record> getRecords() {
        return records;
    }

    public Map<String, RecordType> getRecordTypes() {
        return recordTypes;
    }

    @Override
    public List<NodeId> getRecordProviderChildern() {
        return getNodesFlatModel();
    }

    public Fund getFund() {
        return fund;
    }

    public void setFund(Fund fund) {
        this.fund = fund;
    }

    public String getInternal_code() {
        return internal_code;
    }

    public void setInternal_code(String internal_code) {
        this.internal_code = internal_code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getOutputId() {
        return outputId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
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

    Integer getArrFundVersionId() {
        return getFund().getArrFundVersion().getFundVersionId();
    }
}
