package cz.tacr.elza.print;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDao.DaoType;
import cz.tacr.elza.domain.ArrDaoLink;

public class Dao {

    final String code;

    final DaoType daoType;

    final String label;

    public Dao(ArrDaoLink daoLink) {
        ArrDao dao = daoLink.getDao();
        code = dao.getCode();
        daoType = dao.getDaoType();
        label = dao.getLabel();
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
