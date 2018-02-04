package cz.tacr.elza.print.party;

import cz.tacr.elza.domain.ParRelationRoleType;

public class RelationToType {

    private final String code;

    private final String name;

    public RelationToType(ParRelationRoleType parRelationRoleType) {
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
