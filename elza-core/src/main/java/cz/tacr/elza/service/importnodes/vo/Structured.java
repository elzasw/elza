package cz.tacr.elza.service.importnodes.vo;

/**
 * Rozhraní pro reprezentaci obalu.
 *
 * @since 19.07.2017
 */
public interface Structured {

    /**
     * @return hodnota
     */
    String getValue();

    /**
     * @return kód typu
     */
    String getStructureTypeCode();

}
