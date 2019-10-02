package cz.tacr.elza.dataexchange.output.writer;

/**
 * Output stream for section export.
 */
public interface SectionOutputStream {

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
