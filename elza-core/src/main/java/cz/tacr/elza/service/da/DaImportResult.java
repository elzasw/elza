package cz.tacr.elza.service.da;

import javax.annotation.Nullable;

import cz.tacr.elza.core.data.StaticDataProvider;

/**
 * What the DA_IMPORT script decides about one div (variable {@code RESULT}): whether it becomes a
 * level, and with which items.
 */
public class DaImportResult extends DaLevelItems<DaImportResult> {

    /** What becomes of a div. */
    public enum Decision {
        /** The div becomes a level of the archival description, with the items of the result. */
        LEVEL,
        /** The div becomes no level; its digital entity is attached to the level above it. */
        ATTACH,
        /** The div is left out; the divs below it are imported as if they were in its place. */
        SKIP
    }

    private final DaImportLevel level;

    private Decision decision;

    DaImportResult(StaticDataProvider sdp, DaImportLevel level, @Nullable String eadHref) {
        super(sdp, "DA_IMPORT", eadHref);
        this.level = level;
    }

    @Override
    protected String subject() {
        return level.toString();
    }

    /** The div becomes a level. */
    public DaImportResult level() {
        decision = Decision.LEVEL;
        return this;
    }

    /** The div becomes no level; its digital entity is attached to the level above it. */
    public DaImportResult attach() {
        decision = Decision.ATTACH;
        return this;
    }

    /** The div is left out, the divs below it take its place. */
    public DaImportResult skip() {
        decision = Decision.SKIP;
        return this;
    }

    @Nullable
    Decision getDecision() {
        return decision;
    }
}
