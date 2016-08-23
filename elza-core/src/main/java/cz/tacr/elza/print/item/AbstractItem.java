package cz.tacr.elza.print.item;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import cz.tacr.elza.print.NodeId;

/**
 * Abstraktní základ se společnými metodami pro Items.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public abstract class AbstractItem<T> implements Item<T> {
    private final NodeId nodeId; // vazba na node, může být null, v takovém případě patří přímo k output

    private ItemType type;
    private ItemSpec specification;
    private Integer position;
    private T value;

    protected AbstractItem(final NodeId nodeId, final T value) {
        this.nodeId = nodeId;
        this.value = value;
    }

    @Override
    public Item<T> getItem() {
        return this;
    }

    @Override
    public Integer getPosition() {
        return position;
    }

    public void setPosition(final Integer position) {
        this.position = position;
    }

    @Override
    public int compareToItemViewOrderPosition(final Item o) {
        return new CompareToBuilder()
                .append(type.getViewOrder(), o.getType().getViewOrder())
                .append(position, o.getPosition())
                .toComparison();
    }

    @Override
    public NodeId getNodeId() {
        return nodeId;
    }

    @Override
    public ItemSpec getSpecification() {
        return specification;
    }

    public void setSpecification(final ItemSpec specification) {
        this.specification = specification;
    }

    @Override
    public ItemType getType() {
        return type;
    }

    public void setType(final ItemType type) {
        this.type = type;
    }

    @Override
    public T getValue() {
        return value;
    }

    public void setValue(final T value) {
        this.value = value;
    }

    @Override
    public String serialize() {
        final String s = getType().getName();
        return s + ": " + serializeValue();
    }

    @Override
    public String getSerialized() {
        return serialize();
    }

    @Override
    public String getSerializedValue() {
        return serializeValue();
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
