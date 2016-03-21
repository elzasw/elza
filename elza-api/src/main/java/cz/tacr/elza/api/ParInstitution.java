package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Instituce.
 *
 * @author Martin Šlapa
 * @since 18.3.2016
 */
public interface ParInstitution<IT extends ParInstitutionType, P extends ParParty> extends Serializable {

    /**
     * @return identifikátor
     */
    Integer getInstitutionId();

    /**
     * @param institutionId identifikátor
     */
    void setInstitutionId(Integer institutionId);

    /**
     * @return kód instituce
     */
    String getCode();

    /**
     * @param code kód instituce
     */
    void setCode(String code);

    /**
     * @return typ instituce
     */
    IT getInstitutionType();

    /**
     * @param institutionType typ instituce
     */
    void setInstitutionType(IT institutionType);

    /**
     * @return osoba
     */
    P getParty();

    /**
     * @param party osoba
     */
    void setParty(P party);

}
