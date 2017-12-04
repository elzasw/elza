package cz.tacr.elza.print.party;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;

/**
 * Relation to other items
 *
 *
 */
public class Relation {

    private final RelationType relationType;

    private final String note;

    private final String source;

    private String textFrom;

    private String textTo;

    private String dateNoteFrom;

    private String dateNoteTo;

    private List<RelationTo> relationsTo;

    private Relation(ParRelation parRelation, RelationType relationType) {
        this.relationType = relationType;
        this.note = parRelation.getNote();
        this.source = parRelation.getSource();
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

    public String getTextFrom() {
        return textFrom;
    }

    public String getTextTo() {
        return textTo;
    }

    public String getDateNoteFrom() {
        return dateNoteFrom;
    }

    public String getDateNoteTo() {
        return dateNoteTo;
    }

    /**
     * Creates relation. From/To ParUnitdate reference is fetched during process.
     */
    public static Relation newInstance(ParRelation parRelation, RelationType relationType, List<RelationTo> relationsTo) {
        Relation relation = new Relation(parRelation, relationType);

        // set dateFrom
        ParUnitdate parFrom = parRelation.getFrom();
        if (parFrom != null) {
            relation.dateNoteFrom = parFrom.getNote();
            relation.textFrom = UnitDateConvertor.convertToString(parFrom);
        }

        // set dateTo
        ParUnitdate parTo = parRelation.getTo();
        if (parTo != null) {
            relation.dateNoteTo = parTo.getNote();
            relation.textTo = UnitDateConvertor.convertToString(parTo);
        }

        // copy relationsTo
        if (relationsTo != null) {
            relation.relationsTo = new ArrayList<>(relationsTo);
        }

        return relation;
    }
}
