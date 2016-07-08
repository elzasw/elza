package cz.tacr.elza.api;

import java.io.Serializable;


public interface RulPackage extends Serializable {


    /**
     * @return identifikátor entity
     */
    Integer getPackageId();


    /**
     * @param packageId identifikátor entity
     */
    void setPackageId(Integer packageId);


    /**
     * @return název balíčku
     */
    String getName();


    /**
     * @param name název balíčku
     */
    void setName(String name);


    /**
     * @return kód balíčku
     */
    String getCode();


    /**
     * @param code kód balíčku
     */
    void setCode(String code);


    /**
     * @return popis
     */
    String getDescription();


    /**
     * @param description popis
     */
    void setDescription(String description);


    /**
     * @return verze balíčku
     */
    Integer getVersion();


    /**
     * @param version verze balíčku
     */
    void setVersion(Integer version);

}
