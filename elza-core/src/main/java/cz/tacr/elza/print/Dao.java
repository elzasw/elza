package cz.tacr.elza.print;

import cz.tacr.elza.domain.ArrDaLink;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDao.DaoType;
import cz.tacr.elza.domain.ArrFsLink;
import cz.tacr.elza.domain.ArrLegacyDaoLink;

public class Dao {

    final String code;

    final DaoType daoType;

    final String label;

    /**
     * Code of the AIP in the digital archive, set only for a link to the digital archive.
     */
    final String aipCode;

    public Dao(ArrLegacyDaoLink daoLink) {
        ArrDao dao = daoLink.getDao();
        code = dao.getCode();
        daoType = dao.getDaoType();
        label = dao.getLabel();
        aipCode = null;
    }

    public Dao(ArrFsLink fsLink) {
        // filesystem link: the repository-relative path identifies the object
        code = fsLink.getPath();
        daoType = DaoType.ATTACHMENT;
        label = fsLink.getPath();
        aipCode = null;
    }

    /**
     * Link to the digital archive: the code is the METS ID of the linked part of the AIP, or the
     * AIP code when the whole AIP is linked.
     */
    public Dao(ArrDaLink daLink) {
        aipCode = daLink.getAip().getCode();
        code = daLink.getDaDao() != null ? daLink.getDaDao().getCode() : aipCode;
        daoType = DaoType.LEVEL;
        label = null;
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

    /**
     * @return code of the AIP for a link to the digital archive, otherwise null
     */
    public String getAipCode() {
        return aipCode;
    }
}
