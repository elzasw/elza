package cz.tacr.elza.drools.model.item;

public class DoubleItem extends AbstractItem {

    private Double value;

    public DoubleItem(final Integer id, final String type, final String spec, final String systemCode, final Double value) {
        super(id, type, spec, systemCode);
        this.value = value;
    }

    public Double getValue() {
        return value;
    }
}
