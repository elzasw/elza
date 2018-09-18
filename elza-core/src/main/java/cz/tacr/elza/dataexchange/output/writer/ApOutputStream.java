package cz.tacr.elza.dataexchange.output.writer;

/**
 * Output stream for access points export.
 */
public interface ApOutputStream {

    void addAccessPoint(ApInfo apInfo);

    void processed();

    void close();
}
