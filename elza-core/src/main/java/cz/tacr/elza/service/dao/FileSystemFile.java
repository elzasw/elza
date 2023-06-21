package cz.tacr.elza.service.dao;

import java.nio.file.Path;

import cz.tacr.elza.domain.ArrDaoFile;
import cz.tacr.elza.domain.ArrDaoPackage;

public class FileSystemFile extends FileSystemItem {
    private ArrDaoFile virtFile;

    public FileSystemFile(final ArrDaoPackage virtPackage, final Path itemPath, Integer daoId, final String code) {
        super(virtPackage, itemPath, daoId, code);

        virtFile = new ArrDaoFile();
        virtFile.setDaoFileId(daoId);
        virtFile.setDao(virtDao);
        virtFile.setFileName(code);
    }
}
