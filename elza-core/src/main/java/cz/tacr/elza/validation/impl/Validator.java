package cz.tacr.elza.validation.impl;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemSpecExt;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
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
	
	List<RulDescItemTypeExt> nodeTypes;
	
	List<ArrData> levelData;
	DescItemFactory descItemFactory;

	public Validator(List<RulDescItemTypeExt> nodeTypes, List<ArrData> levelData, DescItemFactory descItemFactory) {
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
                                     final RulDescItemTypeExt type) {
        Assert.notNull(type);
        Assert.notNull(descItemsOfType);

        Map<Integer, RulDescItemSpecExt> specExtMap = ElzaTools.createEntityMap(type.getRulDescItemSpecList(),
                s -> s.getDescItemSpecId());
        Map<RulDescItemSpecExt, List<ArrDescItem>> specItemsMap = new HashMap<>();

        //musí mít hodnoty specifikaci?
        if (type.getUseSpecification()) {
            //seznam požadovaných,ale chybějících hodnot
            Set<RulDescItemSpecExt> missingSpecs = new HashSet<>(specExtMap.values());

            for (ArrDescItem descItem : descItemsOfType) {

                RulDescItemSpecExt extSpec = specExtMap.get(descItem.getDescItemSpec().getDescItemSpecId());
                if (extSpec == null) {
                    continue;
                } else if (RulDescItemSpec.Type.IMPOSSIBLE.equals(extSpec.getType())) {
                    validationResults.createErrorImpossible(descItem, "Prvek " + type.getName() + " se specifikací "
                            + extSpec.getName() + " není možné evidovat u této jednotky archivního popisu.", extSpec.getPolicyTypeCode());
                }

                List<ArrDescItem> specItems = specItemsMap.get(descItem.getDescItemSpec());
                if (specItems == null) {
                    specItems = new LinkedList<>();
                    specItemsMap.put(extSpec, specItems);
                }
                specItems.add(descItem);
            }


            for (Map.Entry<RulDescItemSpecExt, List<ArrDescItem>> specItemsEntry : specItemsMap.entrySet()) {
                missingSpecs.remove(specItemsEntry.getKey());

                postValidateRepeatable(BooleanUtils.isNotFalse(specItemsEntry.getKey().getRepeatable()),
                        specItemsEntry.getValue(), specItemsEntry.getKey().getDescItemType());
            }

            //required specifikace
            for (RulDescItemSpecExt missingSpec : missingSpecs) {
                if (RulDescItemSpec.Type.REQUIRED.equals(missingSpec.getType())) {
                    validationResults.createMissingRequired(type, missingSpec, missingSpec.getPolicyTypeCode());
                }
            }
        }

        Map<Integer, RulDescItemTypeExt> extNodeTypes = ElzaTools.createEntityMap(nodeTypes, typex ->
                typex.getDescItemTypeId());

        for (ArrDescItem descItem : descItemsOfType) {
            RulDescItemTypeExt rulDescItemTypeExt = extNodeTypes.get(descItem.getDescItemType().getDescItemTypeId());
            if (RulDescItemType.Type.IMPOSSIBLE.equals(rulDescItemTypeExt.getType())) {
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
			final RulDescItemType type) {
		if (!repeatable && CollectionUtils.size(descItems) > 1) {
			validationResults.createError(descItems.iterator().next(),
					"Atribut " + type.getName() + " není opakovatelný.", type.getPolicyTypeCode());
		}
	}

	/**
	 * Append set of required types
	 * @param requiredTypes
	 */
	void writeRequiredTypes(Set<RulDescItemTypeExt> requiredTypes) {
        for (RulDescItemTypeExt requiredType : requiredTypes) {
            if (RulDescItemType.Type.REQUIRED.equals(requiredType.getType())) {
            	validationResults.createMissingRequired(requiredType, null, requiredType.getPolicyTypeCode());
            }
        }		
	}

	/**
	 * Run the validation
	 */
	public void validate() {
        // Set of required but non existing types
        Set<RulDescItemTypeExt> requiredTypes = new HashSet<>(nodeTypes);
        Map<Integer, RulDescItemTypeExt> extNodeTypes = ElzaTools.createEntityMap(nodeTypes, type ->
                type.getDescItemTypeId());

        //rozdělení hodnot podle typu
        Map<Integer, List<ArrDescItem>> descItemsInTypeMap = new HashMap<>();

        for (ArrData arrData : levelData) {
            ArrDescItem descItem = arrData.getDescItem();
            descItem = descItemFactory.getDescItem(descItem);
            if (!extNodeTypes.containsKey(descItem.getDescItemType().getDescItemTypeId())) {
                validationResults.createError(descItem, "Prvek " + descItem.getDescItemType().getName()
                                + " není možný u této jednotky popisu.", descItem.getDescItemType().getPolicyTypeCode());
                continue;
            }

            List<ArrDescItem> itemsInType = descItemsInTypeMap.get(descItem.getDescItemType().getDescItemTypeId());
            if (itemsInType == null) {
                itemsInType = new LinkedList<>();
                descItemsInTypeMap.put(descItem.getDescItemType().getDescItemTypeId(), itemsInType);
            }
            itemsInType.add(descItem);
        }


        for (Integer destItemTypeId : descItemsInTypeMap.keySet()) {
            RulDescItemTypeExt extType = extNodeTypes.get(destItemTypeId);
            if(extType != null){
                requiredTypes.remove(extType);
                postValidateDescItemsInType(descItemsInTypeMap.get(destItemTypeId), extType);
            }
        }

        //smazání hodnot, které jsou povinné a nejsou zadány
        writeRequiredTypes(requiredTypes);		
	}
}
