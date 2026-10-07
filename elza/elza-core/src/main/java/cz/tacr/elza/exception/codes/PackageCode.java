package cz.tacr.elza.exception.codes;

/**
 * Kódy balíčků.
 *
 * @author Martin Šlapa
 * @since 21.11.2016
 */
public enum PackageCode implements ErrorCode {

    /**
     * Nenalezen soubor.
     */
    FILE_NOT_FOUND,

    /**
     * Nenalezena entita podle kódu v souboru.
     */
    CODE_NOT_FOUND,

    /**
     * Verze balíčku byla již aplikována.
     */
    VERSION_APPLIED,

    /**
     * Chyba při parsování XML souboru.
     */
    PARSE_ERROR,

    /**
     * Entita existuje již v jiném balíčku.
     */
    OTHER_PACKAGE,

    /**
     * Balíček neexistuje.
     */
    PACKAGE_NOT_EXIST,

    /**
     * Cyklická závislost.
     */
    CIRCULAR_DEPENDENCY,

    /**
     * Není splněna minimální verze balíčku {code}: {version}.
     */
    MIN_DEPENDENCY,

    /**
     * Existuje  {code}: {version}.
     */
    FOREIGN_DEPENDENCY,

    /**
     * Nebyly nalezeny požadované balíčky: {codes}.
     */
    FOREIGN_PACKAGES_NOT_EXIST,

    /**
     * Scenario nebyl nalezen
     */
    SCENARIO_NOT_FOUND,

    /**
     * Invalid row of a translation file: unknown type or field, duplicate key, wrong message code.
     */
    INVALID_TRANSLATION,

    /**
     * Invalid entity rules of a rule set: rules of an entity rule set in the old format, entity
     * rules in a rule set of another type, a rule without a file.
     */
    INVALID_ENTITY_RULE,

    /**
     * An entity class is declared by another package with another parent.
     */
    AP_TYPE_CONFLICT,
}
