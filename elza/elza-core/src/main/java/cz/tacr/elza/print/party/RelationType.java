package cz.tacr.elza.print.party;

import cz.tacr.elza.domain.ParRelationType;

/**
 * Relation type
 */
public class RelationType {

    private final String name;

    private final String code;

    public RelationType(ParRelationType parRelationType) {
        this.name = parRelationType.getName();
        this.code = parRelationType.getCode();
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }
}
