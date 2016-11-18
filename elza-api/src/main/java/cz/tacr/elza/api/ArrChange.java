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
        CREATE_AS("vytvoření AS"),

        /**
         * Připojení JP k výstupu.
         */
        ADD_NODES_OUTPUT("připojení JP k výstupu"),

        /**
         * Odpojení JP od výstupu.
         */
        REMOVE_NODES_OUTPUT("odpojení JP od výstupu"),

        /**
         * Založení JP.
         */
        ADD_LEVEL("založení JP"),

        /**
         * Přesun JP.
         */
        MOVE_LEVEL("přesun JP"),

        /**
         * Zrušení JP.
         */
        DELETE_LEVEL("zrušení JP"),

        /**
         * Založení rejstříkového hesla k JP.
         */
        ADD_RECORD_NODE("založení rejstříkového hesla k JP"),

        /**
         * Zrušení rejstříkového hesla k JP.
         */
        DELETE_RECORD_NODE("zrušení rejstříkového hesla k JP"),

        /**
         * Změna rejstříkového hesla k JP.
         */
        UPDATE_RECORD_NODE("změna rejstříkového hesla k JP"),

        /**
         * Změna atributu včetně změny pořadí.
         */
        UPDATE_DESC_ITEM("změna atributu"),

        /**
         * Založení atributu.
         */
        ADD_DESC_ITEM("založení atributu"),

        /**
         * Zrušení atributu.
         */
        DELETE_DESC_ITEM("zrušení atributu"),

        /**
         * Hromadná změna atributů - změny z tabulkového zobrazení, pokud se týká jen jedné JP, tak jde o typ Změna atributu.
         */
        BATCH_CHANGE_DESC_ITEM("hromadná změna atributů"),

        /**
         * Hromadné vymazání atributů - změny z tabulkového zobrazení, pokud se týká jen jedné JP, tak jde o typ Zrušení atributu.
         */
        BATCH_DELETE_DESC_ITEM("hromadné vymazání atributů"),

        /**
         * Hromadné funkce.
         */
        BULK_ACTION("hromadná funkce"),

        /**
         * Import AS.
         */
        IMPORT("import AS");

        private String description;

        Type(final String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
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
