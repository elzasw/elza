package cz.tacr.elza.print.item;

import org.apache.commons.lang3.StringUtils;

/**
 * Abstract description item implementation.
 */
public abstract class AbstractItem implements Item {

    private ItemType type;

    private ItemSpec specification;

    private int position;

    private boolean undefined;

    @Override
    public ItemType getType() {
        return type;
    }

    public void setType(final ItemType type) {
        this.type = type;
    }

    @Override
    public ItemSpec getSpecification() {
        return specification;
    }

    public void setSpecification(final ItemSpec specification) {
        this.specification = specification;
    }

    @Override
    public int getPosition() {
        return position;
    }

    public void setPosition(final Integer position) {
        this.position = position;
    }

    @Override
    public boolean isUndefined() {
        return undefined;
    }

    public void setUndefined(final Boolean undefined) {
        this.undefined = undefined;
    }

    @Override
    public <T> T getValue(final Class<T> type) {
        return type.cast(getValue());
    }

    @Override
    public String getSerialized() {
        String typeName = getType().getName();
        return typeName + ": " + getSerializedValue();
    }

    @Override
    public boolean isEmpty() {
        String value = getSerializedValue();
        return StringUtils.isEmpty(value);
    }

    @Override
    public final int compareTo(Item o) {
        int result = type.getViewOrder().compareTo(o.getType().getViewOrder());
        if (result != 0) {
            return result;
        }
        return Integer.compare(position, o.getPosition());
    }

    /**
     * Method to return real value object
     *
     * @return return value object
     */
    protected abstract Object getValue();
}
