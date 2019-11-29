package cz.tacr.elza.dataexchange.output.writer;

import java.io.InputStream;

/**
 * Wrapper for DMSFile
 * 
 * This wrapper allows to read binary data
 *
 */
public interface FileInfo {

    Integer getId();

    String getName();

    InputStream getInputStream();

}
