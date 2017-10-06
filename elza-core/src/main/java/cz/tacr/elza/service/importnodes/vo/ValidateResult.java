package cz.tacr.elza.service.importnodes.vo;

import java.util.Collection;
import java.util.HashSet;

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
     * Seznam chyb ve scopech.
     */
    private Collection<String> scopeErrors;

    /**
     * Konflikt souborů.
     */
    private boolean fileConflict;

    /**
     * Seznam konfliktů v souborech.
     */
    private Collection<String> fileConflicts;

    /**
     * Konflikt obalů.
     */
    private boolean packetConflict;

    /**
     * Seznam konfliktů v obalech.
     */
    private Collection<String> packetConflicts;

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

    public Collection<String> getScopeErrors() {
        return scopeErrors;
    }

    public Collection<String> getFileConflicts() {
        return fileConflicts;
    }

    public void setFileConflicts(final Collection<String> fileConflicts) {
        this.fileConflicts = fileConflicts;
    }

    public Collection<String> getPacketConflicts() {
        return packetConflicts;
    }

    public void setPacketConflicts(final Collection<String> packetConflicts) {
        this.packetConflicts = packetConflicts;
    }

	public void addMissingScope(String code) {
		scopeError = true;
		if (scopeErrors == null) {
			scopeErrors = new HashSet<>();
		}
		scopeErrors.add(code);
	}
}
