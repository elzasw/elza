package cz.tacr.elza.dataexchange.output.filters.conditions;

import cz.tacr.elza.dataexchange.output.filters.FilterRuleContext;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.CachedBinding;

/**
 * Condition to validate entity class
 */
public class EntityProperties implements EntityCondition {
	/** 
	 * Expected entity class
	 */
	protected String classType;
	
	/**
	 * Flag if the entity is external (stored in external system)
	 */
	protected Boolean externalEntity;
	
	public EntityProperties() {
		
	}

	public Boolean getExternalEntity() {
		return externalEntity;
	}

	public void setExternalEntity(Boolean externalEntity) {
		this.externalEntity = externalEntity;
	}

	public String getClassType() {
		return classType;
	}

	public void setClassType(String classType) {
		this.classType = classType;
	}

	@Override
	public boolean isTrue(FilterRuleContext frCtx, CachedAccessPoint ap) {
		if(classType!=null) {
			var apType = frCtx.getStaticDataProvider().getApTypeById(ap.getApState().getApTypeId());

			if(!classType.equals(apType.getCode())) {
				return false;
			}
		}
		if(externalEntity!=null) {
			CachedBinding binding = ap.getBindings() != null?ap.getBindings().get(0):null;
			if(externalEntity && binding==null) {
				return false;
			}
			if(!externalEntity && binding!=null) {
				return false;
			}
		}
		return true;
	}	
}
