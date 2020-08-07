package cz.tacr.elza.print.party;

import java.util.List;

/**
 * Relation to other items
 */
public class Relation {

    private RelationType relationType;

    private String note;

    private String source;

    private List<RelationTo> relationsTo;

    private PartyUnitDate from;

    private PartyUnitDate to;

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
        return from;
    }

    public PartyUnitDate getTo() {
        return to;
    }

}
