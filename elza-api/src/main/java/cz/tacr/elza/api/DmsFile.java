package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Soubor
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.2016
 */
public interface DmsFile extends Serializable {

    /**
     * @return id souboru
     */
    Integer getFileId();

    /**
     * Nastaví id souboru
     * @param fileId id souboru
     */
    void setFileId(Integer fileId);

    /**
     * @return název souboru
     */
    String getName();

    /**
     * @param name název souboru
     */
    void setName(String name);

    /**
     * @return Reálný název souboru
     */
    String getFileName();

    /**
     * @param fileName Reálný název souboru
     */
    void setFileName(String fileName);

    /**
     * @return velikost
     */
    Integer getFileSize();

    /**
     * @param fileSize velikost
     */
    void setFileSize(Integer fileSize);

    /**
     * @return mime
     */
    String getMimeType();

    /**
     * @param mimeType mime
     */
    void setMimeType(String mimeType);

    /**
     * @return Počet stran (pouze u pdf)
     */
    Integer getPagesCount();

    /**
     * @param pagesCount Počet stran (pouze u pdf)
     */
    void setPagesCount(Integer pagesCount);
}
