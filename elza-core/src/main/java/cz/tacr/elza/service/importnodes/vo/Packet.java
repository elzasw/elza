package cz.tacr.elza.service.importnodes.vo;

/**
 * Rozhraní pro reprezentaci obalu.
 *
 * @since 19.07.2017
 */
public interface Packet {

    /**
     * @return označení obalu
     */
    String getStorageNumber();

    /**
     * @return kód typu obalu
     */
    String getTypeCode();

}
