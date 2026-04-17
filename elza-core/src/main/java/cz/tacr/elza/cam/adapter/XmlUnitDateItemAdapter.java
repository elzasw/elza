package cz.tacr.elza.cam.adapter;

/**
 * Adapter pro ItemUnitDateXml.
 *
 * V1 používá zkrácené gettery ({@code getF}, {@code getFmt}, {@code isFe} ...),
 * v2 plné varianty ({@code getFrom}, {@code getFormat}, {@code isFromEstimate} ...).
 */
public interface XmlUnitDateItemAdapter extends XmlItemAdapter {

    String getValueFrom();

    String getValueTo();

    String getFormat();

    /** {@code null}, pokud schéma hodnotu neuvádí. */
    Boolean isFromEstimate();

    /** {@code null}, pokud schéma hodnotu neuvádí. */
    Boolean isToEstimate();
}
