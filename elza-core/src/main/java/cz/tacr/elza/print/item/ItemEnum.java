package cz.tacr.elza.print.item;

/**
 * Enumerated Item for outputs
 *
 * This type is without value
 */
public class ItemEnum extends AbstractItem {

    @Override
    public String getSerializedValue() {
        return "";
    }

    @Override
    protected String getValue() {
        return "";
    }

    @Override
    public boolean isEmpty() {
        // Item without specification is considered empty
        ItemSpec spec = this.getSpecification();
        return spec == null;
    }
}
