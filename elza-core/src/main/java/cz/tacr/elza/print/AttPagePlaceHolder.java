package cz.tacr.elza.print;

/**
 * Placeholder stránky pro přílohy
 *
 */
public class AttPagePlaceHolder {

    // mark for attachments 
    public static final String INCL_PATTERN = "#include_attachements";

    public final String itemType;
    public final String placeHolder;

    public AttPagePlaceHolder(final String itemType) {
        this.itemType = itemType;
        this.placeHolder = INCL_PATTERN + "(" + itemType + ")";
    }

    public String getAttPage() {
        return placeHolder;
    }
}
