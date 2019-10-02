package cz.tacr.elza.dataexchange.output.writer;

import cz.tacr.elza.dataexchange.output.aps.ApInfo;

/**
 * Output stream for access points export.
 */
public interface ApOutputStream {

    void addAccessPoint(ApInfo apInfo);

    void processed();

    void close();
}
