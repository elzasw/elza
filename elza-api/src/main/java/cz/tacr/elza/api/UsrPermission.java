package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Oprávnění pro uživatele / skupinu.
 *
 * @author Martin Šlapa
 * @since 26.04.2016
 */
public interface UsrPermission<U extends UsrUser, G extends UsrGroup, F extends ArrFund, S extends RegScope> extends Serializable {

    /**
     * @return identifikátor entity
     */
    Integer getPermissionId();

    /**
     * @param permissionId identifikátor entity
     */
    void setPermissionId(Integer permissionId);

    /**
     * @return typ oprávnění
     */
    Permission getPermission();

    /**
     * @param permission typ oprávnění
     */
    void setPermission(Permission permission);

    /**
     * @return uživatel, který má oprávnění přidělený
     */
    U getUser();

    /**
     * @param user uživatel, který má oprávnění přidělený
     */
    void setUser(U user);

    /**
     * @return skupina, která má oprávnění přidělený
     */
    G getGroup();

    /**
     * @param group skupina, která má oprávnění přidělený
     */
    void setGroup(G group);

    /**
     * @return archivní soubor, ke kterému se oprávnění vztahuje
     */
    F getFund();

    /**
     * @param fund archivní soubor, ke kterému se oprávnění vztahuje
     */
    void setFund(F fund);

    /**
     * @return scope, ke kterému se oprávnění vztahuje
     */
    S getScope();

    /**
     * @param scope scope, ke kterému se oprávnění vztahuje
     */
    void setScope(S scope);

    /**
     * Typy možných oprávnění.
     */
    enum Permission {

        /**
         * administrátor - všechna oprávnění
         * - má uživatel system
         */
        ADMINISTRATOR,

        /**
         * čtení vybraného AS
         * - má náhled jen na konrétní přiřazený AS ve všech verzích včeně OUPUT bez aktivních operací
         */
        AS_READ_ONE,

        /**
         * čtení všech AS
         * - má náhled na všechny AS ve všech verzích včeně OUPUT bez aktivních operací
         */
        AS_READ_ALL,

        /**
         * pořádání vybrané AS (pořádání je myšleno pořádání již vytvořeného AS, tvorba/úprava JP, tvorba úvodu, práce
         * s přílohami, správa obalů)
         * - úvod a přílohy zatím neřešíme
         * - platí jen pro konkrétní přiřazený AS
         * - aktivní správa obalů AS
         * - vidí OUTPUT, ale nemůže s nimi aktivně pracovat
         * - nemůže spouštět hromadné akce
         * - může přiřazovat rejstřík, ale jen v rozsahu práv na rejstříky (scope rejstříků), když nebude mít ani čtení
         *   rejstříků, tak nemůže nic přiřadit, opačně buď může přiřadit, nebo i zakládat nový ....
         */
        AS_ARRANGEMENT_ONE,

        /**
         * pořádání všech AS
         * - obdobně jako výše, ale pro všechny AS
         */
        AS_ARRANGEMENT_ALL,

        /**
         * čtení vybraného scope rejstříku (Pro přístup k rejstříkům a k osobám je řešen společným oprávněním.)
         * - přístup do části rejstříků včetně osob
         * - může jen pasivně číst rejstříková hesla z vybraného scope
         */
        REG_SCOPE_READ_ONE,

        /**
         * čtení všech scope rejstříků
         * - obdobně jako výše jen pro všechna rejstříková hesla
         */
        REG_SCOPE_READ_ALL,

        /**
         * zápis/úprava vybraného scope rejstříku
         * - obdobně jako výše, ale může hesla upravovat, přidávat, rušit, ale jen pro přiřazený scope
         */
        REG_SCOPE_WRITE_ONE,

        /**
         * zápis/úprava všech scope rejstříků
         * - obdboně jako výše pro všechna rejstříková hesla
         */
        REG_SCOPE_WRITE_ALL,

        /**
         * tvorba výstupů vybraného AS (AP, ad-hoc tisky)
         * - možnost vytvářet, měnit OUTPUT u přiřazeného AS
         * - může i verzovat OUTPUT což vyvolá verzi AS, ale beze změny pravidel
         * - nemůže exportovat, jen vytvářet
         */
        AS_OUTPUT_WRITE_ONE,

        /**
         * tvorba výstupů všech AS
         * - obdobně jako výše ale pro všechny AS
         */
        AS_OUTPUT_WRITE_ALL,

        /**
         * verzování a editace vybrané AS
         * - verzování a změna pravidel vpřiřazeného AS + přiřazení scope rejstříku + změna pravidel
         * - nemůže mazat AS
         */
        AS_VERSION_WRITE_ONE,

        /**
         * administrace všech AS (verzování, zakládání AS, zrušení AS, import)
         * - všechna práva na všechny AS včetně rejstříků, OUTPUT apodobně (vyjma uživatelů a případného dalšího systémového
         *   nastavení)
         */
        AS_ADMINISTRATOR,

        /**
         * export vybrané AS
         * - možnost exportu AS či OUTPUT přiřazeného AS
         */
        AS_EXPORT_ONE,

        /**
         * export všech AS
         * - obdobně jako výše ale pro všechny AS
         */
        AS_EXPORT_ALL,

        /**
         * správa oprávnění a uživatelů
         * - zatím neřešíme
         */
        USR_PERMISSION,

        /**
         * spouštění hromadných akcí vybrané AS
         * - možnost spuštění hromadných akcí přiřazeného AS
         */
        AS_BULK_ACTION_ONE,

        /**
         * spouštění hromadných akcí všech AS
         * - obdobně jako výše ale pro všechny AS
         */
        AS_BULK_ACTION_ALL,

        /**
         * drobné úpravy uzavřených vybraných AS
         * - zatím neřešíme
         */
        AS_CLOSE_VERSION_WRITE_ONE,

        /**
         * drobné úpravy uzavřených všech AS
         * - zatím neřešíme
         */
        AS_CLOSE_VERSION_WRITE_ALL,

    }
}
