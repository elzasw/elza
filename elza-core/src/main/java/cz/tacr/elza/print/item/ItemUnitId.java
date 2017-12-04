package cz.tacr.elza.print.item;

/**
 * Unit id
 *
 */
public class ItemUnitId extends AbstractItem {

    private final String value;

    public ItemUnitId(final String value) {
        this.value = value;
    }

    @Override
    public String getSerializedValue() {
        return value;
    }

    @Override
    protected String getValue() {
        return value;
    }
}
