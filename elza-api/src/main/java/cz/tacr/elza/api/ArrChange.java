package cz.tacr.elza.api;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Seznam provedených změn v archivních pomůckách.
 * @author vavrejn
 *
 */
public interface ArrChange<U extends UsrUser, N extends ArrNode> extends Serializable {

    /**
     * Typ změny.
     */
    enum Type {

        /**
         * Vytvoření AS.
         */
        CREATE_AS,

        /**
         * Připojení JP k výstupu.
         */
        ADD_NODES_OUTPUT,

        /**
         * Odpojení JP od výstupu.
         */
        REMOVE_NODES_OUTPUT,

        /**
         * Založení JP.
         */
        ADD_LEVEL,

        /**
         * Přesun JP.
         */
        MOVE_LEVEL,

        /**
         * Zrušení JP.
         */
        DELETE_LEVEL,

        /**
         * Založení rejstříkového hesla k JP.
         */
        ADD_RECORD_NODE,

        /**
         * Zrušení rejstříkového hesla k JP.
         */
        DELETE_RECORD_NODE,

        /**
         * Změna rejstříkového hesla k JP.
         */
        UPDATE_RECORD_NODE,

        /**
         * Změna atributu včetně změny pořadí.
         */
        UPDATE_DESC_ITEM,

        /**
         * Založení atributu.
         */
        ADD_DESC_ITEM,

        /**
         * Zrušení atributu.
         */
        DELETE_DESC_ITEM,

        /**
         * Hromadná změna atributů - změny z tabulkového zobrazení, pokud se týká jen jedné JP, tak jde o typ Změna atributu.
         */
        BATCH_CHANGE_DESC_ITEM,

        /**
         * Hromadné vymazání atributů - změny z tabulkového zobrazení, pokud se týká jen jedné JP, tak jde o typ Zrušení atributu.
         */
        BATCH_DELETE_DESC_ITEM,

        /**
         * Hromadné funkce.
         */
        BULK_ACTION,

        /**
         * Import AS.
         */
        IMPORT,

        /**
         * Požadavek na digitalizaci.
         */
        CREATE_DIGI_REQUEST,

        /**
         * Vytvoření položky ve frontě.
         */
        CREATE_REQUEST_QUEUE,

        /**
         * Vytvoření vazby na DAO
         */
        CREATE_DAO_LINK,

        /**
         * Zrušení vazby na DAO
         */
        DELETE_DAO_LINK;

    }

    /**
     * 
     * @return číslo změny.
     */
    Integer getChangeId();

    /**
     * Nastaví číslo změny.
     * @param changeId  číslo změny.
     */
    void setChangeId(Integer changeId);

    /**
     * 
     * @return datum změny.
     */
    LocalDateTime getChangeDate();

    /**
     * Nastaví datum změny.
     * @param changeDate datum změny.
     */
    void setChangeDate(LocalDateTime changeDate);

    /**
     * @return uživatel, který provedl změnu
     */
    U getUser();

    /**
     * @param user uživatel, který provedl změnu
     */
    void setUser(U user);

    /**
     * @return typ změny
     */
    Type getType();

    /**
     * @param type typ změny
     */
    void setType(Type type);

    /**
     * @return primární uzel změny
     */
    N getPrimaryNode();

    /**
     * @param primaryNode primární uzel změny
     */
    void setPrimaryNode(N primaryNode);
}
