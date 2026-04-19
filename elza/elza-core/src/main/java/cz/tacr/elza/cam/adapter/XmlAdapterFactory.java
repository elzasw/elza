package cz.tacr.elza.cam.adapter;

/**
 * Továrna na adaptery položek entity (ItemXxxXml).
 *
 * Každá verze CAM (v1, v2) má vlastní implementaci, která zná své
 * verzově specifické JAXB typy.
 */
public interface XmlAdapterFactory {

    /**
     * Zabalí libovolný ItemXxxXml objekt do typově specifického adapteru.
     *
     * @throws IllegalArgumentException pokud typ položky není podporován
     */
    XmlItemAdapter wrapItem(Object xmlItem);
}
