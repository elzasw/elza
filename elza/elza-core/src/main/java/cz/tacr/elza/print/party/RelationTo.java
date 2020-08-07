package cz.tacr.elza.print.party;

import cz.tacr.elza.print.Record;

public class RelationTo {

    private RelationToType roleType;

    private Record record;

    private String note;

    public RelationTo() {
        this.roleType = roleType;
        this.record = record;
    }

    public RelationToType getRoleType() {
        return roleType;
    }

    public Record getRecord() {
        return record;
    }

    public String getNote() {
        return note;
    }
}
