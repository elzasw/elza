package cz.tacr.elza.validation.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.RulDescItemConstraint;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemSpecExt;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.domain.vo.DataValidationResults;

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

        List<RulDescItemConstraint> noSpecConstraints = ElzaTools.filter(type.getRulDescItemConstraintList(),
                c -> c.getDescItemSpec() == null);
        Map<Integer, RulDescItemSpecExt> specExtMap = ElzaTools.createEntityMap(type.getRulDescItemSpecList(),
                s -> s.getDescItemSpecId());
        Map<RulDescItemSpecExt, List<ArrDescItem>> specItemsMap = new HashMap<>();

        //musí mít hodnoty specifikaci?
        if (type.getUseSpecification()) {
            //seznam požadovaných,ale chybějících hodnot
            Set<RulDescItemSpecExt> missingSpecs = new HashSet<>(specExtMap.values());

            for (ArrDescItem descItem : descItemsOfType) {
                if (descItem.getDescItemSpec() == null) {
                    validationResults.createError(descItem, 
                    		"Prvek " + type.getName() + " musí mít vyplněnu specifikaci.");
                    continue;
                }

                RulDescItemSpecExt extSpec = specExtMap.get(descItem.getDescItemSpec().getDescItemSpecId());
                if (extSpec == null) {
                    validationResults.createError(descItem,
                            "Prvek " + type.getName() + " nesmí mít specifikaci " + descItem
                                    .getDescItemSpec().getName());
                    continue;
                } else if (RulDescItemSpec.Type.IMPOSSIBLE.equals(extSpec.getType())) {
                    validationResults.createErrorImpossible(descItem, "Prvek " + type.getName() + " se specifikací "
                            + extSpec.getName() + " není možné evidovat u této jednotky archivního popisu.");
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

                postValidateSpecConstraints(specItemsEntry.getValue(), specItemsEntry.getKey(),
                        specItemsEntry.getKey().getRulDescItemConstraintList());
            }

            //required specifikace
            for (RulDescItemSpecExt missingSpec : missingSpecs) {
                if (RulDescItemSpec.Type.REQUIRED.equals(missingSpec.getType())) {
                    validationResults.createMissingRequired(type, missingSpec);
                }
            }
        }

        for (ArrDescItem descItem : descItemsOfType) {
            if (RulDescItemType.Type.IMPOSSIBLE.equals(descItem.getDescItemType().getType())) {
                validationResults.createErrorImpossible(descItem, "Prvek " + descItem.getDescItemType().getName()
                        + " není možné evidovat u této jednotky archivního popisu.");
            }
        }


        postValidateTypeConstraints(descItemsOfType, type, noSpecConstraints);

    }	

    /**
     * Provede validaci omezení pro specifikaci.
     *
     * @param descItems       seznam hodnot dané specifikace
     * @param spec            testovaná specifikace
     * @param specConstraints seznam omezení specifikace
     * @return prázdný seznam nebo seznam chyb
     */
    private void postValidateSpecConstraints(final List<ArrDescItem> descItems,
                                                               final RulDescItemSpecExt spec,
                                                               final List<RulDescItemConstraint> specConstraints) {

        postValidateRepeatable(BooleanUtils.isNotFalse(spec.getRepeatable()), 
        		descItems, spec.getDescItemType());

        if (CollectionUtils.isNotEmpty(specConstraints)) {
            for (ArrDescItem descItem : descItems) {

                for (RulDescItemConstraint constraint : specConstraints) {
                    validateDataDescItemConstraintTextLenghtLimit(descItem, constraint);
                    validateDataDescItemConstraintRegexp(descItem, constraint);
                }
            }
        }
    }

    /**
     * Provede validaci omezení pro typ atributu.
     *
     * @param descItems         seznam hodnot daného atributu
     * @param type              typ atributu
     * @param noSpecConstraints seznam omezení bez specifikace
     * @return prázdný seznam nebo seznam chyb
     */
    private void postValidateTypeConstraints(final List<ArrDescItem> descItems,
                                                               final RulDescItemTypeExt type,
                                                               final List<RulDescItemConstraint> noSpecConstraints) {
        postValidateRepeatable(BooleanUtils.isNotFalse(type.getRepeatable()), descItems, type);

        for (ArrDescItem descItem : descItems) {
            for (RulDescItemConstraint noSpecConstraint : noSpecConstraints) {
                validateDataDescItemConstraintTextLenghtLimit(descItem, noSpecConstraint);
                validateDataDescItemConstraintRegexp(descItem, noSpecConstraint);
            }
        }
    }

    /**
	 * Pokud má typ atributu vyplněný constraint na délku textového řetězce, tak
	 * je potřeba zkontrolovat délku hodnoty
	 *
	 * @param descItem                 Kontrolovaná data
	 * @param rulDescItemConstraint    Podmínka
	 */
	private void validateDataDescItemConstraintTextLenghtLimit(ArrDescItem descItem,
			RulDescItemConstraint rulDescItemConstraint) {
		Integer textLenghtLimit = rulDescItemConstraint.getTextLenghtLimit();
		if (textLenghtLimit != null && descItem.toString().length() > textLenghtLimit) {
			validationResults.createError(descItem, "Hodnota atributu "
					+ descItem.getDescItemType().getName()
					+ " je delší než maximální povolená délka textového řetězce");

		}
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
					"Atribut " + type.getName() + " není opakovatelný.");
		}
	}

	/**
	 * Pokud má typ atributu vyplněný constraint na regulární výraz, tak je
	 * potřeba hodnotu ověřit předaným regulárním výrazem
	 *
	 * @param descItem              Kontrolovaná data
	 * @param rulDescItemConstraint Podmínka
	 */
	private void validateDataDescItemConstraintRegexp(ArrDescItem descItem,
			RulDescItemConstraint rulDescItemConstraint) {
		String regexp = rulDescItemConstraint.getRegexp();
		if (regexp != null && !descItem.toString().matches(regexp)) {
			validationResults.createError(descItem, "Hodnota atributu " + descItem.getDescItemType().getName()
					+ " nevyhovuje regulárnímu výrazu " + regexp);
		}
	}

	/**
	 * Append set of required types
	 * @param requiredTypes
	 */
	void writeRequiredTypes(Set<RulDescItemTypeExt> requiredTypes) {
        for (RulDescItemTypeExt requiredType : requiredTypes) {
            if (RulDescItemType.Type.REQUIRED.equals(requiredType.getType())) {
            	validationResults.createMissingRequired(requiredType, null);
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
                                + " není možný u této jednotky popisu.");
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
