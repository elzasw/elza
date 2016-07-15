package cz.tacr.elza.print.item;

import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.ItemSpec;
import cz.tacr.elza.print.ItemType;
import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.Output;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.validation.constraints.NotNull;

/**
 * Abstraktní základ se společnými metodami pro Items.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public abstract class AbstractItem<T> implements Item<T> {
    private final ArrItem arrItem; // odkaz na zdrojovou položku
    private final Output output; // vazba na output
    private final Node node; // vazba na node, může být null, v takovém případě patří přímo k output

    private ItemType type;
    private ItemSpec specification;
    private Integer position;
    private T value;

    protected AbstractItem(@NotNull ArrItem arrItem, @NotNull Output output, Node node) {
        this.arrItem = arrItem;
        this.output = output;
        this.node = node;
    }

    @Override
    public Item<T> getItem() {
        return this;
    }

    @Override
    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    @Override
    public int compareToItemViewOrderPosition(Item o) {
        return new CompareToBuilder()
                .append(getArrItem().getItemType().getViewOrder(), o.getArrItem().getItemType().getViewOrder())
                .append(getArrItem().getPosition(), o.getArrItem().getPosition())
                .toComparison();
    }

    @Override
    public ArrItem getArrItem() {
        return arrItem;
    }

    public Node getNode() {
        return node;
    }

    public Output getOutput() {
        return output;
    }

    @Override
    public ItemSpec getSpecification() {
        return specification;
    }

    public void setSpecification(ItemSpec specification) {
        this.specification = specification;
    }

    @Override
    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    @Override
    public T getValue() {
        return value;
    }

    public void setValue(T value) {
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
