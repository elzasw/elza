package cz.tacr.elza.print;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDao.DaoType;
import cz.tacr.elza.domain.ArrFsLink;
import cz.tacr.elza.domain.ArrLegacyDaoLink;

public class Dao {

    final String code;

    final DaoType daoType;

    final String label;

    public Dao(ArrLegacyDaoLink daoLink) {
        ArrDao dao = daoLink.getDao();
        code = dao.getCode();
        daoType = dao.getDaoType();
        label = dao.getLabel();
    }

    public Dao(ArrFsLink fsLink) {
        // filesystem link: the repository-relative path identifies the object
        code = fsLink.getPath();
        daoType = DaoType.ATTACHMENT;
        label = fsLink.getPath();
    }

    public String getCode() {
        return code;
    }

    public boolean isAttachment() {
        return ArrDao.DaoType.ATTACHMENT.equals(daoType);
    }

    public String getLabel() {
        return (label == null ? "" : label);
    }
}
