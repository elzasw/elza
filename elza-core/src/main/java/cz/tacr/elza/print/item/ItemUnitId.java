package cz.tacr.elza.print.item;

/**
 * Unit id
 * 
 */
public class ItemUnitId extends AbstractItem {
	
	String value;

    public ItemUnitId(final String value) {
        super();
        
        this.value = value;
    }

    @Override
    public String serializeValue() {
        return value;
    }

	@Override
	public Object getValue() {
		return value;
	}
}
