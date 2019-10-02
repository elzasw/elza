package cz.tacr.elza.print;

import java.io.Closeable;
import java.util.List;

/**
 * Page provider for attachments
 *
 * Page provider have to be closed when not needed.
 */
public interface AttPageProvider
        extends Closeable {

    /**
     * Return collection of placeholders for given item type
     * 
     * @param itemTypeCode
     * @return
     */
    List<AttPagePlaceHolder> getAttPagePlaceHolders(String itemTypeCode);

}
