package cz.tacr.elza.service.da;

import java.util.Locale;

import javax.annotation.Nullable;

/**
 * An element of {@code <did>} of a unit of description, as the DA_IMPORT script sees it. The
 * script reads what it needs to choose the item type and hands the element back to
 * {@link DaLevelItems#item(String, String, DaImportElement)}, which reads its value - the
 * script never parses EAD itself.
 */
public class DaImportElement {

    private final Object source;
    private final String unitId;

    DaImportElement(Object source, @Nullable String unitId) {
        this.source = source;
        this.unitId = unitId;
    }

    /** Name of the element in EAD, e.g. unitdatestructured, unitid. */
    public String getName() {
        return source.getClass().getSimpleName().toLowerCase(Locale.ROOT);
    }

    /**
     * The kind of the element within its name, e.g. CONTENT for a date of content, UKLADACI_ZNAK
     * for a filing code; null when it has none.
     */
    @Nullable
    public String getLocalType() {
        return DidElementConverters.localType(source);
    }

    /**
     * Own text of the element, with white space normalized - e.g. the name of a storage unit
     * ({@code <container>}) a script names a level by; null for an element without text, such as
     * a structured date.
     */
    @Nullable
    public String getText() {
        return DidElementConverters.text(source);
    }

    /** id of the unit of description the element belongs to - for the messages. */
    @Nullable
    String getUnitId() {
        return unitId;
    }

    Object getSource() {
        return source;
    }

    @Override
    public String toString() {
        String localType = getLocalType();
        return "<" + getName() + (localType != null ? " localtype=\"" + localType + "\"" : "") + ">";
    }
}
