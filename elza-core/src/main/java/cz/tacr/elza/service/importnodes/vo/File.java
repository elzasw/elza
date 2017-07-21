package cz.tacr.elza.service.importnodes.vo;

import java.io.InputStream;

/**
 * Rozhraní pro import souboru.
 *
 * @since 19.07.2017
 */
public interface File {

    /**
     * @return název souboru
     */
    String getName();

    /**
     * @return soubor
     */
    InputStream getFileStream();

    /**
     * @return název souboru
     */
    String getFileName();

    /**
     * @return velikost souboru
     */
    Integer getFileSize();

    /**
     * @return mime type souboru
     */
    String getMimeType();

    /**
     * @return počet stránek (nepovinné)
     */
    Integer getPagesCount();

}
