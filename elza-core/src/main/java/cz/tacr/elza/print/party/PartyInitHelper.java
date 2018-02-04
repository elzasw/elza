package cz.tacr.elza.print.party;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.print.Record;

/**
 * Helper object for party initialization
 */
public class PartyInitHelper {

    private final Record ap;

    private PartyName preferredName;

    private final List<PartyName> names = new ArrayList<>();

    private Relation creation;

    private Relation destruction;

    private List<Relation> relations;

    private List<RelationsByType> relationsByTypeList;

    public PartyInitHelper(Record ap) {
        this.ap = Validate.notNull(ap);
    }

    public Record getAP() {
        return ap;
    }

    public PartyName getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(PartyName preferredName) {
        this.preferredName = preferredName;
    }

    public List<PartyName> getNames() {
        return names;
    }

    public void addName(PartyName name) {
        Validate.notNull(name);
        names.add(name);
    }

    public Relation getCreation() {
        return creation;
    }

    public void setCreation(Relation creation) {
        this.creation = creation;
    }

    public Relation getDestruction() {
        return destruction;
    }

    public void setDestruction(Relation destruction) {
        this.destruction = destruction;
    }

    public List<Relation> getRelations() {
        return relations;
    }

    public List<RelationsByType> getRelationsByType() {
        return relationsByTypeList;
    }

    public void addRelation(Relation relation) {
        Validate.notNull(relation);

        if (relations == null) {
            relations = new ArrayList<>();
        }
        relations.add(relation);

        RelationsByType relByType = getRelationsByType(relation.getType());
        relByType.addRelation(relation);
    }

    private RelationsByType getRelationsByType(RelationType type) {
        if (relationsByTypeList == null) {
            relationsByTypeList = new ArrayList<>();
        } else {
            // find existing by relation type
            for (RelationsByType relByType : relationsByTypeList) {
                if (relByType.getRelationType() == type) {
                    return relByType;
                }
            }
        }
        // create new
        RelationsByType relByType = new RelationsByType(type);
        relationsByTypeList.add(relByType);
        return relByType;
    }
}
