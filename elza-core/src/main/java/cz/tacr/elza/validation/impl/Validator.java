package cz.tacr.elza.validation.impl;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.domain.vo.DataValidationResults;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of separate validator object
 * 
 * @author Petr Pytelka
 *
 */
public class Validator {

	DataValidationResults validationResults = new DataValidationResults();
	
	List<RulItemTypeExt> nodeTypes;
	
	List<ArrData> levelData;
	DescItemFactory descItemFactory;

	public Validator(List<RulItemTypeExt> nodeTypes, List<ArrData> levelData, DescItemFactory descItemFactory) {
		this.nodeTypes = nodeTypes;
		this.levelData = levelData;
		this.descItemFactory = descItemFactory;
	}

	public DataValidationResults getValidationResults() {
		return validationResults;
	}

	public List<DataValidationResult> getValidationResultList() {
		return validationResults.getResults();
	}
	
    /**
     * Provede validaci hodnot jednoho daného typu atributu.
     */
    void postValidateDescItemsInType(final List<ArrDescItem> descItemsOfType,
                                     final RulItemTypeExt type) {
        Assert.notNull(type);
        Assert.notNull(descItemsOfType);

        Map<Integer, RulItemSpecExt> specExtMap = ElzaTools.createEntityMap(type.getRulItemSpecList(),
                s -> s.getItemSpecId());
        Map<RulItemSpecExt, List<ArrDescItem>> specItemsMap = new HashMap<>();

        //musí mít hodnoty specifikaci?
        if (type.getUseSpecification()) {
            //seznam požadovaných,ale chybějících hodnot
            Set<RulItemSpecExt> missingSpecs = new HashSet<>(specExtMap.values());

            for (ArrDescItem descItem : descItemsOfType) {

                RulItemSpecExt extSpec = specExtMap.get(descItem.getItemSpec().getItemSpecId());
                if (extSpec == null) {
                    continue;
                } else if (RulItemSpec.Type.IMPOSSIBLE.equals(extSpec.getType())) {
                    validationResults.createErrorImpossible(descItem, "Prvek " + type.getName() + " se specifikací "
                            + extSpec.getName() + " není možné evidovat u této jednotky archivního popisu.", extSpec.getPolicyTypeCode());
                }

                List<ArrDescItem> specItems = specItemsMap.get(descItem.getItemSpec());
                if (specItems == null) {
                    specItems = new LinkedList<>();
                    specItemsMap.put(extSpec, specItems);
                }
                specItems.add(descItem);
            }


            for (Map.Entry<RulItemSpecExt, List<ArrDescItem>> specItemsEntry : specItemsMap.entrySet()) {
                missingSpecs.remove(specItemsEntry.getKey());

                postValidateRepeatable(BooleanUtils.isNotFalse(specItemsEntry.getKey().getRepeatable()),
                        specItemsEntry.getValue(), specItemsEntry.getKey().getItemType());
            }

            //required specifikace
            for (RulItemSpecExt missingSpec : missingSpecs) {
                if (RulItemSpec.Type.REQUIRED.equals(missingSpec.getType())) {
                    validationResults.createMissingRequired(type, missingSpec, missingSpec.getPolicyTypeCode());
                }
            }
        }

        Map<Integer, RulItemTypeExt> extNodeTypes = ElzaTools.createEntityMap(nodeTypes, typex ->
                typex.getItemTypeId());

        for (ArrDescItem descItem : descItemsOfType) {
            RulItemTypeExt rulDescItemTypeExt = extNodeTypes.get(descItem.getItemType().getItemTypeId());
            if (RulItemType.Type.IMPOSSIBLE.equals(rulDescItemTypeExt.getType())) {
                validationResults.createErrorImpossible(descItem, "Prvek " + rulDescItemTypeExt.getName()
                        + " není možné evidovat u této jednotky archivního popisu.", rulDescItemTypeExt.getPolicyTypeCode());
            }
        }


        postValidateRepeatable(BooleanUtils.isNotFalse(type.getRepeatable()), descItemsOfType, type);
    }	

	/**
	 * Provede validaci opakovatelnosti hodnoty atributu.
	 *
	 * @param repeatable   true -> opakovatelný atribut
	 * @param descItems    seznam hodnot daného typu nebo specifikace
	 * @param type         typ
	 */
	private void postValidateRepeatable(final boolean repeatable, final Collection<ArrDescItem> descItems,
			final RulItemType type) {
		if (!repeatable && CollectionUtils.size(descItems) > 1) {
			validationResults.createError(descItems.iterator().next(),
					"Atribut " + type.getName() + " není opakovatelný.", type.getPolicyTypeCode());
		}
	}

	/**
	 * Append set of required types
	 * @param requiredTypes
	 */
	void writeRequiredTypes(Set<RulItemTypeExt> requiredTypes) {
        for (RulItemTypeExt requiredType : requiredTypes) {
            if (RulItemType.Type.REQUIRED.equals(requiredType.getType())) {
            	validationResults.createMissingRequired(requiredType, null, requiredType.getPolicyTypeCode());
            }
        }		
	}

	/**
	 * Run the validation
	 */
	public void validate() {
        // Set of required but non existing types
        Set<RulItemTypeExt> requiredTypes = new HashSet<>(nodeTypes);
        Map<Integer, RulItemTypeExt> extNodeTypes = ElzaTools.createEntityMap(nodeTypes, type ->
                type.getItemTypeId());

        //rozdělení hodnot podle typu
        Map<Integer, List<ArrDescItem>> descItemsInTypeMap = new HashMap<>();

        for (ArrData arrData : levelData) {
            ArrDescItem descItem = (ArrDescItem) arrData.getItem();
            descItem = descItemFactory.getDescItem(descItem);
            if (!extNodeTypes.containsKey(descItem.getItemType().getItemTypeId())) {
                validationResults.createError(descItem, "Prvek " + descItem.getItemType().getName()
                                + " není možný u této jednotky popisu.", descItem.getItemType().getPolicyTypeCode());
                continue;
            }

            List<ArrDescItem> itemsInType = descItemsInTypeMap.get(descItem.getItemType().getItemTypeId());
            if (itemsInType == null) {
                itemsInType = new LinkedList<>();
                descItemsInTypeMap.put(descItem.getItemType().getItemTypeId(), itemsInType);
            }
            itemsInType.add(descItem);
        }


        for (Integer destItemTypeId : descItemsInTypeMap.keySet()) {
            RulItemTypeExt extType = extNodeTypes.get(destItemTypeId);
            if(extType != null){
                requiredTypes.remove(extType);
                postValidateDescItemsInType(descItemsInTypeMap.get(destItemTypeId), extType);
            }
        }

        //smazání hodnot, které jsou povinné a nejsou zadány
        writeRequiredTypes(requiredTypes);		
	}
}
