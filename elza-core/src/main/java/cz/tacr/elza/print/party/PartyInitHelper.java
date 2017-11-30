package cz.tacr.elza.print.party;

import java.util.List;

import cz.tacr.elza.print.Record;

/**
 * Helper object for party initialization
 *
 */
public class PartyInitHelper {

    private final Record record;

    private final List<Relation> relations;

    public PartyInitHelper(Record record, List<Relation> relations) {
        this.record = record;
        this.relations = relations;
    }

    public Record getRecord() {
        return record;
    }

    public List<Relation> getRelations() {
        return relations;
    }
}
