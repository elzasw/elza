package cz.tacr.elza.print;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class Record implements Comparable<Record> {

    private final Output output; // vazba na nadřazený output

    private RecordType type;
    private String record;
    private String characteristics;
    private List<String> variantRecords = new ArrayList<>();

    public Record(@NotNull final Output output) {
        this.output = output;
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
    public Map<NodeId, Node> getNodes() {

        IteratorNodes iterator = output.getNodesBFS();

        Map<NodeId, Node> nodes = new LinkedHashMap<>();
        while (iterator.hasNext()) {
            Node node = iterator.next();
            if (node.getRecords().contains(this)) {
                NodeId nodeId = iterator.getActualNodeId();
                nodes.put(nodeId, node);
            }
        }

        return nodes;
    }

    /**
     * Serializuje seznam node navázaných na record v aktuálním outputu pomocí první nalezené hodnoty z itemů dle code.
     * Pokud není naleze žádný vyhovující item, vypíše se node.toString().
     *
     * @param codes seznam kódů možných itemů, pořadí je respektováno
     * @return seznam serializovaných node oddělěný čárkou
     */
    public String getNodesSerialized(@NotNull final Collection<String> codes) {
        List<String> result = new ArrayList<>();
        final Map<NodeId, Node> nodes = getNodes();

        for (Map.Entry<NodeId, Node> nodeIdNodeEntry : nodes.entrySet()) {

            String serializedString = "";
            for (String code : codes) {
                serializedString = nodeIdNodeEntry.getValue().getAllItemsAsString(Collections.singletonList(code));
                if (StringUtils.isNotBlank(serializedString)) {
                    break;
                }
            }
            if (StringUtils.isBlank(serializedString)) {
                serializedString = "[" + nodeIdNodeEntry.getKey().toString() + "]";
            }
            final String trim = StringUtils.trim(serializedString);
            result.add(trim);
        }


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

    public void setCharacteristics(final String characteristics) {
        this.characteristics = characteristics;
    }

    public String getRecord() {
        return record;
    }

    public void setRecord(final String record) {
        this.record = record;
    }

    public RecordType getType() {
        return type;
    }

    public void setType(final RecordType type) {
        this.type = type;
    }

    public List<String> getVariantRecords() {
        return variantRecords;
    }

    public void setVariantRecords(final List<String> variantRecords) {
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
                .append(getRecord())
                .append(getCharacteristics())
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("record", record).append("characteristics", characteristics).toString();
    }

    @Override
    public int compareTo(final Record o) {
        return CompareToBuilder.reflectionCompare(this, o);
    }
}
