package cz.tacr.elza.exception.codes;

/**
 * Kódy pro digitalizaci.
 *
 * @author Martin Lebeda
 * @since 22.12.2016
 */
public enum DigitizationCode implements ErrorCode {

    /**
     * Digitalizační repository neexistuje.
     */
    REPOSITORY_NOT_FOUND,

    /**
     * DAO neexistuje.
     */
    DAO_NOT_FOUND,

    /**
     * DAO a Node okazují na různý package
     */
    DAO_AND_NODE_HAS_DIFFERENT_PACKAGE,

    /**
     * DAO package neexistuje.
     */
    PACKAGE_NOT_FOUND,

    /**
     * DAO Request je neočekávaného typu
     */
    UNWANTED_REQUEST_TYPE,

    /**
     * Není vyplněn povinný externí identifikátor
     */
    NOT_FILLED_EXTERNAL_IDENTIRIER,

    /**
     * DAO má navázané aktivní requesty na externí systém
     */
    DAO_HAS_REQUEST,


}
