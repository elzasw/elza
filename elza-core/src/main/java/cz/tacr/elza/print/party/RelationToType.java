package cz.tacr.elza.print.party;

import cz.tacr.elza.domain.ParRelationRoleType;

public class RelationToType {
	
	private String code;
	private String name;

	private RelationToType(ParRelationRoleType roleType)
	{
		code = roleType.getCode();
		name = roleType.getName();
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public static RelationToType newInstance(ParRelationRoleType roleType) {
		RelationToType relToType = new RelationToType(roleType); 
		return relToType;
	}

}
