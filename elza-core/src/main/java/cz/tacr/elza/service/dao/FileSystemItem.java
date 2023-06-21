package cz.tacr.elza.service.dao;

import java.nio.file.Path;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoPackage;

public abstract class FileSystemItem {

    protected final Path itemPath;
    protected final ArrDao virtDao = new ArrDao();
    protected final String code;

    protected FileSystemItem(final ArrDaoPackage virtPackage, final Path itemPath, final Integer daoId,
                             final String code) {
        this.itemPath = itemPath;
        this.code = code;

        virtDao.setDaoId(daoId);
        virtDao.setLabel(code);
        virtDao.setCode(code);
        virtDao.setDaoPackage(virtPackage);
    }

    public ArrDao getDao() {
        return virtDao;
    }

}
