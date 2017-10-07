package cz.tacr.elza.service.importnodes.vo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import cz.tacr.elza.domain.ArrPacket;

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

	public boolean isFileConflict() {
		return fileConflict;
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

    public Collection<String> getPacketConflicts() {
        return packetConflicts;
    }


	public void addMissingScope(String code) {
		if (scopeErrors == null) {
			scopeError = true;
			scopeErrors = new HashSet<>();
		}
		scopeErrors.add(code);
	}

	public void addPacketConflicts(ArrPacket srcPacket) {
		if (packetConflicts == null) {
			packetConflict = true;
			packetConflicts = new ArrayList<>();
		}
		packetConflicts.add(srcPacket.getStorageNumber());
	}

	public void addFileConflict(String name) {
		if (fileConflicts == null) {
			fileConflict = true;
			fileConflicts = new ArrayList<>();
		}
		fileConflicts.add(name);
	}
}
