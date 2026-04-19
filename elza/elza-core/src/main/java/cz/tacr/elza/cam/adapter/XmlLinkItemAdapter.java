package cz.tacr.elza.cam.adapter;

/** Adapter pro ItemLinkXml. */
public interface XmlLinkItemAdapter extends XmlItemAdapter {

    String getUrl();

    /**
     * Popisek odkazu, může být {@code null}.
     *
     * Adaptery vrací surovou hodnotu — případnou normalizaci prázdného
     * řetězce na {@code null} (chování v1) řeší volající.
     */
    String getDescription();
}
