package cz.tacr.elza.print.party;

import java.util.ArrayList;
import java.util.List;

/**
 * Relations grouped by type
 *
 */
public class RelationsByType {
	private RelationType relType;
	
	final List<Relation> relations = new ArrayList<>();

	public RelationsByType(RelationType relType) {
		this.relType = relType;
	}

	public RelationType getRelationType() {
		return relType;
	}
	
	public List<Relation> getRelations() {
		return relations;
	}

	void addRelation(Relation rel) {
		relations.add(rel);
	}
}
