package cz.tacr.elza.api;

import java.io.Serializable;

/**
 *  Typ instituce.
 *
 *  @author Martin Šlapa
 *  @since 18.3.2016
 */
public interface ParInstitutionType extends Serializable {

    /**
     * @return identifikátor
     */
    Integer getInstitutionTypeId();

    /**
     * @param institutionTypeId identifikátor
     */
    void setInstitutionTypeId(Integer institutionTypeId);

    /**
     * @return název typu instituce
     */
    String getName();

    /**
     * @param name název typu instituce
     */
    void setName(String name);

    /**
     * @return kód typu instituce
     */
    String getCode();

    /**
     * @param code kód typu instituce
     */
    void setCode(String code);

}
