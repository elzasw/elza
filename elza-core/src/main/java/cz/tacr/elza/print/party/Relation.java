package cz.tacr.elza.print.party;

import java.util.List;

import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParUnitdate;

/**
 * Relation to other items
 */
public class Relation {

    private final RelationType relationType;

    private final String note;

    private final String source;

    private final List<RelationTo> relationsTo;

    // can be proxy, initialized only when needed
    private final ParUnitdate srcFrom;

    // can be proxy, initialized only when needed
    private final ParUnitdate srcTo;

    private PartyUnitDate from;

    private PartyUnitDate to;

    private Relation(ParRelation parRelation, RelationType relationType, List<RelationTo> relationsTo) {
        this.relationType = relationType;
        this.note = parRelation.getNote();
        this.source = parRelation.getSource();
        this.relationsTo = relationsTo;
        this.srcFrom = parRelation.getFrom();
        this.srcTo = parRelation.getTo();
    }

    public List<RelationTo> getRelationsTo() {
        return relationsTo;
    }

    public RelationType getType() {
        return relationType;
    }

    public String getNote() {
        return note;
    }

    public String getSource() {
        return source;
    }

    public PartyUnitDate getFrom() {
        if (from == null && srcFrom != null) {
            from = new PartyUnitDate(srcFrom); // lazy initialization
        }
        return from;
    }

    public PartyUnitDate getTo() {
        if (to == null && srcTo != null) {
            to = new PartyUnitDate(srcTo); // lazy initialization
        }
        return to;
    }

    /**
     * Return new instance of Relation. From/To unit dates are required (fetched from database if
     * not initialized).
     */
    public static Relation newInstance(ParRelation parRelation, RelationType relationType, List<RelationTo> relationsTo) {
        Relation relation = new Relation(parRelation, relationType, relationsTo);
        return relation;
    }
}
