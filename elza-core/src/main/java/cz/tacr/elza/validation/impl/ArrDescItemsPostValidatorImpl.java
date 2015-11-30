package cz.tacr.elza.validation.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.controller.RuleManager;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulDescItemConstraint;
import cz.tacr.elza.domain.RulDescItemSpecExt;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.validation.ArrDescItemsPostValidator;


/**
 * Validátor povinnosti, opakovatelnosti, hodnot atd pro atributy. Validace probíhá až po uložení všech hodnot.
 * Neslouží
 * k validaci při ukládání jedné hodnoty.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 30.11.2015
 */
@Component
public class ArrDescItemsPostValidatorImpl implements ArrDescItemsPostValidator {

    @Autowired
    private DataRepository arrDataRepository;

    @Autowired
    private RuleManager ruleManager;
    @Autowired
    private DescItemFactory descItemFactory;


    @Override
    public List<DataValidationResult> postValidateNodeDescItems(final ArrLevel level,
                                                            final ArrFindingAidVersion version) {
        List<DataValidationResult> result = new LinkedList<>();

        List<ArrData> levelData;
        if (version.getLockChange() == null) {
            levelData = arrDataRepository
                    .findByNodeAndDeleteChangeIsNull(level.getNode());
        } else {
            levelData = arrDataRepository.findByNodeAndChange(level.getNode(), version.getLockChange());
        }


        List<RulDescItemTypeExt> nodeTypes = ruleManager.getDescriptionItemTypesForNode(
                version.getFindingAidVersionId(), level.getNode().getNodeId());


        Set<RulDescItemTypeExt> requiredTypes = new HashSet<>(nodeTypes);
        Map<Integer, RulDescItemTypeExt> extNodeTypes = ElzaTools.createEntityMap(nodeTypes, type ->
                type.getDescItemTypeId());


        //rozdělení hodnot podle typu
        Map<Integer, List<ArrDescItem>> descItemsInTypeMap = new HashMap<>();

        for (ArrData arrData : levelData) {
            ArrDescItem descItem = arrData.getDescItem();
            descItem = descItemFactory.getDescItem(descItem);
            if (!extNodeTypes.containsKey(descItem.getDescItemType().getDescItemTypeId())) {
                result.add(DataValidationResult
                        .createError(descItem, "Atribut " + descItem.getDescItemType().getName()
                                + " není možné evidovat u této jednotky archivního popisu."));
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
            requiredTypes.remove(extType);

            result.addAll(postValidateDescItemsInType(descItemsInTypeMap.get(destItemTypeId), extType));
        }

        //smazání hodnot, které jsou povinné a nejsou zadány
        for (RulDescItemTypeExt requiredType : requiredTypes) {
            if (BooleanUtils.isTrue(requiredType.getRequired())) {
                result.add(DataValidationResult.createMissing(requiredType, null));
            }
        }

        return result;
    }

    /**
     * Provede validaci hodnot jednoho daného typu atributu.
     */
    private List<DataValidationResult> postValidateDescItemsInType(final List<ArrDescItem> descItemsOfType,
                                                               final RulDescItemTypeExt type) {
        Assert.notNull(type);
        Assert.notNull(descItemsOfType);

        List<DataValidationResult> validationResults = new LinkedList<>();

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
                    validationResults.add(DataValidationResult
                            .createError(descItem, "Atribut " + type.getName() + " musí mít vyplněn typ specifikace."));
                    continue;
                }

                RulDescItemSpecExt extSpec = specExtMap.get(descItem.getDescItemSpec().getDescItemSpecId());
                if (extSpec == null) {
                    validationResults.add(DataValidationResult.createError(descItem,
                            "Atribut " + type.getName() + " nemůže mít nastavenou specifikaci " + descItem
                                    .getDescItemSpec().getName()));
                    continue;
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

                validationResults.addAll(postValidateSpecConstraints(specItemsEntry.getValue(), specItemsEntry.getKey(),
                        specItemsEntry.getKey().getRulDescItemConstraintList()));
            }

            //required specifikace
            for (RulDescItemSpecExt missingSpec : missingSpecs) {
                if (BooleanUtils.isTrue(missingSpec.getRequired())) {
                    validationResults.add(DataValidationResult.createMissing(type, missingSpec));
                }
            }
        }

        postValidateTypeConstraints(descItemsOfType, type, noSpecConstraints);


        return validationResults;
    }

    /**
     * Provede validaci opakovatelnosti hodnoty atributu.
     *
     * @param repeatable true -> opakovatelný atribut
     * @param descItems  seznam hodnot daného typu nebo specifikace
     * @param type       typ
     * @return prázdný seznam nebo seznam obsahující chybu
     */
    private List<DataValidationResult> postValidateRepeatable(final boolean repeatable,
                                                          final Collection<ArrDescItem> descItems,
                                                          final RulDescItemType type) {
        if (!repeatable && CollectionUtils.size(descItems) > 1) {
            return Arrays.asList(DataValidationResult
                    .createError(descItems.iterator().next(), "Atribut " + type.getName() + " není opakovatelný."));
        }
        return Collections.EMPTY_LIST;
    }


    /**
     * Provede validaci omezení pro specifikaci.
     *
     * @param descItems       seznam hodnot dané specifikace
     * @param spec            testovaná specifikace
     * @param specConstraints seznam omezení specifikace
     * @return prázdný seznam nebo seznam chyb
     */
    private List<DataValidationResult> postValidateSpecConstraints(final List<ArrDescItem> descItems,
                                                               final RulDescItemSpecExt spec,
                                                               final List<RulDescItemConstraint> specConstraints) {
        List<DataValidationResult> result = new LinkedList<>();
        result.addAll(postValidateRepeatable(BooleanUtils.isNotFalse(spec.getRepeatable()),
                descItems, spec.getDescItemType()));

        if (CollectionUtils.isNotEmpty(specConstraints)) {
            for (ArrDescItem descItem : descItems) {

                for (RulDescItemConstraint constraint : specConstraints) {
                    result.addAll(validateDataDescItemConstraintTextLenghtLimit(descItem, constraint));
                    result.addAll(validateDataDescItemConstraintRegexp(descItem, constraint));
                }
            }
        }

        return result;
    }


    /**
     * Provede validaci omezení pro typ atributu.
     *
     * @param descItems         seznam hodnot daného atributu
     * @param type              typ atributu
     * @param noSpecConstraints seznam omezení bez specifikace
     * @return prázdný seznam nebo seznam chyb
     */
    private List<DataValidationResult> postValidateTypeConstraints(final List<ArrDescItem> descItems,
                                                               final RulDescItemTypeExt type,
                                                               final List<RulDescItemConstraint> noSpecConstraints) {
        List<DataValidationResult> result = new LinkedList<>();
        result.addAll(postValidateRepeatable(BooleanUtils.isNotFalse(type.getRepeatable()), descItems, type));

        for (ArrDescItem descItem : descItems) {
            for (RulDescItemConstraint noSpecConstraint : noSpecConstraints) {
                result.addAll(validateDataDescItemConstraintTextLenghtLimit(descItem, noSpecConstraint));
                result.addAll(validateDataDescItemConstraintRegexp(descItem, noSpecConstraint));
            }
        }

        return result;
    }


    /**
     * Pokud má typ atributu vyplněný constraint na délku textového řetězce, tak je potřeba zkontrolovat délku hodnoty
     *
     * @param descItem              Kontrolovaná data
     * @param rulDescItemConstraint Podmínka
     */
    private List<DataValidationResult> validateDataDescItemConstraintTextLenghtLimit(ArrDescItem descItem,
                                                                                 RulDescItemConstraint rulDescItemConstraint) {
        Integer textLenghtLimit = rulDescItemConstraint.getTextLenghtLimit();
        if (textLenghtLimit != null && descItem.toString().length() > textLenghtLimit) {
            return Arrays.asList(DataValidationResult.createError(descItem,
                    "Hodnota atributu " + descItem.getDescItemType().getName()
                            + " je delší než maximální povolená délka textového řetězce"));

        }

        return Collections.EMPTY_LIST;
    }

    /**
     * Pokud má typ atributu vyplněný constraint na regulární výraz, tak je potřeba hodnotu ověřit předaným regulárním
     * výrazem
     *
     * @param descItem              Kontrolovaná data
     * @param rulDescItemConstraint Podmínka
     */
    private List<DataValidationResult> validateDataDescItemConstraintRegexp(ArrDescItem descItem,
                                                                        RulDescItemConstraint rulDescItemConstraint) {
        String regexp = rulDescItemConstraint.getRegexp();
        if (regexp != null && !descItem.toString().matches(regexp)) {
            return Arrays.asList(
                    DataValidationResult
                            .createError(descItem, "Hodnota atributu " + descItem.getDescItemType().getName()
                                    + " nevyhovuje regulárnímu výrazu " + regexp));
        }
        return Collections.EMPTY_LIST;
    }

}
