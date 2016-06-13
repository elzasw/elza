package cz.tacr.elza.drools.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecExt;
import cz.tacr.elza.domain.RulItemTypeExt;

/**
 * Results of DescItemTypesRules
 * 
 * This object is used to aggregate results in Drools
 * @author Petr Pytelka
 *
 */
public class AvailableDescItems {
	List<RulItemTypeExt> descItemTypes = new ArrayList<>();
	
	public List<RulItemTypeExt> getDescItemTypes() {
		return descItemTypes;
	}
	
	/**
	 * Add type to the list of available types
	 * 
	 * @param descItemType Description item type
	 */
	public void add(RulItemTypeExt descItemType) {
		descItemTypes.add(descItemType);
	}
	
	/**
	 * Set specification type for all specifications
	 * @param descItemType	description item type
	 * @param specType		specification type
	 */
	public void setSpecTypeForAll(RulItemTypeExt descItemType, RulItemSpec.Type specType)
	{
		for( RulItemSpecExt spec: descItemType.getRulItemSpecList() )
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
		for(RulItemTypeExt descItemType: descItemTypes)
		{
			List<RulItemSpecExt> reducedList;
			reducedList = descItemType.getRulItemSpecList().stream().filter(t -> t.getType()!=null ).collect(Collectors.toList());
			descItemType.setRulItemSpecList(reducedList);
		}
	}
}
