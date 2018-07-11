package cz.tacr.elza.exception.codes;

/**
 * Kódy pro rejstříky.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 21. 12. 2016
 */
public enum RegistryCode implements ErrorCode {

    /**
     * Existuje vazba z osoby
     */
    EXIST_FOREIGN_PARTY,

    /**
     * Nalezeno použití hesla v návazné tabulce.
     */
    EXIST_FOREIGN_DATA,

    /**
     * Nelze smazat rejstříkové heslo, které má potomky.
     */
    EXISTS_CHILD,

    /** Nebyl nalezen typ rejstříku. */
    REGISTRY_TYPE_NOT_FOUND,

    /**
     * Nelze vytvořit rejstříkové heslo, které je navázané na typ osoby.
     */
    CANT_CREATE_WITH_TYPE_PARTY,

    /**
     * Nelze nastavit rodiče rejstříkovému heslu sebe samotného.
     */
    CANT_BE_SELF_PARENT,

    /**
     * Nelze přidávat heslo do typu, který nemá přidávání hesel povolené.
     */
    REGISTRY_TYPE_DISABLE,

    /**
     * Nelze přidávat heslo k rodiči, který není hierarchický.
     */
    PARENT_IS_NOT_HIERARCHICAL,

    /**
     * Nelze změnit třídu rejstříku.
     */
    SCOPE_CANT_CHANGE,

    /**
     * Nelze změnit typ rejstříkového hesla na nehierarchický, pokud má heslo potomky.
     */
    HIERARCHICAL_RECORD_HAS_CHILDREN,

    /**
     * Nelze nastavit typ hesla, které je navázané na typ osoby.
     */
    CANT_CHANGE_WITH_TYPE_PARTY,

    /**
     * Nelze změnit typ rejstříkového hesla osoby, který odkazuje na jiný typ osoby.
     */
    CANT_CREATE_WITH_OTHER_TYPE_PARTY,

    /**
     * Nelze editovat hodnotu rejstříkového hesla napojeného na osobu.
     */
    CANT_CHANGE_VALUE_WITH_PARTY,

    /**
     * Nelze editovat charakteristiku rejstříkového hesla napojeného na osobu.
     */
    CANT_CHANGE_CHAR_WITH_PARTY,

    /**
     * Nelze editovat externí id rejstříkového hesla napojeného na osobu.
     */
    CANT_CHANGE_EXID_WITH_PARTY,

    /**
     * Nelze editovat externí systém rejstříkového hesla, které je napojené na osobu.
     */
    CANT_CHANGE_EXSYS_WITH_PARTY,

    /**
     * Potomek rejstříkového hesla musí mít stejný typ jako jeho rodič.
     */
    CHILD_AND_PARENT_DIFFERENT_TYPE,

    /**
     * Nelze smazat třídu rejstříku, která je nastavena na rejstříku.
     */
    USING_SCOPE_CANT_DELETE,

    /**
     * Kod třídy rejstříku již existuje.
     */
    SCOPE_EXISTS,

    /**
     * Třídě rejstříku nelze změnít kód.
     */
    SCOPE_CODE_CANT_CHANGE,

    /**
     * Osoba neexistuje.
     */
    PARTY_NOT_EXIST,

    /**
     * Navázaná entita musí mít stejnou třídu rejstříkového hesla jako osoba, ke které entitu navazujeme.
     */
    FOREIGN_ENTITY_INVALID_SCOPE,

    /**
     * Navázaná entita musí mít typ rejstříku nebo podtyp, který je navázaný na roli entity.
     */
    FOREIGN_ENTITY_INVALID_SUBTYPE,

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
     * Typ hesla musí mít vazbu na typ osoby.
     */
    REGISTRY_HAS_NOT_TYPE_PARTY;

}
