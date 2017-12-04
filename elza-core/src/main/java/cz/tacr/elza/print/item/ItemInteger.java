package cz.tacr.elza.print.item;

/**
 * Integer item for print
 */
public class ItemInteger extends AbstractItem {

    private final Integer value;

    public ItemInteger(final Integer value) {
        this.value = value;
    }

    @Override
    public String getSerializedValue() {
        return value.toString();
    }

    @Override
    protected Integer getValue() {
        return value;
    }
}
