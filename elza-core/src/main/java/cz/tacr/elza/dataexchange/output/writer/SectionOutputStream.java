package cz.tacr.elza.dataexchange.output.writer;

/**
 * Output stream for section export.
 */
public interface SectionOutputStream {

    /**
     * Add file to the section
     * 
     * @param fileInfo
     */
    void addFile(FileInfo fileInfo);

    void addLevel(LevelInfo levelInfo);

    void addStructObject(StructObjectInfo structObjectInfo);

    /**
     * Writer will be notified about finished section export.
     */
    void processed();

    /**
     * Release all resources.
     */
    void close();
}
