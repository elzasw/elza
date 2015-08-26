package cz.tacr.elza.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.domain.ArrArrangementType;
import cz.tacr.elza.domain.ArrFaVersion;
import cz.tacr.elza.domain.RulDescItemConstraint;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemSpecExt;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulFaView;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.DescItemConstraintRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.FaViewRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.VersionRepository;

/**
 * API pro práci s pravidly.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 30. 7. 2015
 */
@RestController
@RequestMapping("/api/ruleSetManager")
public class RuleManager implements cz.tacr.elza.api.controller.RuleManager {

    private static final String VIEW_SPECIFICATION_SEPARATOR = ";";

    @Autowired
    private RuleSetRepository ruleSetRepository;

    @Autowired
    private DescItemTypeRepository descItemTypeRepository;

    @Autowired
    private DescItemSpecRepository descItemSpecRepository;

    @Autowired
    private DescItemConstraintRepository descItemConstraintRepository;

    @Autowired
    private ArrangementTypeRepository arrangementTypeRepository;

    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    private FaViewRepository faViewRepository;

    @Override
    @RequestMapping(value = "/getRuleSets", method = RequestMethod.GET)
    public List<RulRuleSet> getRuleSets() {
        return ruleSetRepository.findAll();
    }

    @Override
    @RequestMapping(value = "/getArrangementTypes", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrArrangementType> getArrangementTypes() {
        return arrangementTypeRepository.findAll();
    }

    @Override
    @RequestMapping(value = "/getDescriptionItemTypes", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RulDescItemTypeExt> getDescriptionItemTypes(
            @RequestParam(value = "ruleSetId") Integer ruleSetId) {
        List<RulDescItemType> itemTypeList = descItemTypeRepository.findAll();
        return createExt(itemTypeList);
    }

    @Override
    @RequestMapping(value = "/getDescriptionItemTypesForNodeId", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RulDescItemTypeExt> getDescriptionItemTypesForNodeId(
            @RequestParam(value = "faVersionId") Integer faVersionId,
            @RequestParam(value = "nodeId") Integer nodeId,
            @RequestParam(value = "mandatory") Boolean mandatory) {
        List<RulDescItemType> itemTypeList = descItemTypeRepository.findAll();
        return createExt(itemTypeList);
    }

    private List<RulDescItemTypeExt> createExt(final List<RulDescItemType> itemTypeList) {
        if (itemTypeList.isEmpty()) {
            return new LinkedList<>();
        }

        List<RulDescItemSpec> listDescItem = descItemSpecRepository.findByItemTypeIds(itemTypeList);
        Map<Integer, List<RulDescItemSpec>> itemSpecMap =
                ElzaTools.createGroupMap(listDescItem, p -> p.getDescItemType().getDescItemTypeId());

        List<RulDescItemConstraint> findItemConstList =
                descItemConstraintRepository.findByItemTypeIds(itemTypeList);
        Map<Integer, List<RulDescItemConstraint>> itemConstrainMap =
                ElzaTools.createGroupMap(findItemConstList, p -> p.getDescItemType().getDescItemTypeId());

        List<RulDescItemConstraint> findItemSpecConstList =
                descItemConstraintRepository.findByItemSpecIds(listDescItem);
        Map<Integer, List<RulDescItemConstraint>> itemSpecConstrainMap =
                ElzaTools.createGroupMap(findItemSpecConstList, p -> p.getDescItemSpec().getDescItemSpecId());

        List<RulDescItemTypeExt> result = new LinkedList<>();
        for (RulDescItemType rulDescItemType : itemTypeList) {
            RulDescItemTypeExt descItemTypeExt = new RulDescItemTypeExt();
            BeanUtils.copyProperties(rulDescItemType, descItemTypeExt);
            List<RulDescItemSpec> itemSpecList =
                    itemSpecMap.get(rulDescItemType.getDescItemTypeId());
            if (itemSpecList != null) {
                for (RulDescItemSpec rulDescItemSpec : itemSpecList) {
                    RulDescItemSpecExt descItemSpecExt = new RulDescItemSpecExt();
                    BeanUtils.copyProperties(rulDescItemSpec, descItemSpecExt);
                    descItemTypeExt.getRulDescItemSpecList().add(descItemSpecExt);
                    List<RulDescItemConstraint> itemConstrainList =
                            itemSpecConstrainMap.get(rulDescItemSpec.getDescItemSpecId());
                    if (itemConstrainList != null) {
                        descItemSpecExt.getRulDescItemConstraintList().addAll(itemConstrainList);
                    }
                }
            }
            List<RulDescItemConstraint> itemConstrainList =
                    itemConstrainMap.get(rulDescItemType.getDescItemTypeId());
            if (itemConstrainList != null) {
                descItemTypeExt.getRulDescItemConstraintList().addAll(itemConstrainList);
            }
            result.add(descItemTypeExt);
        }

        return result;
    }

    @Override
    @RequestMapping(value = "/getFaViewDescItemTypes", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Integer> getFaViewDescItemTypes(@RequestParam(value = "faVersionId") Integer faVersionId) {
        Assert.notNull(faVersionId);
        ArrFaVersion version = versionRepository.getOne(faVersionId);
        RulRuleSet ruleSet = version.getRuleSet();
        ArrArrangementType arrangementType = version.getArrangementType();

        List<RulFaView> faViewList =
                faViewRepository.findByRuleSetAndArrangementType(ruleSet, arrangementType);
        if (faViewList.size() > 1) {
            throw new IllegalStateException("Bylo nalezeno více záznamů (" + faViewList.size()
                    + ") podle RuleSetId " + ruleSet.getRuleSetId() + " a ArrangementTypeId "
                    + arrangementType.getArrangementTypeId());
        } else if (faViewList.isEmpty()) {
            throw new IllegalStateException(
                    "Nebyl nalezen záznam podle RuleSetId " + ruleSet.getRuleSetId()
                            + " a ArrangementTypeId " + arrangementType.getArrangementTypeId());
        }
        RulFaView faView = faViewList.get(0);

        String itemTypesStr = faView.getViewSpecification();
        String[] itemTypes = itemTypesStr.split(VIEW_SPECIFICATION_SEPARATOR);
        List<Integer> resultList = new LinkedList<>();
        for (String itemTypeIdStr : itemTypes) {
            resultList.add(Integer.valueOf(itemTypeIdStr));
        }
        return resultList;
    }

    @Override
    @RequestMapping(value = "/saveFaViewDescItemTypes", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Integer> saveFaViewDescItemTypes(@RequestParam(value = "ruleSetId") Integer ruleSetId,
                                                 @RequestParam(value = "arrangementTypeId") Integer arrangementTypeId,
                                                 @RequestParam(value = "descItemTypeIds") Integer[] descItemTypeIds) {
        Assert.notNull(ruleSetId);
        Assert.notNull(arrangementTypeId);
        RulRuleSet ruleSet = ruleSetRepository.getOne(ruleSetId);
        ArrArrangementType arrangementType = arrangementTypeRepository.getOne(arrangementTypeId);
        List<RulFaView> faViewList =
                faViewRepository.findByRuleSetAndArrangementType(ruleSet, arrangementType);

        String itemTypesStr = null;
        for (Integer itemTypeId : descItemTypeIds) {
            if (itemTypesStr == null) {
                itemTypesStr = itemTypeId.toString();
            } else {
                itemTypesStr += VIEW_SPECIFICATION_SEPARATOR + itemTypeId.toString();
            }
        }

        RulFaView faView = null;
        if (faViewList.size() > 1) {
            throw new IllegalStateException("Bylo nalezeno více záznamů (" + faViewList.size()
                    + ") podle RuleSetId " + ruleSet.getRuleSetId() + " a ArrangementTypeId "
                    + arrangementType.getArrangementTypeId());
        } else if (faViewList.isEmpty()) {
            faView = new RulFaView();
            faView.setArrangementType(arrangementType);
            faView.setRuleSet(ruleSet);
            faView.setViewSpecification(itemTypesStr);
            faViewRepository.save(faView);
        } else {
            faView = faViewList.get(0);
            faView.setViewSpecification(itemTypesStr);
            faViewRepository.save(faView);
        }

        return Arrays.asList(descItemTypeIds);
    }
}
