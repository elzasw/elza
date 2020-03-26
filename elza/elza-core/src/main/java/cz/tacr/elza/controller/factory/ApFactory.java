package cz.tacr.elza.controller.factory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.controller.vo.ap.item.*;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.common.FactoryUtils;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.ap.ApFormVO;
import cz.tacr.elza.controller.vo.ap.ApFragmentVO;
import cz.tacr.elza.controller.vo.ap.ApStateVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeLiteVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.service.RuleService;

@Service
public class ApFactory {

    private final ApAccessPointRepository apRepository;

    private final ApStateRepository stateRepository;

    private final ApNameRepository nameRepository;

    private final ApDescriptionRepository descRepository;

    private final ApExternalIdRepository eidRepository;

    private final ScopeRepository scopeRepository;

    private final StaticDataService staticDataService;

    private final ApFragmentItemRepository fragmentItemRepository;

    private final ApNameItemRepository nameItemRepository;

    private final ApFragmentRepository fragmentRepository;

    private final ApAccessPointItemRepository accessPointItemRepository;

    private final RuleService ruleService;

    private final RuleFactory ruleFactory;

    private final PartyRepository partyRepository;

    private final ClientFactoryVO factoryVO;

    @Autowired
    public ApFactory(final ApAccessPointRepository apRepository,
                     final ApStateRepository stateRepository,
                     final ApNameRepository nameRepository,
                     final ApDescriptionRepository descRepository,
                     final ApExternalIdRepository eidRepository,
                     final ScopeRepository scopeRepository,
                     final StaticDataService staticDataService,
                     final ApFragmentItemRepository fragmentItemRepository,
                     final ApNameItemRepository nameItemRepository,
                     final ApFragmentRepository fragmentRepository,
                     final ApAccessPointItemRepository accessPointItemRepository,
                     final RuleService ruleService,
                     final RuleFactory ruleFactory,
                     final PartyRepository partyRepository,
                     final ClientFactoryVO factoryVO) {
        this.apRepository = apRepository;
        this.stateRepository = stateRepository;
        this.nameRepository = nameRepository;
        this.descRepository = descRepository;
        this.eidRepository = eidRepository;
        this.scopeRepository = scopeRepository;
        this.staticDataService = staticDataService;
        this.fragmentItemRepository = fragmentItemRepository;
        this.nameItemRepository = nameItemRepository;
        this.fragmentRepository = fragmentRepository;
        this.accessPointItemRepository = accessPointItemRepository;
        this.ruleService = ruleService;
        this.ruleFactory = ruleFactory;
        this.partyRepository = partyRepository;
        this.factoryVO = factoryVO;
    }

    /**
     * Creates simple value object from AP.
     */
    public ApRecordSimple createVOSimple(ApState apState) {
        ApAccessPoint ap = apState.getAccessPoint();
        ApName prefName = nameRepository.findPreferredNameByAccessPoint(ap);
        ApDescription desc = descRepository.findByAccessPoint(ap);
        // create VO
        ApRecordSimple vo = new ApRecordSimple();
        vo.setTypeId(apState.getApTypeId());
        vo.setId(ap.getAccessPointId());
        vo.setRecord(prefName.getFullName());
        vo.setScopeName(apState.getScope().getName());
        if (desc != null) {
            vo.setCharacteristics(desc.getDescription());
        }
        return vo;
    }

    /**
     * Creates AP from value object.
     * If id is present persist entity must be found.
     * If id is null new AP is created without create change.
     */
    public ApState create(ApAccessPointVO apVO) {
        Integer id = apVO.getId();
        if (id != null) {
            ApAccessPoint ap = apRepository.findOne(id);
            Validate.notNull(ap);
            ApState apState = stateRepository.findLastByAccessPoint(ap);
            return Validate.notNull(apState);
        }
        Validate.isTrue(!apVO.isInvalid());
        // prepare type and scope
        StaticDataProvider staticData = staticDataService.getData();
        ApType type = staticData.getApTypeById(apVO.getTypeId());
        ApScope scope = scopeRepository.findOne(apVO.getScopeId());
        // create new AP
        ApAccessPoint accessPoint = new ApAccessPoint();
        //accessPoint.setAccessPointId(accessPointId);
        //accessPoint.setCreateChange(createChange);
        //accessPoint.setDeleteChange(deleteChange);
        accessPoint.setUuid(apVO.getUuid());
        ApState apState = new ApState();
        apState.setStateApproval(ApState.StateApproval.NEW);
        apState.setApType(Validate.notNull(type));
        apState.setScope(Validate.notNull(scope));
        apState.setAccessPoint(accessPoint);
        return apState;
    }

    /**
     * Creates value object from AP. Party Id is not set.
     */
    public ApAccessPointVO createVO(ApAccessPoint accessPoint) {
        ApState apState = stateRepository.findLastByAccessPoint(accessPoint);
        return createVO(apState);
    }

    /**
     * Creates value object from AP. Party Id is not set.
     */
    public ApAccessPointVO createVO(ApState apState) {
        ApAccessPoint ap = apState.getAccessPoint();
        ApDescription desc = descRepository.findByAccessPoint(ap);
        // prepare names
        List<ApName> names = nameRepository.findByAccessPoint(ap);
        // prepare external ids
        List<ApExternalId> eids = eidRepository.findByAccessPoint(ap);
        return createVO(apState, desc, names, eids);
    }

    public List<ApStateHistoryVO> createStateHistoriesVO(final Collection<ApState> states) {
        if (CollectionUtils.isEmpty(states)) {
            return Collections.emptyList();
        }

        List<ApStateHistoryVO> results = new ArrayList<>();

        for (ApState state : states) {
            ApStateHistoryVO result = new ApStateHistoryVO();
            ApChange createChange = state.getCreateChange();
            ApScope scope = state.getScope();
            UsrUser user = createChange.getUser();
            result.setChangeDate(Date.from(createChange.getChangeDate().toInstant()));
            result.setComment(state.getComment());
            result.setType(state.getApType().getName());
            result.setUsername(user == null ? null : user.getUsername());
            result.setScope(scope.getName());
            result.setState(state.getStateApproval());
            results.add(result);
        }
        return results;
    }

    public ApAccessPointVO createVO(ApState apState, boolean fillForm) {
        ApAccessPointVO apVO = createVO(apState);
        ApType apType = apState.getApType();
        if (fillForm && apType.getRuleSystem() != null) {
            apVO.setForm(createFormVO(apState.getAccessPoint(), apType));
        }
        return apVO;
    }

    public ApAccessPointVO createVO(final ApState apState,
                                    final ApDescription desc,
                                    final List<ApName> names,
                                    final List<ApExternalId> eids) {
        StaticDataProvider staticData = staticDataService.getData();
        ApAccessPoint ap = apState.getAccessPoint();
        ApName prefName = names.get(0);
        ApRuleSystem ruleSystem = ap.getRuleSystem();
        Validate.isTrue(prefName.isPreferredName());
        List<ApAccessPointNameVO> namesVO = FactoryUtils.transformList(names, n -> ApAccessPointNameVO.newInstance(n, staticData));
        // prepare external ids
        List<ApExternalIdVO> eidsVO = FactoryUtils.transformList(eids, ApExternalIdVO::newInstance);

        // create VO
        ApAccessPointVO vo = new ApAccessPointVO();
        vo.setId(ap.getAccessPointId());
        vo.setInvalid(apState.getDeleteChange() != null);
        vo.setScopeId(apState.getScopeId());
        vo.setTypeId(apState.getApTypeId());
        vo.setComment(apState.getComment());
        vo.setStateApproval(apState.getStateApproval());
        vo.setUuid(ap.getUuid());
        vo.setExternalIds(eidsVO);
        vo.setNames(namesVO);
        vo.setErrorDescription(ap.getErrorDescription());
        vo.setRuleSystemId(ruleSystem == null ? null : ruleSystem.getRuleSystemId());
        vo.setState(ap.getState() == null ? null : ApStateVO.valueOf(ap.getState().name()));
        // vo.setPartyId(partyId);
        vo.setRecord(prefName.getFullName());
        if (desc != null) {
            vo.setCharacteristics(desc.getDescription());
        }
        vo.setPreferredNameItem(ap.getPreferredNameItem() != null ? ap.getPreferredNameItem().getItemId() : null);
        return vo;
    }

    /**
     * Create collection of VO from APs
     *
     * Function guarantees ordering of APs between input and output
     *
     * @param accessPoints
     * @return Collection of VOs
     */
    public List<ApAccessPointVO> createVO(final Collection<ApAccessPoint> accessPoints) {
        if (CollectionUtils.isEmpty(accessPoints)) {
            return Collections.emptyList();
        }

        List<ApAccessPointVO> result = new ArrayList<>(accessPoints.size());

        Map<Integer, ApState> apStateMap = stateRepository.findLastByAccessPoints(accessPoints).stream()
                .collect(Collectors.toMap(o -> o.getAccessPointId(), Function.identity()));
        Map<Integer, List<ApExternalId>> apEidsMap = eidRepository.findByAccessPoints(accessPoints).stream()
                .collect(Collectors.groupingBy(o -> o.getAccessPointId()));
        Map<Integer, ApDescription> apDescriptionMap = descRepository.findByAccessPoints(accessPoints).stream()
                .collect(Collectors.toMap(o -> o.getAccessPointId(), Function.identity()));
        Map<Integer, List<ApName>> apNamesMap = nameRepository.findByAccessPoints(accessPoints).stream()
                .collect(Collectors.groupingBy(o -> o.getAccessPointId()));

        for (ApAccessPoint accessPoint : accessPoints) {
            Integer accessPointId = accessPoint.getAccessPointId();
            ApState apState = apStateMap.get(accessPointId);
            List<ApExternalId> apExternalIds = apEidsMap.getOrDefault(accessPointId, Collections.emptyList());
            ApDescription apDescription = apDescriptionMap.get(accessPointId);
            List<ApName> apNames = apNamesMap.getOrDefault(accessPointId, Collections.emptyList());
            result.add(createVO(apState, apDescription, apNames, apExternalIds));
        }

        return result;
    }

    public ApAccessPointNameVO createVO(ApName name) {
        StaticDataProvider staticData = staticDataService.getData();
        return ApAccessPointNameVO.newInstance(name, staticData);
    }

    public ApFragmentVO createVO(final ApFragment fragment, final boolean fillForm) {
        ApFragmentVO fragmentVO = createVO(fragment);
        if (fillForm) {
            fragmentVO.setForm(createFormVO(fragment));
        }
        return fragmentVO;
    }

    public ApAccessPointNameVO createVO(final ApName name, final ApType type, boolean fillForm) {
        ApAccessPointNameVO nameVO = createVO(name);
        if (fillForm) {
            nameVO.setForm(createFormVO(name, type));
        }
        return nameVO;
    }

    public ApFragmentVO createVO(final ApFragment fragment) {
        return ApFragmentVO.newInstance(fragment);
    }

    private ApFormVO createFormVO(final ApFragment fragment) {
        List<ApItem> fragmentItems = new ArrayList<>(fragmentItemRepository.findValidItemsByFragment(fragment));
        List<RulItemTypeExt> rulItemTypes = ruleService.getFragmentItemTypesInternal(fragment.getFragmentType(), fragmentItems);

        ApFormVO form = new ApFormVO();
        form.setItemTypes(createItemTypesVO(rulItemTypes));
        form.setItems(createItemsVO(fragmentItems));
        return form;
    }

    private ApFormVO createFormVO(ApAccessPoint accessPoint, ApType apType) {
        List<ApItem> apItems = new ArrayList<>(accessPointItemRepository.findValidItemsByAccessPoint(accessPoint));
        List<RulItemTypeExt> rulItemTypes = ruleService.getApItemTypesInternal(apType, apItems, ApRule.RuleType.BODY_ITEMS);

        ApFormVO form = new ApFormVO();
        form.setItemTypes(createItemTypesVO(rulItemTypes));
        form.setItems(createItemsVO(apItems));
        return form;
    }

    private ApFormVO createFormVO(ApName name, ApType type) {
        List<ApItem> nameItems = new ArrayList<>(nameItemRepository.findValidItemsByName(name));
        List<RulItemTypeExt> rulItemTypes = ruleService.getApItemTypesInternal(type, nameItems, ApRule.RuleType.NAME_ITEMS);

        ApFormVO form = new ApFormVO();
        form.setItemTypes(createItemTypesVO(rulItemTypes));
        form.setItems(createItemsVO(nameItems));
        return form;
    }

    public List<ApItemVO> createItemsVO(final List<ApItem> apItems) {
        List<ApItemVO> items = new ArrayList<>(apItems.size());
        for (ApItem fragmentItem : apItems) {
            items.add(createItem(fragmentItem));
        }
        fillRefEntities(items);
        return items;
    }

    private List<ItemTypeLiteVO> createItemTypesVO(final List<RulItemTypeExt> rulItemTypes) {
        List<ItemTypeLiteVO> itemTypes = new ArrayList<>();
        for (RulItemTypeExt rulItemType : rulItemTypes) {
            if (rulItemType.getType() != RulItemType.Type.IMPOSSIBLE) {
                itemTypes.add(ruleFactory.createVO(rulItemType));
            }
        }
        return itemTypes;
    }

    private void fillRefEntities(final List<ApItemVO> items) {
        Map<Integer, List<ApItemAccessPointRefVO>> accessPointsMap = new HashMap<>();
        Map<Integer, List<ApItemPartyRefVO>> partyMap = new HashMap<>();
        Map<Integer, List<ApItemAPFragmentRefVO>> fragmentMap = new HashMap<>();

        for (ApItemVO item : items) {
            if (item instanceof ApItemAccessPointRefVO) {
                Integer accessPointId = ((ApItemAccessPointRefVO) item).getValue();
                if (accessPointId != null) {
                    List<ApItemAccessPointRefVO> list = accessPointsMap.computeIfAbsent(accessPointId, k -> new ArrayList<>());
                    list.add((ApItemAccessPointRefVO) item);
                }
            } else if (item instanceof ApItemPartyRefVO) {
                Integer partyId = ((ApItemPartyRefVO) item).getValue();
                if (partyId != null) {
                    List<ApItemPartyRefVO> list = partyMap.computeIfAbsent(partyId, k -> new ArrayList<>());
                    list.add((ApItemPartyRefVO) item);
                }
            } else if (item instanceof ApItemAPFragmentRefVO) {
                Integer fragmentId = ((ApItemAPFragmentRefVO) item).getValue();
                if (fragmentId != null) {
                    List<ApItemAPFragmentRefVO> list = fragmentMap.computeIfAbsent(fragmentId, k -> new ArrayList<>());
                    list.add((ApItemAPFragmentRefVO) item);
                }
            }
        }

        Set<Integer> accessPointIds = accessPointsMap.keySet();
        if (!accessPointIds.isEmpty()) {
            List<ApAccessPoint> accessPoints = apRepository.findAll(accessPointIds);
            List<ApAccessPointVO> accessPointVOList = createVO(accessPoints);
            for (ApAccessPointVO accessPointVO : accessPointVOList) {
                List<ApItemAccessPointRefVO> accessPointRefVOS = accessPointsMap.get(accessPointVO.getId());
                for (ApItemAccessPointRefVO accessPointRefVO : accessPointRefVOS) {
                    accessPointRefVO.setAccessPoint(accessPointVO);
                }
            }
        }

        Set<Integer> partyIds = partyMap.keySet();
        if (!partyIds.isEmpty()) {
            List<ParParty> parties = partyRepository.findAll(partyIds);
            List<ParPartyVO> partyVOList = factoryVO.createPartyList(parties);
            for (ParPartyVO partyVO : partyVOList) {
                List<ApItemPartyRefVO> partyRefVOS = partyMap.get(partyVO.getId());
                for (ApItemPartyRefVO partyRefVO : partyRefVOS) {
                    partyRefVO.setParty(partyVO);
                }
            }
        }

        Set<Integer> fragmentIds = fragmentMap.keySet();
        if (!fragmentIds.isEmpty()) {
            List<ApFragment> fragments = fragmentRepository.findAll(fragmentIds);
            List<ApFragmentVO> fragmentVOList = FactoryUtils.transformList(fragments, this::createVO);
            for (ApFragmentVO fragmentVO : fragmentVOList) {
                List<ApItemAPFragmentRefVO> fragmentRefVOS = fragmentMap.get(fragmentVO.getId());
                for (ApItemAPFragmentRefVO fragmentRefVO : fragmentRefVOS) {
                    fragmentRefVO.setFragment(fragmentVO);
                }
            }
        }
    }

    private ApItemVO createItem(final ApItem apItem) {
        StaticDataProvider sdp = staticDataService.getData();
        ItemType type = sdp.getItemTypeById(apItem.getItemTypeId());

        ApItemVO item;
        DataType dataType = type.getDataType();
        switch (dataType) {
            case INT:
                item = new ApItemIntVO(apItem);
                break;
            case STRING:
                item = new ApItemStringVO(apItem);
                break;
            case TEXT:
                item = new ApItemTextVO(apItem);
                break;
            case UNITDATE:
                item = new ApItemUnitdateVO(apItem);
                break;
            case UNITID:
                item = new ApItemUnitidVO(apItem);
                break;
            case FORMATTED_TEXT:
                item = new ApItemFormattedTextVO(apItem);
                break;
            case COORDINATES:
                item = new ApItemCoordinatesVO(apItem);
                break;
            case PARTY_REF:
                item = new ApItemPartyRefVO(apItem);
                break;
            case RECORD_REF:
                item = new ApItemAccessPointRefVO(apItem);
                break;
            case DECIMAL:
                item = new ApItemDecimalVO(apItem);
                break;
            case ENUM:
                item = new ApItemEnumVO(apItem);
                break;
            case JSON_TABLE:
                item = new ApItemJsonTableVO(apItem);
                break;
            case DATE:
                item = new ApItemDateVO(apItem);
                break;
            case APFRAG_REF:
                item = new ApItemAPFragmentRefVO(apItem);
                break;
            case URI_REF:
                item = new ApItemUriRefVO(apItem);
                break;
            case BIT:
                item = new ApItemBitVO(apItem);
                break;
            case STRING_50:
                item = new ApItemString50VO(apItem);
                break;
            case STRING_250:
                item = new ApItemString250VO(apItem);
                break;
            default:
                throw new NotImplementedException("Není implementováno: " + dataType.getCode());
        }

        return item;
    }

    /**
     * Creates types with theirs parent hierarchy up to root.
     *
     * @return List of root nodes which contains given types on proper parent path.
     */
    public List<ApTypeVO> createTypesWithHierarchy(Collection<ApType> types) {
        if (CollectionUtils.isEmpty(types)) {
            return Collections.emptyList();
        }

        StaticDataProvider staticData = staticDataService.getData();
        Map<Integer, ApTypeVO> typeIdVOMap = new HashMap<>();
        List<ApTypeVO> rootsVO = new ArrayList<>();

        for (ApType type : types) {
            createTypeHierarchy(type, typeIdVOMap, rootsVO, staticData);
        }

        rootsVO.sort(Comparator.comparing(ApTypeVO::getId));
        return rootsVO;
    }

    /**
     * Creates type with his parent hierarchy up to root.
     */
    private ApTypeVO createTypeHierarchy(ApType type,
                                         Map<Integer, ApTypeVO> typeIdVOMap,
                                         List<ApTypeVO> rootsVO,
                                         StaticDataProvider staticData) {
        ApTypeVO typeVO = typeIdVOMap.get(type.getApTypeId());
        if (typeVO != null) {
            return typeVO;
        }

        typeVO = ApTypeVO.newInstance(type, staticData);
        typeIdVOMap.put(typeVO.getId(), typeVO);

        if (type.getParentApTypeId() != null) {
            ApType parent = staticData.getApTypeById(type.getParentApTypeId());
            ApTypeVO parentVO = createTypeHierarchy(parent, typeIdVOMap, rootsVO, staticData);
            parentVO.addChild(typeVO);
            // TODO: parent names is needed/used on client?
            typeVO.addParent(parentVO.getName());
            typeVO.addParents(parentVO.getParents());
        } else {
            rootsVO.add(typeVO);
        }
        return typeVO;
    }

    public LanguageVO createVO(final SysLanguage language) {
        if (language == null) {
            return null;
        }
        LanguageVO languageVO = new LanguageVO();
        languageVO.setId(language.getLanguageId());
        languageVO.setCode(language.getCode());
        languageVO.setName(language.getName());
        return languageVO;
    }

    public ApEidTypeVO createVO(ApExternalIdType type) {
        if (type == null) {
            return null;
        }
        return new ApEidTypeVO(type.getExternalIdTypeId(), type.getCode(), type.getName());
    }

}
