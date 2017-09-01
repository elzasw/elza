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
	
	final RelationType relType;
	final String note;
	final String source;
	final String textFrom;
	final String textTo;
	final String dateNoteFrom;
	final String dateNoteTo;
	final List<RelationTo> relationsTo;
	
	private Relation(ParRelation dbRelation, RelationType relType, List<RelationTo> relsTo)
	{
		note = dbRelation.getNote();
		source = dbRelation.getSource();
		this.relType = relType;
		
		// set dateFrom
		ParUnitdate parFrom = dbRelation.getFrom();		
		if(parFrom!=null) {
			dateNoteFrom = parFrom.getNote();
			textFrom = UnitDateConvertor.convertToString(parFrom);
		} else {
			dateNoteFrom = null;
			textFrom = null;
		}
		// set dateTo
		ParUnitdate parTo = dbRelation.getTo();
		if(parTo!=null) {
			dateNoteTo = parTo.getNote();
			textTo = UnitDateConvertor.convertToString(parTo);
		} else {
			dateNoteTo = null;
			textTo = null;
		}		
		// copy relationsTo
		if(relsTo!=null) {
			relationsTo = new ArrayList<>(relsTo);
		} else {
			relationsTo = null;
		}
	}

	public List<RelationTo> getRelationsTo() {
		return relationsTo;
	}

	public RelationType getRelType() {
		return relType;
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

	public static Relation newInstance(ParRelation dbRelation, RelationType relType, List<RelationTo> relsTo) {
		Relation relation = new Relation(dbRelation, relType, relsTo);
		return relation;
	}

}
