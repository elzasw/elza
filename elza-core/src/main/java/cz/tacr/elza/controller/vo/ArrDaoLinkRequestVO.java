package cz.tacr.elza.controller.vo;

import cz.tacr.elza.api.ArrDaoRequest;
import cz.tacr.elza.domain.ArrDaoLinkRequest;

/**
 * Value objekt {@link ArrDaoLinkRequest}
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
public class ArrDaoLinkRequestVO extends ArrRequestVO {

    private ArrDaoVO dao;

    private ArrDaoLinkRequest.Type type;

    private String didCode;

    private TreeNodeClient node;

    public ArrDaoVO getDao() {
        return dao;
    }

    public void setDao(final ArrDaoVO dao) {
        this.dao = dao;
    }

    public ArrDaoLinkRequest.Type getType() {
        return type;
    }

    public void setType(final ArrDaoLinkRequest.Type type) {
        this.type = type;
    }

    public String getDidCode() {
        return didCode;
    }

    public void setDidCode(final String didCode) {
        this.didCode = didCode;
    }

    public TreeNodeClient getNode() {
        return node;
    }

    public void setNode(final TreeNodeClient node) {
        this.node = node;
    }
}
