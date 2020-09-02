package cz.tacr.elza.print.item;

import cz.tacr.elza.domain.ApBinding;

public class ItemRecordExtRef extends AbstractItem {

    final ApBinding apBinding;

    public ItemRecordExtRef(final ApBinding apBinding) {
        this.apBinding = apBinding;
    }

    @Override
    public String getSerializedValue() {
        return apBinding.getValue();
    }

    @Override
    protected Object getValue() {
        return apBinding;
    }

}
