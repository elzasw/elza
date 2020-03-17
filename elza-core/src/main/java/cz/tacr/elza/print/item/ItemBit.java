package cz.tacr.elza.print.item;

public class ItemBit extends AbstractItem {

    private final Boolean value;

    public ItemBit(Boolean value) {
        this.value = value;
    }

    @Override
    public Boolean getValue() {
        return value;
    }


    @Override
    public String getSerializedValue() {
        return Boolean.toString(value);
    }
}
