package cz.tacr.elza.exception.codes;

/**
 * Kódy pro rejstříky.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 21. 12. 2016
 */
public enum RegistryCode implements ErrorCode {

    /**
     * Nelze upravit odstraněný přístupový bod.
     */
    CANT_CHANGE_DELETED_AP,

    /**
     * Nelze upravit odstraněné jméno přístupového bodu.
     */
    CANT_CHANGE_DELETED_NAME,

    /**
     * Nelze smazat preferované jméno.
     */
    CANT_DELETE_PREFERRED_NAME,

    /**
     * Nelze migrovat přístupový bod.
     */
    CANT_MIGRATE_AP,

    /**
     * Cílová entita je schválená nebo čeká na schválení a nelze ji měnit.
     */
    CANT_MERGE,

    /**
     * Nalezeno použití hesla v návazné tabulce.
     */
    EXIST_FOREIGN_DATA,

    /**
     * Existuje vazba z osoby
     */
    EXIST_FOREIGN_PARTY,

    /**
     * Existuje vazba z institucí
     */
    EXIST_INSTITUCI,

    /**
     * Navázaná entita musí mít stejnou třídu rejstříkového hesla jako osoba, ke
     * které entitu navazujeme.
     */
    FOREIGN_ENTITY_INVALID_SCOPE,

    /**
     * Navázaná entita musí mít typ rejstříku nebo podtyp, který je navázaný na roli
     * entity.
     */
    FOREIGN_ENTITY_INVALID_SUBTYPE,

    /**
     * Celé jméno není unikátní v rámci třídy.
     */
    NOT_UNIQUE_FULL_NAME,

    /**
     * Osoba neexistuje.
     */
    PARTY_NOT_EXIST,

    /**
     * Typ hesla musí mít vazbu na typ osoby.
     */
    REGISTRY_HAS_NOT_TYPE_PARTY,

    /** Nebyl nalezen typ rejstříku. */
    REGISTRY_TYPE_NOT_FOUND,

    /**
     * Třídě rejstříku nelze změnít kód.
     */
    SCOPE_CODE_CANT_CHANGE,

    /**
     * Kod třídy rejstříku již existuje.
     */
    SCOPE_EXISTS,

    /**
     * Nelze smazat třídu rejstříku, která je nastavena na rejstříku.
     */
    USING_SCOPE_CANT_DELETE,

    /**
     * Nelze smazat třídu rejstříku na kterou je navázána jiná třída rejstříku.
     */
    CANT_DELETE_CONNECTED_SCOPE,

    /**
     * Nelze smazat třídu rejstříku která je navázána na jinou třídu rejstříku.
     */
    CANT_DELETE_SCOPE_WITH_CONNECTED,

    /**
     * Nelze navázat třídu rejstříku sama na sebe.
     */
    CANT_CONNECT_SCOPE_TO_SELF,

    /**
     * Nelze navázat třídu, protože je již navázána.
     */
    SCOPES_ALREADY_CONNECTED,

    /**
     * Nelze zrušit provázání tříd, protože vazba neexistuje.
     */
    SCOPES_NOT_CONNECTED,

    /**
     * Nelze zrušit provázání tříd, protože existuje vztah mezi osobami těchto tříd.
     */
    CANT_DELETE_SCOPE_RELATION_EXISTS,

    /**
     * Archivní entita má jíž existující propojení s externím systémem.
     */
    EXT_SYSTEM_CONNECTED,
    
    /**
     * Archivní entita má nevhodný scope
     */
    INVALID_ENTITY_SCOPE,
}
