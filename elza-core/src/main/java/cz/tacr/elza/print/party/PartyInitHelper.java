package cz.tacr.elza.print.party;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.print.Record;

/**
 * Helper object for party initialization
 */
public class PartyInitHelper {

    // we can use IdentityHashMap because output model caches relation types
    private final Map<RelationType, RelationsByType> relationTypeMap = new IdentityHashMap<>();

    private final List<Relation> relations = new ArrayList<>();

    private final List<PartyName> names = new ArrayList<>();

    private final Record ap;

    private PartyName preferredName;

    private Relation creation;

    private Relation destruction;

    public PartyInitHelper(Record ap) {
        this.ap = ap;
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

    public List<PartyName> getNames() {
        return Collections.unmodifiableList(names);
    }

    public void addName(PartyName name) {
        Validate.notNull(name);
        names.add(name);
    }

    public List<Relation> getRelations() {
        return Collections.unmodifiableList(relations);
    }

    public void addRelation(Relation relation) {
        relations.add(relation);

        // adds relation to type
        RelationType relationType = relation.getType();
        RelationsByType relationsByType = relationTypeMap.computeIfAbsent(relationType, RelationsByType::new);
        relationsByType.addRelation(relation);
    }

    public List<RelationsByType> getRelationsByType() {
        return new ArrayList<>(relationTypeMap.values());
    }
}
