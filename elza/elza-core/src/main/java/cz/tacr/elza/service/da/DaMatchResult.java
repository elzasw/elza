package cz.tacr.elza.service.da;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * What the DA_MATCH script decides about a received package that matches no unit of description
 * by UUID (variable {@code MATCH}): whether it is placed into the archival description
 * automatically, and under which levels.
 *
 * The script describes a chain of levels, top-down from the root of the fund - each call of
 * {@link #level()} adds the next one - and ends it with exactly one of {@link #importHere()},
 * {@link #linkHere()} or {@link #none()}. Each level of the chain is found among the children of
 * the level above it by its match key, or created. The script writes nothing itself.
 */
public class DaMatchResult {

    /** How the chain ends. */
    public enum Ending {
        /** The package is imported below the last level, as the DA_IMPORT script plans it. */
        IMPORT_HERE,
        /** The whole package is linked to the last level; nothing of it is imported. */
        LINK_HERE,
        /** The package is not placed automatically. */
        NONE
    }

    private final StaticDataProvider sdp;
    private final String eadHref;
    private final List<DaMatchLevel> levels = new ArrayList<>();
    private Ending ending;

    DaMatchResult(StaticDataProvider sdp, @Nullable String eadHref) {
        this.sdp = sdp;
        this.eadHref = eadHref;
    }

    /** The next level of the chain, below the previous one; the first one is below the root of the fund. */
    public DaMatchLevel level() {
        DaMatchLevel level = new DaMatchLevel(sdp, levels.size() + 1, eadHref);
        levels.add(level);
        return level;
    }

    /** The package is imported below the last level of the chain (below the root of the fund without one). */
    public void importHere() {
        end(Ending.IMPORT_HERE);
    }

    /** The whole package is linked to the last level of the chain. */
    public void linkHere() {
        end(Ending.LINK_HERE);
    }

    /** The package is not placed automatically; it waits for a user. */
    public void none() {
        end(Ending.NONE);
    }

    private void end(Ending ending) {
        if (this.ending != null) {
            throw scriptError("ukončil zařazení dvakrát (" + this.ending + " a " + ending + ")");
        }
        this.ending = ending;
    }

    /**
     * Checks what the script decided: it ended the chain, the package is linked to a level, and
     * every level of the chain can be found again - a level without a match key would be created
     * anew for every package.
     */
    void validate() {
        if (ending == null) {
            throw scriptError("nerozhodl, kam balíček zařadit (importHere, linkHere nebo none)");
        }
        if (ending == Ending.NONE) {
            return;
        }
        if (ending == Ending.LINK_HERE && levels.isEmpty()) {
            throw scriptError("připojuje balíček (linkHere) bez úrovně, ke které by byl připojen");
        }
        for (DaMatchLevel level : levels) {
            if (level.getItems().isEmpty()) {
                throw level.scriptError("úroveň nemá žádné prvky popisu");
            }
            if (level.getMatchBy().isEmpty()) {
                throw level.scriptError("úroveň nemá klíč shody (matchBy), vznikala by pro každý balíček znovu");
            }
        }
    }

    private SystemException scriptError(String reason) {
        return new SystemException("Skript DA_MATCH " + reason, BaseCode.INVALID_STATE);
    }

    @Nullable
    Ending getEnding() {
        return ending;
    }

    List<DaMatchLevel> getLevels() {
        return Collections.unmodifiableList(levels);
    }
}
