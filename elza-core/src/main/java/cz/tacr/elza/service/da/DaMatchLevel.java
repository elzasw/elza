package cz.tacr.elza.service.da;

import javax.annotation.Nullable;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;

/**
 * One level of the chain the DA_MATCH script places a package under ({@link DaMatchResult#level()}):
 * a level that does not stand for any part of the package, e.g. a series collecting the packages
 * of one kind. It is found among the children of the level above it by its match key, or created.
 */
public class DaMatchLevel extends DaLevelItems<DaMatchLevel> {

    private final int position;

    DaMatchLevel(StaticDataProvider sdp, int position, @Nullable String eadHref) {
        super(sdp, "DA_MATCH", eadHref);
        this.position = position;
    }

    @Override
    protected String subject() {
        return "úrovně " + position + " zařazení";
    }

    /** What the level is called in the messages - its first text value. */
    String label() {
        for (Item item : getItems()) {
            if (item.data() instanceof ArrDataString s) {
                return s.getStringValue();
            }
            if (item.data() instanceof ArrDataText t) {
                return t.getTextValue();
            }
        }
        return "úroveň " + position + " zařazení";
    }
}
