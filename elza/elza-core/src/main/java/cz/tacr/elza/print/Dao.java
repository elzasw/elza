package cz.tacr.elza.print;

import cz.tacr.elza.domain.ArrDaoLink;

public class Dao {

    final String code;

    public Dao(ArrDaoLink daoLink) {
        code = daoLink.getDao().getCode();
    }

    public String getCode() {
        return code;
    }
}
