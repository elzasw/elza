package cz.tacr.elza.drools.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemSpecExt;
import cz.tacr.elza.domain.RulDescItemTypeExt;

/**
 * Results of DescItemTypesRules
 * 
 * This object is used to aggregate results in Drools
 * @author Petr Pytelka
 *
 */
public class AvailableDescItems {
	List<RulDescItemTypeExt> descItemTypes = new ArrayList<>();
	
	public List<RulDescItemTypeExt> getDescItemTypes() {
		return descItemTypes;
	}
	
	/**
	 * Add type to the list of available types
	 * 
	 * @param descItemType Description item type
	 */
	public void add(RulDescItemTypeExt descItemType) {
		descItemTypes.add(descItemType);
	}
	
	/**
	 * Set specification type for all specifications
	 * @param descItemType	description item type
	 * @param specType		specification type
	 */
	public void setSpecTypeForAll(RulDescItemTypeExt descItemType, RulDescItemSpec.Type specType)
	{
		for( RulDescItemSpecExt spec: descItemType.getRulDescItemSpecList() )
		{
			spec.setType(specType);
		}
	}
	
	/**
	 * Finalize list of available description items and its specification
	 */
	public void finalize()
	{
		// Remove specifications without type
		for(RulDescItemTypeExt descItemType: descItemTypes)
		{
			List<RulDescItemSpecExt> reducedList;
			reducedList = descItemType.getRulDescItemSpecList().stream().filter(t -> t.getType()!=null ).collect(Collectors.toList());
			descItemType.setRulDescItemSpecList(reducedList);
		}
	}
}
