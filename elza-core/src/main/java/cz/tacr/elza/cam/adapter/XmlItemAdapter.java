package cz.tacr.elza.cam.adapter;

/**
 * Společné rozhraní pro položky entity (ItemXxxXml) napříč CAM v1/v2.
 *
 * Skrývá rozdíly v pojmenování JAXB getterů (např. {@code getT()} vs.
 * {@code getType()}). Konkrétní hodnotové gettery jsou v podtypech
 * podle datového typu položky.
 */
public interface XmlItemAdapter {

    /** UUID položky (ItemXxxXml.getUuid().getValue()). */
    String getUuid();

    /** Kód typu položky (ItemXxxXml.getT()/getType()). */
    XmlCodeAdapter getType();

    /** Kód specifikace, může být {@code null}. */
    XmlCodeAdapter getSpec();

    /**
     * Vrací původní JAXB objekt.
     *
     * Použít jen tam, kde je potřeba předat původní instanci dál
     * (např. {@code ItemUpdates.addNewItem()} ji ukládá pro pozdější zpracování).
     */
    Object getRaw();
}
