package cz.tacr.elza.print;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringJoiner;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author Martin Lebeda
 * @author Petr Pytelka
 * @Since  Date: 22.6.16
 */
public class Packet implements Comparable<Packet> {

    private String type;
    private String typeCode;
    private String typeShortcut;
    private String storageNumber;
    private String state;
    private Set<Node> nodes;

    /**
     * Metoda pro získání hodnoty do fieldu v Jasper.
     * Umožní na položce v detailu volat metody sám nad sebou (nejen implicitně zpřístupněné gettery).
     *
     * @return odkaz sám na sebe
     */
    public Packet getPacket() {
        return this;
    }

    /**
     * Return formatted packet name
     *
     * Function return typeShortcut and storageNumber
     * @return Return formatted value
     */
    public String formatAsString() {
        StringJoiner sj = new StringJoiner(" ");
        if (StringUtils.isNotBlank(typeShortcut)) {
            sj.add(typeShortcut);
        }
        if (StringUtils.isNotBlank(storageNumber)) {
            sj.add(storageNumber);
        }
        return sj.toString();
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public String getStorageNumber() {
        return storageNumber;
    }

    public void setStorageNumber(final String storageNumber) {
        this.storageNumber = storageNumber;
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

    public String getTypeShortcut() {
        return typeShortcut;
    }

    public void setTypeShortcut(final String typeShortcut) {
        this.typeShortcut = typeShortcut;
    }

    public Set<Node> getNodes() {
        if (nodes == null) {
            return Collections.emptySet();
        }
        return nodes;
    }

    public void setNodes(final Set<Node> nodes) {
        this.nodes = nodes;
    }

    public void addNode(final Node node) {
        if (nodes == null) {
            nodes = new LinkedHashSet<>();
        }
        nodes.add(node);
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

    @Override
    public int compareTo(final Packet o) {
        return CompareToBuilder.reflectionCompare(this, o);
    }
}
