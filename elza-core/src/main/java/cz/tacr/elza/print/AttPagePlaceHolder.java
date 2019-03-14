package cz.tacr.elza.print;

/**
 * Placeholder stránky pro přílohy
 *
 */
public class AttPagePlaceHolder {

    // mark for attachments 
    public static final String INCL_PATTERN = "#include_attachements";

    //private final String itemType;
    private final String placeHolder;

    private final String attachmentName;
    /**
     * Page index (counted from 0)
     */
    private final int pageIndex;

    public AttPagePlaceHolder(final String itemType, final String attachmentName, final int pageNumber) {
        //this.itemType = itemType;
        this.attachmentName = attachmentName;
        this.placeHolder = INCL_PATTERN + "(" + itemType + ")";
        this.pageIndex = pageNumber;
    }

    public String getAttPage() {
        return placeHolder;
    }

    public String getName() {
        return attachmentName;
    }

    public Integer getPageIndex() {
        return pageIndex;
    }
}
