package cz.tacr.elza.cam.adapter;

/**
 * Adapter pro ItemEntityRefXml.
 *
 * Vrací již normalizovaný identifikátor (entityId nebo UUID) z původního
 * EntityRecordRefXml — jeho typ se mezi v1/v2 liší a do sdíleného rozhraní nepatří.
 */
public interface XmlEntityRefItemAdapter extends XmlItemAdapter {

    String getRefIdOrUuid();
}
