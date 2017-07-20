package cz.tacr.elza.service.importnodes.vo;

/**
 * Výsledek validace.
 *
 * @since 19.07.2017
 */
public class ValidateResult {

    /**
     * Chyba ve scopech - fatální.
     */
    private boolean scopeError;

    /**
     * Konflikt souborů.
     */
    private boolean fileConflict;

    /**
     * Konflikt obalů.
     */
    private boolean packetConflict;

    public void setScopeError(final boolean scopeError) {
        this.scopeError = scopeError;
    }

    public boolean isScopeError() {
        return scopeError;
    }

    public void setFileConflict(final boolean fileConflict) {
        this.fileConflict = fileConflict;
    }

    public boolean isFileConflict() {
        return fileConflict;
    }

    public void setPacketConflict(final boolean packetConflict) {
        this.packetConflict = packetConflict;
    }

    public boolean isPacketConflict() {
        return packetConflict;
    }
}
