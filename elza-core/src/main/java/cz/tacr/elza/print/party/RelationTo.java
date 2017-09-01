package cz.tacr.elza.print.party;

import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.print.Record;

public class RelationTo {
	
	private final RelationToType relationToType;
	private final Record record;
	private final String note;

	private RelationTo(ParRelationEntity dbEntity, RelationToType relToType, Record record)
	{
		note = dbEntity.getNote();
		this.relationToType = relToType; 
		this.record = record;
	}

	public RelationToType getRelationToType() {
		return relationToType;
	}

	public Record getRecord() {
		return record;
	}

	public String getNote() {
		return note;
	}

	public static RelationTo newInstance(ParRelationEntity dbEntity, RelationToType relToType, Record record) {
		RelationTo relTo = new RelationTo(dbEntity, relToType, record);
		return relTo;
	}

}
