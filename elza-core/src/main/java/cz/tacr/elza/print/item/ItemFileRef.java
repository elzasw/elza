package cz.tacr.elza.print.item;

import cz.tacr.elza.print.File;

public class ItemFileRef extends AbstractItem {

    private final File file;

    public ItemFileRef(File file) {
        this.file = file;
    }

    @Override
    protected File getValue() {
        return file;
    }

    @Override
    public boolean isValueSerializable() {
        return true;
    }

    @Override
    public String getSerializedValue() {
        return file.getName();
    }
}
