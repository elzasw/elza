package cz.tacr.elza.controller.vo;

import java.util.List;

/**
 * Tail of the log file for the administration.
 */
public class LogVO {

    /** Why the lines could not be read; the client shows the message. */
    public enum Error {
        /** No path to the log file is configured. */
        NO_PATH,
        /** The file at {@link #getPath()} does not exist. */
        FILE_NOT_FOUND,
        /** The file at {@link #getPath()} could not be read. */
        READ_ERROR
    }

    private List<String> lines;

    private Integer lineCount;

    private Error error;

    private String path;

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
    }

    public Integer getLineCount() {
        return lineCount;
    }

    public void setLineCount(Integer lineCount) {
        this.lineCount = lineCount;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    /**
     * @return path of the log file named by {@link #getError()}, null otherwise
     */
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
