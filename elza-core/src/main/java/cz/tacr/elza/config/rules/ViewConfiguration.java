package cz.tacr.elza.config.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.packageimport.xml.SettingTypeGroups.Group;

/**
 * Single view configuration 
 *
 */
public class ViewConfiguration {
	
	protected List<GroupConfiguration> groups = new ArrayList<>();
	
	/**
	 * Lookup table for typeCode to group
	 */
	Map<String, GroupConfiguration> typeCodeLookup;
	
	public ViewConfiguration(List<Group> inputGroups) {
		loadGroups(inputGroups);
	}

	/**
	 * Default constructor
	 */
	public ViewConfiguration() {
	}

	private void loadGroups(List<Group> inputGroups) {
		// check parameter
		if(inputGroups==null) {
			return;
		}
		
		// Order of groups is same as order in list
		inputGroups.forEach(group->{
			groups.add(loadGroup(group));
		});
		
		prepareLookupTable();
	}
	
	/**
	 * Prepare internal lookup table
	 */
	private void prepareLookupTable() {
		
		// Prepare lookup table for descItem code to group
		typeCodeLookup = new HashMap<>();
		
		groups.forEach(g -> {
			g.getTypes().forEach(t -> {
				typeCodeLookup.put(t.getCode(), g);
			});
		});
	}

	private GroupConfiguration loadGroup(Group inputGroup) {
        GroupConfiguration groupConf = new GroupConfiguration(inputGroup.getName(), inputGroup.getCode());
        groupConf.loadTypes(inputGroup.getTypes());
        return groupConf;
	}

	/**
	 * Return list of group codes
	 * @return
	 */
	public Collection<? extends String> getGroupCodes() {
		ArrayList<String> codes = new ArrayList<>(groups.size());
		for(GroupConfiguration c: groups) {
			codes.add(c.getCode());
		}
		return codes;
	}

	/**
	 * Check if group includes configuration for given description item type
	 * @param typeCode
	 * @return
	 */
	public GroupConfiguration getGroupForType(String typeCode) {
		GroupConfiguration c = typeCodeLookup.get(typeCode);
		if(c!=null) {
			return c;
		}
		return null;
	}

	/**
	 * Return group configuration by group code
	 * @param groupCode
	 * @return
	 */
	public GroupConfiguration getGroup(String groupCode) {
		for(GroupConfiguration c: groups) {
			if(groupCode.equals(c.getCode())) {
				return c;
			}
		}
		return null;
	}

	public int getTypeWidthByCode(String typeCode) {
		GroupConfiguration c = typeCodeLookup.get(typeCode);
		if(c!=null) {
			TypeInfo typeInfo = c.getTypeInfo(typeCode);
			if(typeInfo!=null) {
				return typeInfo.getWidth();
			}			
		}
		// by default return 1
		return 1;
	}

	public List<GroupConfiguration> getGroups() {
		return groups;
	}
}
