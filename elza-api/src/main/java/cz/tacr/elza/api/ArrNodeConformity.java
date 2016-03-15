package cz.tacr.elza.api;

import java.util.Date;


/**
 * Stav uzlu v rámci verze archivní pomůcky.
 * V případě sdílení jsou stavy uzlů uloženy pro každou verzi AP.
 * Při uzamčení pomůcky zůstane stav uzlu uložen a nemůže již být měněn.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 19.11.2015
 */
public interface ArrNodeConformity<AN extends ArrNode, AFAV extends ArrFundVersion> {

    /**
     * Stav uzlu.
     */
    enum State {
        OK,
        ERR;
    }


    /**
     * @return id stavu
     */
    Integer getNodeConformityId();


    /**
     * @param nodeConformityId id stavu
     */
    void setNodeConformityId(Integer nodeConformityId);


    /**
     * @return uzel, kterému názeží stav
     */
    AN getNode();


    /**
     * @param node uzel, kterému názeží stav
     */
    void setNode(AN node);


    /**
     * @return verze archivní pomůcky
     */
    AFAV getFundVersion();


    /**
     * @param fundVersion verze archivní pomůcky
     */
    void setFundVersion(AFAV fundVersion);


    /**
     * @return stav uzlu (OK/ERR)
     */
    State getState();


    /**
     * @param state stav uzlu   (OK/ERR)
     */
    void setState(State state);


    /**
     * @return textový popis případného chybového stavu
     */
    String getDescription();


    /**
     * @param description textový popis případného chybového stavu
     */
    void setDescription(String description);


    /**
     * @return datum a čas nastavení stavu
     */
    Date getDate();


    /**
     * @param date datum a čas nastavení stavu
     */
    void setDate(Date date);
}
