package cz.tacr.elza.validation.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.ApStateRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.domain.vo.DataValidationResults;
import cz.tacr.elza.service.ArrangementService;

/**
 * Implementation of separate validator object
 */
public class Validator {

    DataValidationResults validationResults = new DataValidationResults();

    /**
     * List of required types
     */
    final List<RulItemTypeExt> requiredItemTypes;

    /**
     * List of current description items
     */
    final List<ArrDescItem> descItems;

    final DescItemFactory descItemFactory;

    private final ApStateRepository stateRepository;

    public Validator(final List<RulItemTypeExt> requiredItemTypes,
                     final List<ArrDescItem> descItems,
                     final DescItemFactory descItemFactory,
                     final ApStateRepository stateRepository) {
        this.requiredItemTypes = requiredItemTypes;
        if (descItems == null) {
            this.descItems = Collections.emptyList();
        } else {
            this.descItems = descItems;
        }
		this.descItemFactory = descItemFactory;
        this.stateRepository = stateRepository;
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
        Assert.notNull(type, "Typ musí být vyplněn");
        Assert.notNull(descItemsOfType, "Seznam hodnot atributů musí být vyplněn");

        Map<Integer, RulItemSpecExt> specExtMap = ElzaTools.createEntityMap(type.getRulItemSpecList(), RulItemSpec::getItemSpecId);
        Map<RulItemSpecExt, List<ArrDescItem>> specItemsMap = new HashMap<>();

        for (ArrDescItem descItem : descItemsOfType) {
            if (descItem.isUndefined() && !type.getIndefinable()) {
                validationResults.createError(descItem, "U prvku popisu " + descItem.getItemType().getName()
                        + " není možné nastavit hodnotu '" + ArrangementService.UNDEFINED + "'.", type.getPolicyTypeCode());
            }
        }

        //musí mít hodnoty specifikaci?
        if (type.getUseSpecification()) {
            //seznam požadovaných,ale chybějících hodnot
            Set<RulItemSpecExt> missingSpecs = new HashSet<>(specExtMap.values());

            for (ArrDescItem descItem : descItemsOfType) {

                RulItemSpec itemSpec = descItem.getItemSpec();
                if (itemSpec == null) {
                    throw new IllegalStateException(
                            "Missing item spec for itemTypeCode:" + type.getCode() + ", descItemId:" + descItem.getItemId());
                }
                RulItemSpecExt extSpec = specExtMap.get(itemSpec.getItemSpecId());
                if (extSpec == null) {
                    continue;
                } else if (RulItemSpec.Type.IMPOSSIBLE.equals(extSpec.getType())) {
                    validationResults.createErrorImpossible(descItem, "Prvek " + type.getName() + " se specifikací "
                            + extSpec.getName() + " není možné evidovat u této jednotky archivního popisu.",
                                                            extSpec.getPolicyTypeCode());
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

        Map<Integer, RulItemTypeExt> extNodeTypes = ElzaTools.createEntityMap(requiredItemTypes, typex ->
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
        Map<Integer, RulItemTypeExt> extNodeTypes = ElzaTools.createEntityMap(requiredItemTypes,
                RulItemType::getItemTypeId);

        //rozdělení hodnot podle typu
        Map<Integer, List<ArrDescItem>> descItemsInTypeMap = new HashMap<>();

        if (descItems != null) {

            for (ArrDescItem descItem : descItems) {

                ArrData data = descItem.getData();
                RulItemType itemType = descItem.getItemType();
                Integer itemTypeId = itemType.getItemTypeId();
                String name = itemType.getName();
                String policyTypeCode = extNodeTypes.get(itemTypeId).getPolicyTypeCode();

                if (data instanceof ArrDataRecordRef) {
                    ApAccessPoint accessPoint = ((ArrDataRecordRef) data).getRecord();
                    ApState apState = stateRepository.findLastByAccessPoint(accessPoint);
                    if (apState.getStateApproval() != ApState.StateApproval.APPROVED) {
                        validationResults.createError(descItem, "Prvek " + name + " není schválený.", policyTypeCode);
                        continue;
                    }
                }

                if (!extNodeTypes.containsKey(itemTypeId)) {
                    validationResults.createError(descItem, "Prvek " + name + " není možný u této jednotky popisu.", policyTypeCode);
                    continue;
                }

                List<ArrDescItem> itemsInType = descItemsInTypeMap.computeIfAbsent(itemTypeId, k -> new LinkedList<>());
                itemsInType.add(descItem);
            }
        }

        // Set of required but non existing types
        Set<RulItemTypeExt> requiredTypes = new HashSet<>(requiredItemTypes);
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
