package cz.tacr.elza.api;

/**
 * Skupina souborů k DAO.
 *
 * @author Martin Šlapa
 * @since 06.12.2016
 */
public interface ArrDaoFileGroup<D extends ArrDao> {

    Integer getDaoFileGroupId();

    void setDaoFileGroupId(Integer daoFileGroupId);

    D getDao();

    void setDao(D dao);

    String getLabel();

    void setLabel(String label);

    String getCode();

    void setCode(String code);
}
