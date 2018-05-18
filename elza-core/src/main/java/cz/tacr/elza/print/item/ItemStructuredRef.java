package cz.tacr.elza.print.item;

import cz.tacr.elza.print.Structured;

/**
 * @since 16.11.2017
 */
public class ItemStructuredRef extends AbstractItem {

	Structured structured;

    public ItemStructuredRef(final Structured structured) {
        super();
        this.structured = structured;
    }

    @Override
    public String getSerializedValue() {
        return structured.getValue();
    }

    @Override
    public Structured getValue() {
    	return structured;
    }

}
