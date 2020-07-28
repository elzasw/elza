package cz.tacr.elza.config.rules;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import cz.tacr.elza.packageimport.xml.SettingTypeGroups.Type;

/**
 * Configuration of on group
 *
 * Group is collection of types
 */
public class GroupConfiguration {
	
	/**
	 * Name of the group
	 */
	private String name;
	
	public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
	
	/**
	 * Code of the group
	 */
	private String code;
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
	
	public GroupConfiguration(String name, String code){
		this.name = name;
		this.code = code;
	}

	/**
	 * Types in the group
	 */
    private List<TypeInfo> types = new ArrayList<>();    

    public List<TypeInfo> getTypes() {
        return types;
    }

    /**
     * Load types from XML objects
     * @param xmlTypes
     */
	public void loadTypes(List<Type> importTypes) {
		if(CollectionUtils.isEmpty(importTypes)) {
			return;
		}
		for(Type type: importTypes)
		{
			loadType(type);
		}
		
	}

	/**
	 * Load single type
	 * @param type
	 */
	private void loadType(Type type) {
        TypeInfo typeInfo = new TypeInfo();
        typeInfo.setCode(type.getCode());
        typeInfo.setWidth(type.getWidth());
        this.types.add(typeInfo);		
	}

	public TypeInfo getTypeInfo(String typeCode) {
		for(TypeInfo typeInfo: types) {
			if(typeCode.equals(typeInfo.getCode())) {
				return typeInfo;
			}
		}
		return null;
	}

	/**
	 * Return list of type codes
	 * @return
	 */
	public List<String> getTypeCodes() {
		ArrayList<String> result = new ArrayList<>();
		for(TypeInfo typeInfo: types) {
			result.add(typeInfo.getCode());
		}
		return result;
	}
}
