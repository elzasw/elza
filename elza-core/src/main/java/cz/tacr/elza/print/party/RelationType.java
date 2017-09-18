package cz.tacr.elza.print.party;

import cz.tacr.elza.domain.ParRelationClassType;
import cz.tacr.elza.domain.ParRelationType;

/**
 * Relation type
 *
 */
public class RelationType {
	
	final String name;
	final String code;
	final RelationClassType relClassType;
	
	public RelationType(ParRelationType dbRelType)
	{
		name = dbRelType.getName();
		code = dbRelType.getCode();
		ParRelationClassType dbClassType = dbRelType.getRelationClassType();
		relClassType = RelationClassType.valueOf(dbClassType.getCode()); 
	}

	public String getName() {
		return name;
	}

	public String getCode() {
		return code;
	}

	public RelationClassType getRelClassType() {
		return relClassType;
	}

	public static RelationType newInstance(ParRelationType dbRelType) {
		RelationType relType = new RelationType(dbRelType);
		return relType;
	}

}
