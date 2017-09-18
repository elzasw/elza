package cz.tacr.elza.print.party;

import java.util.List;

import cz.tacr.elza.print.Record;

/**
 * Helper object for party initialization
 *
 */
public class PartyInitHelper {

	Record record;
	List<Relation> relations;

	public PartyInitHelper(Record record, List<Relation> rels) {
		this.record = record;
		this.relations = rels;
	}
}
