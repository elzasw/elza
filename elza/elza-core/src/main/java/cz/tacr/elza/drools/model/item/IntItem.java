package cz.tacr.elza.drools.model.item;

public class IntItem extends AbstractItem {

    private Integer value;

    public IntItem(final Integer id, final String type, final String spec, final String systemCode, final Integer value) {
        super(id, type, spec, systemCode);
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
