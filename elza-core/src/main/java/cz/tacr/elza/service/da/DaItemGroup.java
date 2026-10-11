package cz.tacr.elza.service.da;

import javax.annotation.Nullable;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.RulStructuredType;

/**
 * A group of items a script plans beside the items of a level: the items of a new structured
 * object ({@link DaItems#structuredItem(String)}), or the items a level gets only when it is
 * created ({@link DaLevelItems#whenCreated()}).
 */
public class DaItemGroup extends DaItems<DaItemGroup> {

    private final String subject;
    private final RulStructuredType structuredType;

    DaItemGroup(StaticDataProvider sdp, String scriptName, @Nullable String eadHref, String subject,
                @Nullable RulStructuredType structuredType) {
        super(sdp, scriptName, eadHref);
        this.subject = subject;
        this.structuredType = structuredType;
    }

    @Override
    protected String subject() {
        return subject;
    }

    /** The structured type of the object the items belong to; null for the items of a level. */
    @Nullable
    public RulStructuredType getStructuredType() {
        return structuredType;
    }
}
