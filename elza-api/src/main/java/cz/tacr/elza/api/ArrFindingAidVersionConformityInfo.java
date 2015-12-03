package cz.tacr.elza.api;

import java.io.Serializable;


/**
 * Stav verze archivní pomůcky.
 *
 * @author Martin Šlapa
 * @since 2.12.2015
 */
public interface ArrFindingAidVersionConformityInfo<FAV extends ArrFindingAidVersion> extends Serializable {

    enum State {
        OK,
        ERR;
    }


    /**
     * @return identifikátor záznamu
     */
    Integer getFindingAidVersionConformityInfoId();


    /**
     * @param findingAidVersionConformityInfoId identifikátor záznamu
     */
    void setFindingAidVersionConformityInfoId(Integer findingAidVersionConformityInfoId);


    /**
     * @return verze archivní pomůcky
     */
    FAV getFaVersion();


    /**
     * @param faVersion verze archivní pomůcky
     */
    void setFaVersion(FAV faVersion);


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
