package cz.tacr.elza.service.dao;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.domain.ArrDaoPackage;

public class FileSystemFolder extends FileSystemItem {
    private List<FileSystemItem> subItems = new ArrayList<>();

    public FileSystemFolder(final ArrDaoPackage virtPackage, final Path itemPath, Integer daoId, final String code) {
        super(virtPackage, itemPath, daoId, code);
    }

    public void addItem(FileSystemItem item) {
        subItems.add(item);
    }

    public List<? extends FileSystemItem> getItems() {
        return subItems;
    }
}
