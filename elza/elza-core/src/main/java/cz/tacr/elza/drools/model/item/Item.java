package cz.tacr.elza.drools.model.item;

public class Item extends AbstractItem {

    private String value;

    public Item(final Integer id, final String type, final String spec, final String systemCode, final String value) {
        super(id, type, spec, systemCode);
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
