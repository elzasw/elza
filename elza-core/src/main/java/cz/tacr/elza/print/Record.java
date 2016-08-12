package cz.tacr.elza.print;

import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.print.item.ItemRecordRef;
import cz.tacr.elza.service.OutputService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
@Scope("prototype")
public class Record implements Comparable<Record> {
    private final Output output; // vazba na nadřazený output
    private final NodeId nodeId; // vazba na node, může být null, v takovém případě patří přímo k output
    private final RegRecord regRecord;
    private ItemRecordRef item; // vazba na item, může být null

    @Autowired
    private OutputService outputService; // interní vazba na service

    private RecordType type;
    private String record;
    private String characteristics;
    private List<String> variantRecords = new ArrayList<>();

    public Record(@NotNull Output output, NodeId nodeId, @NotNull RegRecord regRecord) {
        this.output = output;
        this.nodeId = nodeId;
        this.regRecord = regRecord;
    }

    /**
     * Metoda pro získání hodnoty do fieldu v Jasper.
     * Umožní na položce v detailu volat metody sám nad sebou (nejen implicitně zpřístupněné gettery).
     *
     * @return odkaz sám na sebe
     */
    public Record getRecordVo() {
        return this;
    }

    // vrací seznam Node přiřazených přes vazbu arr_node_register
    List<NodeId> getNodes() {
        return output.getNodesFlatModel().stream()
                .filter(node -> node.getRecords().contains(this))
                .collect(Collectors.toList());
    }

    /**
     * Serializuje seznam node navázaných na record v aktuálním outputu pomocí první nalezené hodnoty z itemů dle code.
     * Pokud není naleze žádný vyhovující item, vypíše se node.toString().
     *
     * @param codes seznam kódů možných itemů, pořadí je respektováno
     * @return seznam serializovaných node oddělěný čárkou
     */
    public String getNodesSerialized(@NotNull Collection<String> codes) {
        List<String> result = new ArrayList<>();
        final List<NodeId> nodeIds = getNodes().stream().distinct().collect(Collectors.toList());

        nodeIds.stream().forEach(nodeId -> {
            String serializedString = "";
            for (String code : codes) {
                serializedString = nodeId.getNode().getAllItemsAsString(Collections.singletonList(code));
                if (StringUtils.isNotBlank(serializedString)) {
                    break;
                }
            }
            if (StringUtils.isBlank(serializedString)) {
                serializedString = "[" + nodeId.toString() + "]";
            }
            final String trim = StringUtils.trim(serializedString);
            result.add(trim);
        });

        return StringUtils.join(result, ", ");
    }

    /**
     * Fieldy spojí v uvedeném pořadí "record characteristics" a oddělí mezerou, zohledňuje pouze vyplněné položky.
     *
     * @return hodnota formátovaná jako text k tisku
     */
    public String serialize() {
        return (StringUtils.defaultString(record) + " " + StringUtils.defaultString(characteristics)).trim();
    }

    public String getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(String characteristics) {
        this.characteristics = characteristics;
    }

    public String getRecord() {
        return record;
    }

    public void setRecord(String record) {
        this.record = record;
    }

    public RecordType getType() {
        return type;
    }

    public void setType(RecordType type) {
        this.type = type;
    }

    public List<String> getVariantRecords() {
        return variantRecords;
    }

    public void setVariantRecords(List<String> variantRecords) {
        this.variantRecords = variantRecords;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof Record) {
            Record other = (Record) o;
            return new EqualsBuilder()
                    .append(getRecord(), other.getRecord())
                    .append(getCharacteristics(), other.getCharacteristics())
                    .isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
//                .append(getType())
                .append(getRecord())
                .append(getCharacteristics())
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("record", record).append("characteristics", characteristics).toString();
    }

    public ItemRecordRef getItem() {
        return item;
    }

    public void setItem(ItemRecordRef item) {
        this.item = item;
    }

    @Override
    public int compareTo(Record o) {
        return CompareToBuilder.reflectionCompare(this, o);
    }
}
