package cz.tacr.elza.print.item;

import cz.tacr.elza.print.File;

public class ItemFileRef extends AbstractItem {

    private File file;

    public ItemFileRef(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    @Override
    public Object getValue() {
        return file;
    }

    @Override
    public String serializeValue() {
        return file.getName();
    }
}
