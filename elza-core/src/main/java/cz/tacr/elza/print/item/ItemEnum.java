package cz.tacr.elza.print.item;

/**
 * Enumerated Item for outputs
 * 
 * This type is without value
 */
public class ItemEnum extends AbstractItem {

    private ItemEnum() {
        super();
    }

    @Override
    public String serializeValue() {
        return "";
    }

    @Override
    public String serialize() {
        return serializeValue();
    }
    
    @Override
    public Object getValue() {
    	return "";
    }

	@Override
	public boolean isEmpty() {
		// Item without specification is considered empty
		ItemSpec itemSpec = this.getSpecification();
		return itemSpec==null;
	}

	public static ItemEnum newInstance() {
		return new ItemEnum();
	}

}
