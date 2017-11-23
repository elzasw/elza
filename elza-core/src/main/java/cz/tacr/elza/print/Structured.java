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
 * Structured
 * @Since  16.11.2017
 */
public class Structured implements Comparable<Structured> {

    private String value;
    private Set<Node> nodes;

    /**
     * Metoda pro získání hodnoty do fieldu v Jasper.
     * Umožní na položce v detailu volat metody sám nad sebou (nejen implicitně zpřístupněné gettery).
     *
     * @return odkaz sám na sebe
     */
    public Structured getStructured() {
        return this;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
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
    public int compareTo(final Structured o) {
        return CompareToBuilder.reflectionCompare(this, o);
    }
}
