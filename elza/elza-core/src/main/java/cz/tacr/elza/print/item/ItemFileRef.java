package cz.tacr.elza.print.item;

import cz.tacr.elza.print.File;

public class ItemFileRef extends AbstractItem {

    private final File file;
    private final String name;

    public ItemFileRef(File file, String name) {
        this.file = file;
        this.name = name;
    }

    @Override
    protected File getValue() {
        return file;
    }

    @Override
    public String getSerializedValue() {
        return file.getName();
    }

    public String getName() {
        return name;
    }
}
