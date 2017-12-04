package cz.tacr.elza.print.party;

import cz.tacr.elza.domain.ParRelationRoleType;

public class RelationRoleType {

    private final String code;

    private final String name;

    public RelationRoleType(ParRelationRoleType parRelationRoleType) {
        this.code = parRelationRoleType.getCode();
        this.name = parRelationRoleType.getName();
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
