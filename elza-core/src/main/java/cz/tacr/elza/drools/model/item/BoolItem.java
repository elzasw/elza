package cz.tacr.elza.drools.model.item;

public class BoolItem extends AbstractItem {

    private Boolean value;

    public BoolItem(final Integer id, final String type, final String spec, final String systemCode, final Boolean value) {
        super(id, type, spec, systemCode);
        this.value = value;
    }

    public Boolean getValue() {
        return value;
    }
}
