package cz.tacr.elza.print.format;

public interface ConditionalFormatAction extends FormatAction {
    /**
     * Add subaction
     * @param formatAction
     */
    void addAction(FormatAction formatAction);
}
