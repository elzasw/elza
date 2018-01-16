package cz.tacr.elza.print.party;

import java.util.ArrayList;
import java.util.List;

/**
 * Relations grouped by type
 *
 */
public class RelationsByType {

    private final List<Relation> relations = new ArrayList<>();

    private final RelationType relationType;

    public RelationsByType(RelationType relationType) {
        this.relationType = relationType;
    }

    public RelationType getRelationType() {
        return relationType;
    }

    public List<Relation> getRelations() {
        return relations;
    }

    void addRelation(Relation relation) {
        relations.add(relation);
    }
}
