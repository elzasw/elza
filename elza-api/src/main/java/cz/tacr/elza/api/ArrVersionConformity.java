package cz.tacr.elza.api;

import java.io.Serializable;


/**
 * Stav verze archivní pomůcky.
 *
 * @author Martin Šlapa
 * @since 2.12.2015
 */
public interface ArrVersionConformity<FAV extends ArrFundVersion> extends Serializable {

    enum State {
        OK,
        ERR;
    }


    /**
     * @return identifikátor záznamu
     */
    Integer getVersionConformityId();


    /**
     * @param versionConformityId identifikátor záznamu
     */
    void setVersionConformityId(Integer versionConformityId);


    /**
     * @return verze archivní pomůcky
     */
    FAV getFundVersion();


    /**
     * @param fundVersion verze archivní pomůcky
     */
    void setFundVersion(FAV fundVersion);


    /**
     * @return Stav verze.
     */
    State getState();


    /**
     * @param state Stav verze.
     */
    void setState(State state);


    /**
     * @return popis stavu
     */
    String getStateDescription();


    /**
     * @param stateDescription popis stavu
     */
    void setStateDescription(String stateDescription);
}
