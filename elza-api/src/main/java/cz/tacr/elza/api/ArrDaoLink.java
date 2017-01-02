package cz.tacr.elza.api;


import java.io.Serializable;


/**
 * Digitální archivní objekt (digitalizát).
 *
 * @author Martin Šlapa
 * @since 06.12.2016
 */
public interface ArrDaoLink<D extends ArrDao, C extends ArrChange, N extends ArrNode> extends Serializable {

    Integer getDaoLinkId();

    void setDaoLinkId(Integer daoLinkId);

    N getNode();

    void setNode(N node);

    D getDao();

    void setDao(D dao);

    C getCreateChange();

    void setCreateChange(C createChange);

    C getDeleteChange();

    void setDeleteChange(C deleteChange);
}
