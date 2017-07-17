package cz.tacr.elza.controller.vo;

/**
 * @since 17.07.2017
 */
public class CopyNodesValidateResult {

    private boolean scopeError;

    private boolean fileConflict;

    private boolean packetConflict;

    public boolean isScopeError() {
        return scopeError;
    }

    public void setScopeError(final boolean scopeError) {
        this.scopeError = scopeError;
    }

    public boolean isFileConflict() {
        return fileConflict;
    }

    public void setFileConflict(final boolean fileConflict) {
        this.fileConflict = fileConflict;
    }

    public boolean isPacketConflict() {
        return packetConflict;
    }

    public void setPacketConflict(final boolean packetConflict) {
        this.packetConflict = packetConflict;
    }
}
