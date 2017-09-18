package cz.tacr.elza.print.item;


/**
 * Integer item for print
 */
public class ItemInteger extends AbstractItem {
	
	Integer value;

    public ItemInteger(final Integer value) {
        super();
        this.value = value;
    }

    @Override
    public String serializeValue() {
        return value.toString();
    }

	@Override
	public Object getValue() {
		return value;
	}
}
