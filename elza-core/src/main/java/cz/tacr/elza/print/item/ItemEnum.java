package cz.tacr.elza.print.item;

public class ItemEnum extends AbstractItem {

    @Override
    public String getSerializedValue() {
        return "";
    }

    @Override
    public boolean isValueSerializable() {
        return false;
    }

    @Override
    protected ItemSpec getValue() {
        return null;
    }
}
