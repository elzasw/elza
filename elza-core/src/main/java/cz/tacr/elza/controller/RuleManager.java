package cz.tacr.elza.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.api.exception.ConcurrentUpdateException;
import cz.tacr.elza.domain.ArrArrangementType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDescItemExt;
import cz.tacr.elza.domain.ArrFaVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemConstraint;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemSpecExt;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulFaView;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.vo.FaViewDescItemTypes;
import cz.tacr.elza.repository.AbstractPartyRepository;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.DescItemConstraintRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.FaViewRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
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
public class RuleManager implements cz.tacr.elza.api.controller.RuleManager<RulDataType, RulDescItemType, RulDescItemSpec, RulFaView> {

    private static final String VIEW_SPECIFICATION_SEPARATOR = "|";
    private static final String VIEW_SPECIFICATION_SEPARATOR_REGEX = "\\|";

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

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private DataTypeRepository dataTypeRepository;

    @Autowired
    private AbstractPartyRepository abstractPartyRepository;

    @Autowired
    private RegRecordRepository regRecordRepository;

    @Autowired
    private NodeRepository nodeRepository;


    @Override
    @RequestMapping(value = "/getRuleSets", method = RequestMethod.GET)
    public List<RulRuleSet> getRuleSets() {
        return ruleSetRepository.findAll();
    }

    @Override
    @RequestMapping(value = "/getArrangementTypes", method = RequestMethod.GET)
    public List<ArrArrangementType> getArrangementTypes() {
        return arrangementTypeRepository.findAll();
    }

    @Override
    @RequestMapping(value = "/getDescriptionItemTypes", method = RequestMethod.GET)
    public List<RulDescItemTypeExt> getDescriptionItemTypes(
            @RequestParam(value = "ruleSetId") Integer ruleSetId) {
        List<RulDescItemType> itemTypeList = descItemTypeRepository.findAll();
        return createExt(itemTypeList);
    }

    @Override
    @RequestMapping(value = "/getDescriptionItemTypesForNodeId", method = RequestMethod.GET)
    public List<RulDescItemTypeExt> getDescriptionItemTypesForNodeId(
            @RequestParam(value = "faVersionId") Integer faVersionId,
            @RequestParam(value = "nodeId") Integer nodeId,
            @RequestParam(value = "mandatory") Boolean mandatory) {
        List<RulDescItemType> itemTypeList = descItemTypeRepository.findAll();
        return createExt(itemTypeList);
    }

    @Override
    @RequestMapping(value = "/getDescriptionItemsForAttribute", method = RequestMethod.GET)
    public List<ArrDescItemExt> getDescriptionItemsForAttribute(
            @RequestParam(value = "faVersionId") Integer faVersionId,
            @RequestParam(value = "nodeId") Integer nodeId,
            @RequestParam(value = "rulDescItemTypeId") Integer rulDescItemTypeId) {
        ArrFaVersion version = versionRepository.findOne(faVersionId);
        Assert.notNull(version);
        List<ArrDescItem> itemList;
        ArrNode node = nodeRepository.findOne(nodeId);
        Assert.notNull(node);
        if (version.getLockChange() == null) {
            itemList = descItemRepository.findByNodeAndDeleteChangeIsNullAndDescItemTypeId(node, rulDescItemTypeId);
        } else {
            itemList = descItemRepository.findByNodeDescItemTypeIdAndLockChangeId(node, rulDescItemTypeId, version.getLockChange());
        }
        return createItemExt(itemList);
    }

    @Override
    @RequestMapping(value = "/getDescItemSpecsFortDescItemType", method = RequestMethod.GET)
    public List<RulDescItemSpec> getDescItemSpecsFortDescItemType(
            @RequestBody() RulDescItemType rulDescItemType) {
        List<RulDescItemSpec> itemList = descItemSpecRepository.findByDescItemType(rulDescItemType);
        return itemList;
    }

    @Override
    @RequestMapping(value = "/getDataTypeForDescItemType", method = RequestMethod.GET)
    public RulDataType getDataTypeForDescItemType(
            @RequestBody() RulDescItemType rulDescItemType) {
        List<RulDataType> typeList = descItemTypeRepository.findRulDataType(rulDescItemType);
        return typeList.get(0);
    }

    private List<ArrDescItemExt> createItemExt(List<ArrDescItem> itemList) {
        List<ArrDescItemExt> descItemList = new LinkedList<>();
        if (itemList.isEmpty()) {
            return descItemList;
        }

        for (ArrDescItem descItem : itemList) {
            ArrDescItemExt descItemExt = new ArrDescItemExt();

            BeanUtils.copyProperties(descItem, descItemExt);

            List<ArrData> dataList = dataRepository.findByDescItem(descItem);

            if (dataList.size() != 1) {
                throw new IllegalStateException("Neplatný počet odkazujících dat (" + dataList.size() + ")");
            }

            ArrData data = dataList.get(0);
            descItemExt.setData(data.getData());
            descItemList.add(descItemExt);

            if(data instanceof ArrDataPartyRef){
                ArrDataPartyRef partyRef = (ArrDataPartyRef) data;
                descItemExt.setAbstractParty(abstractPartyRepository.findOne(partyRef.getAbstractPartyId()));
            }  else if(data instanceof ArrDataRecordRef){
                ArrDataRecordRef recordRef = (ArrDataRecordRef) data;
                descItemExt.setRecord(regRecordRepository.findOne(recordRef.getRecordId()));
            }

        }

        return descItemList;
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
    @RequestMapping(value = "/getFaViewDescItemTypes", method = RequestMethod.GET)
    public FaViewDescItemTypes getFaViewDescItemTypes(@RequestParam(value = "faVersionId") Integer faVersionId) {
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
        String[] itemTypes = itemTypesStr.split(VIEW_SPECIFICATION_SEPARATOR_REGEX);
        List<Integer> resultIdList = new LinkedList<>();
        for (String itemTypeIdStr : itemTypes) {
            resultIdList.add(Integer.valueOf(itemTypeIdStr));
        }
        final List<RulDescItemType> resultList = descItemTypeRepository.findAll(resultIdList);
        Collections.sort(resultList, new Comparator<RulDescItemType>() {

            @Override
            public int compare(RulDescItemType r1, RulDescItemType r2) {
                Integer position1 = resultIdList.indexOf(r1.getDescItemTypeId());
                Integer position2 = resultIdList.indexOf(r2.getDescItemTypeId());
                return position1.compareTo(position2);
            }

        });

        FaViewDescItemTypes result = new FaViewDescItemTypes();
        result.setRulFaView(faView);
        result.setDescItemTypes(resultList);

        return result;
    }

    @Override
    @RequestMapping(value = "/saveFaViewDescItemTypes", method = RequestMethod.PUT)
    @Transactional
    public List<Integer> saveFaViewDescItemTypes(@RequestBody RulFaView rulFaView,
                                                 @RequestParam(value = "descItemTypeIds") Integer[] descItemTypeIds) {
        Assert.notNull(rulFaView);

        Integer faViewId = rulFaView.getFaViewId();
        if (!faViewRepository.exists(faViewId)) {
            throw new ConcurrentUpdateException("Nastavení zobrazení sloupců s identifikátorem " + faViewId + " již neexistuje.");
        }

        String itemTypesStr = null;
        for (Integer itemTypeId : descItemTypeIds) {
            if (itemTypesStr == null) {
                itemTypesStr = itemTypeId.toString();
            } else {
                itemTypesStr += VIEW_SPECIFICATION_SEPARATOR + itemTypeId.toString();
            }
        }
        rulFaView.setViewSpecification(itemTypesStr);
        faViewRepository.save(rulFaView);

        return Arrays.asList(descItemTypeIds);
    }
}
