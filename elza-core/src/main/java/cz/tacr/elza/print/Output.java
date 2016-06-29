package cz.tacr.elza.print;

import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.service.OutputService;
import org.apache.commons.lang.StringUtils;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * Základní objekt pro generování výstupu, při tisku se vytváří 1 instance.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 21.6.16
 */
@Scope("prototype")
public class Output implements RecordProvider {

    @Autowired
    private OutputService outputService; // interní vazba na service

    // review Lebeda - není lepší držet jen objekt místo ID???
    private final ArrOutput arrOutput; //
    private final Integer outputId; // ID pro vazbu do DB na entitu arr_output

    private String internal_code;
    private String name;
    private String type;
    private String typeCode;
    private Fund fund;

    // seznam všech atributů outputu
    private List<Item> items = new ArrayList<>();

    // seznam všech node outputu (přímo přiřazené + jejich potomci + nadřízení až do root); seřazeno dle pořadí ve stromu
    private List<Node> nodes = new ArrayList<>();

    // seznam rejstříkových hesel všech nodes outputu odkazovaných přes arr_node_register
    private List<Record> records = new ArrayList<>();

    /**
     * Vytvoření instance s povinnými údaji
     *
     * @param output        arr_output s definicí zpracovávaného výstupu
     */
    public Output(ArrOutput output) {
        this.outputId = output.getOutputId();
        this.arrOutput = output;
    }

    // TODO - JavaDoc - Lebeda
    private static List<Record> getRecordsInternal(final RecordProvider recordProvider, final String code) {
        // za samotný output
        final List<Record> records = recordProvider.getRecords().stream()
                .filter(record -> (!StringUtils.isNotBlank(code) || code.equals(record.getType().getCode()))) // pokud je vyplněno code, pak filtrovat
                .collect(Collectors.toList());

        // rekurzivně za jednotlivé podřízené node
        for (RecordProvider provider : recordProvider.getRecordProviderChildern()) {
            records.addAll(getRecordsInternal(provider, code));
        }

        // seřadit podle názvu (record)
        return records.stream().sorted((o1, o2) -> o1.getRecord().compareTo(o1.getRecord())).collect(Collectors.toList());
    }

    /**
     * Metoda sahá pomocí service do DB a zafiltruje seznam přímo přiřazených nodes.
     *
     * @return seznam nodes, které jsou přímo přiřazené outputu (arr_node_output), řazeno dle pořadí ve stromu
     */
    public List<Node> getDirectNodes() {
        // Načíst seznam nodes z DB
        final List<ArrNode> nodesForOutput = outputService.getNodesForOutput(arrOutput);

        // zafiltrovat seznam všech vazeb dle seznamu se zachováním pořadí
        return nodes.stream().filter(node -> node.getArrNode().equals(nodesForOutput)).collect(Collectors.toList());
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

    // TODO - JavaDoc - Lebeda
    // vstupem je seznam kódu typů atributů a vrací se seznam všech hodnot atributů výstupu kromě hodnot typů uvedených ve vstupu metody;
    // řazeno dle rul_desc_item.view_order + arr_item.position
    public List<Item> getAllItems(@NotNull Collection<String> codes) {
        Assert.notNull(codes);
        return items.stream()
                .filter(item -> !codes.contains(item.getType().getCode()))
                .sorted(Item::compareToItemViewOrderPosition)
                .collect(Collectors.toList());
    }

    // TODO - JavaDoc - Lebeda
    // vrací seznam typů rejstříku, pro každý počet záznamů v něm přímo zařazených a počet záznamů včetně podřízených typů;
    // řazeno dle pořadí ve stromu typů rejstříku (zjevně dle názvu typu)
    public List<RecordType> getRecordTypes(boolean withCount) {
        final List<Record> records = getRecordsInternal(this, null); // všechny záznamy rekurzivně
        return records.stream()
                .filter(record -> (!withCount || (record.getType().getCountRecords() != null) && (record.getType().getCountRecords() > 0))) // zafiltrovat dle count
                .map(Record::getType) // převést na typ záznamu
                .sorted((o1, o2) -> o1.getName().compareTo(o2.getName())) // seřadit dle zadání -> dle názvu typu
                .collect(Collectors.toList());
    }

    // TODO - JavaDoc - Lebeda
    // vstupem je kód typu rejstříku a vrací se seznam rejstříkových hesel řazených podle názvu (record)
    public List<Record> getRecordsByType(String code) {
        return getRecordsInternal(this, code);

    }

    // TODO - JavaDoc - Lebeda
    // vstupem je seznam kódu typů atributů a vrací se seznam hodnot těchto atributů řazených dle ???
    public List<Item> getNodeItems(Collection<String> codes) {
        return null;  // TODO Lebeda - ?? implementovat
    }

    // TODO - JavaDoc - Lebeda
    // vstupem je seznam kódu typů atributů a vrací se seznam unikátních hodnot těchto atributů řazených dle ???
    public List<Item> getNodeItemsDistinct(Collection<String> codes) {
        return null;  // TODO Lebeda - ?? implementovat
    }

    /**
     * Getter položky items
     *
     * @return seznam items
     */
    public List<Item> getItems() {
        return items;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public List<Record> getRecords() {
        return records;
    }

    @Override
    public List<Node> getRecordProviderChildern() {
        return nodes;
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

}
