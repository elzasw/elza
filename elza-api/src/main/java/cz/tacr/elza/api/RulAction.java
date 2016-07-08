package cz.tacr.elza.api;

import java.io.Serializable;


public interface RulAction<P extends RulPackage> extends Serializable {


    /**
     * @return identifikátor entity
     */
    Integer getActionId();


    /**
     * @param actionId identifikátor entity
     */
    void setActionId(Integer actionId);


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
