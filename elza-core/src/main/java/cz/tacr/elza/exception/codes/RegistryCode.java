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
     * Nalezeno použití hesla v návazné tabulce.
     */
    EXIST_FOREIGN_DATA,

    /**
     * Existuje vazba z osoby
     */
    EXIST_FOREIGN_PARTY,

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
    USING_SCOPE_CANT_DELETE;
}
