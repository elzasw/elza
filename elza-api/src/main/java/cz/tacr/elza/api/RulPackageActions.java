package cz.tacr.elza.api;

import java.io.Serializable;


public interface RulPackageActions<P extends RulPackage> extends Serializable {


    /**
     * @return identifikátor entity
     */
    Integer getPackageActionsId();


    /**
     * @param packageActionsId identifikátor entity
     */
    void setPackageActionsId(Integer packageActionsId);


    /**
     * @return balíček
     */
    P getPackage();


    /**
     * @param rulPackage balíček
     */
    void setPackage(P rulPackage);


    /**
     * @return název souboru
     */
    String getFilename();


    /**
     * @param filename název souboru
     */
    void setFilename(String filename);


}
