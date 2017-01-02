package cz.tacr.elza.api;


import java.io.Serializable;


/**
 * Digitální archivní objekt (digitalizát).
 *
 * @author Martin Šlapa
 * @since 06.12.2016
 */
public interface ArrDao<DP extends ArrDaoPackage> extends Serializable {

    Integer getDaoId();

    void setDaoId(Integer daoId);

    DP getDaoPackage();

    void setDaoPackage(DP daoPackage);

    Boolean getValid();

    void setValid(Boolean valid);

    String getCode();

    void setCode(String code);

    String getLabel();

    void setLabel(String label);
}
