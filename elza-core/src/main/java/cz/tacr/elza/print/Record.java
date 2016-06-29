package cz.tacr.elza.print;

import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RegRecord;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// TODO - JavaDoc - Lebeda

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class Record {
    private final Output output; // vazba na nadřazený output
    private final Node node; // vazba na node, může být null, v takovém případě patří přímo k output

    private final RegRecord regRecord;

    private RecordType type;
    private String record;
    private String characteristics;
    private List<String> variantRecords = new ArrayList<>();

    public Record(@NotNull Output output, Node node, @NotNull RegRecord regRecord) {
        this.output = output;
        this.node = node;
        this.regRecord = regRecord;
    }

    // vrací seznam Node přiřazených přes vazbu arr_node_register
    List<Node> getNodes() {
        final List<ArrNode> nodesByRegister = output.getOutputService().getNodesByRegister(regRecord);
        return output.getNodes().stream()
                .filter(node -> nodesByRegister.contains(node.getArrNode()))
                .collect(Collectors.toList());
    }

    // TODO - JavaDoc - Lebeda
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
