package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ArrDaoLinkRequest;
import cz.tacr.elza.domain.ArrDaoRequest;

/**
 * Value objekt {@link ArrDaoLinkRequest}
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
public class ArrDaoLinkRequestVO extends ArrRequestVO {

    private ArrDaoVO dao;

    private ArrDaoRequest.Type type;

    private String didCode;

    public ArrDaoVO getDao() {
        return dao;
    }

    public void setDao(final ArrDaoVO dao) {
        this.dao = dao;
    }

    public ArrDaoRequest.Type getType() {
        return type;
    }

    public void setType(final ArrDaoRequest.Type type) {
        this.type = type;
    }

    public String getDidCode() {
        return didCode;
    }

    public void setDidCode(final String didCode) {
        this.didCode = didCode;
    }
}
