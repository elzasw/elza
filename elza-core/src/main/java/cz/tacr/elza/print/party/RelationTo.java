package cz.tacr.elza.print.party;

import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.print.Record;

public class RelationTo {

    private final RelationRoleType roleType;

    private final Record record;

    private final String note;

    public RelationTo(ParRelationEntity parRelationEntity, RelationRoleType roleType, Record record) {
        this.note = parRelationEntity.getNote();
        this.roleType = roleType;
        this.record = record;
    }

    public RelationRoleType getRoleType() {
        return roleType;
    }

    public Record getRecord() {
        return record;
    }

    public String getNote() {
        return note;
    }
}
