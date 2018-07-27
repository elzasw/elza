package cz.tacr.elza.controller.factory;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.controller.vo.ap.ApFormVO;
import cz.tacr.elza.controller.vo.ap.ApFragmentTypeVO;
import cz.tacr.elza.controller.vo.ap.ApFragmentVO;
import cz.tacr.elza.controller.vo.ap.ApStateVO;
import cz.tacr.elza.controller.vo.ap.item.*;
import cz.tacr.elza.controller.vo.nodes.ItemTypeLiteVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.RuleService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ApFactory {

    private final ApAccessPointRepository apRepository;

    private final ApNameRepository nameRepository;

    private final ApDescriptionRepository descRepository;

    private final ApExternalIdRepository eidRepository;

    private final ScopeRepository scopeRepository;

    private final StaticDataService staticDataService;

    private final ApFragmentItemRepository fragmentItemRepository;

    private final ApNameItemRepository nameItemRepository;

    private final ApFragmentRepository fragmentRepository;

    private final ApBodyItemRepository bodyItemRepository;

    private final RuleService ruleService;

    private final RuleFactory ruleFactory;

    private final PartyRepository partyRepository;

    private final ClientFactoryVO factoryVO;

    @Autowired
    public ApFactory(final ApAccessPointRepository apRepository,
                     final ApNameRepository nameRepository,
                     final ApDescriptionRepository descRepository,
                     final ApExternalIdRepository eidRepository,
                     final ScopeRepository scopeRepository,
                     final StaticDataService staticDataService,
                     final ApFragmentItemRepository fragmentItemRepository,
                     final ApNameItemRepository nameItemRepository,
                     final ApFragmentRepository fragmentRepository,
                     final ApBodyItemRepository bodyItemRepository,
                     final RuleService ruleService,
                     final RuleFactory ruleFactory,
                     final PartyRepository partyRepository,
                     final ClientFactoryVO factoryVO) {
        this.apRepository = apRepository;
        this.nameRepository = nameRepository;
        this.descRepository = descRepository;
        this.eidRepository = eidRepository;
        this.scopeRepository = scopeRepository;
        this.staticDataService = staticDataService;
        this.fragmentItemRepository = fragmentItemRepository;
        this.nameItemRepository = nameItemRepository;
        this.fragmentRepository = fragmentRepository;
        this.bodyItemRepository = bodyItemRepository;
        this.ruleService = ruleService;
        this.ruleFactory = ruleFactory;
        this.partyRepository = partyRepository;
        this.factoryVO = factoryVO;
    }

    /**
     * Creates simple value object from AP.
     */
    public ApRecordSimple createVOSimple(ApAccessPoint ap) {
        ApName prefName = nameRepository.findPreferredNameByAccessPoint(ap);
        ApDescription desc = descRepository.findByAccessPoint(ap);
        // create VO
        ApRecordSimple vo = new ApRecordSimple();
        vo.setApTypeId(ap.getApTypeId());
        vo.setId(ap.getAccessPointId());
        vo.setRecord(prefName.getFullName());
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
    public ApAccessPoint create(ApAccessPointVO apVO) {
        Integer id = apVO.getId();
        if (id != null) {
            ApAccessPoint ap = apRepository.findOne(id);
            return Validate.notNull(ap);
        }
        Validate.isTrue(!apVO.isInvalid());
        // prepare type and scope
        StaticDataProvider staticData = staticDataService.getData();
        ApType type = staticData.getApTypeById(apVO.getTypeId());
        ApScope scope = scopeRepository.findOne(apVO.getScopeId());
        // create new AP
        ApAccessPoint entity = new ApAccessPoint();
        //entity.setAccessPointId(accessPointId);
        entity.setApType(Validate.notNull(type));
        //entity.setCreateChange(createChange);
        //entity.setDeleteChange(deleteChange);
        entity.setScope(Validate.notNull(scope));
        entity.setUuid(apVO.getUuid());
        return entity;
    }

    /**
     * Creates value object from AP. Party Id is not set.
     */
    public ApAccessPointVO createVO(ApAccessPoint ap) {
        ApDescription desc = descRepository.findByAccessPoint(ap);
        // prepare names
        List<ApName> names = nameRepository.findByAccessPoint(ap);
        // prepare external ids
        List<ApExternalId> eids = eidRepository.findByAccessPoint(ap);
        return createVO(ap, desc, names, eids);
    }

    public ApAccessPointVO createVO(final ApAccessPoint ap, final boolean fillForm) {
        ApAccessPointVO apVO = createVO(ap);
        ApType apType = ap.getApType();
        if (fillForm && apType.getRuleSystem() != null) {
            apVO.setForm(createFormVO(ap));
        }
        return apVO;
    }

    public ApAccessPointVO createVO(final ApAccessPoint ap,
                                    final ApDescription desc,
                                    final List<ApName> names,
                                    final List<ApExternalId> eids) {
        StaticDataProvider staticData = staticDataService.getData();
        ApName prefName = names.get(0);
        Validate.isTrue(prefName.isPreferredName());
        List<ApAccessPointNameVO> namesVO = transformList(names, n -> ApAccessPointNameVO.newInstance(n, staticData));
        // prepare external ids
        List<ApExternalIdVO> eidsVO = transformList(eids, ApExternalIdVO::newInstance);
        // create VO
        ApAccessPointVO vo = new ApAccessPointVO();
        vo.setExternalIds(eidsVO);
        vo.setId(ap.getAccessPointId());
        vo.setInvalid(ap.getDeleteChange() != null);
        vo.setNames(namesVO);
        vo.setErrorDescription(ap.getErrorDescription());
        vo.setState(ap.getState() == null ? null : ApStateVO.valueOf(ap.getState().name()));
        // vo.setPartyId(partyId);
        vo.setRecord(prefName.getFullName());
        vo.setScopeId(ap.getScopeId());
        vo.setTypeId(ap.getApTypeId());
        vo.setUuid(ap.getUuid());
        if (desc != null) {
            vo.setCharacteristics(desc.getDescription());
        }

        // TODO: remove these properties from client
        //result.setExternalSystem();
        //result.setNote();
        //result.setVersion();
        //result.setParents();
        // TODO: client must read AP type from storage by typeId
        //result.setTypesToRoot();
        //result.setAddRecord();
        // TODO: change to more detail history (undo) overview
        //result.setLastUpdate(findLastUpdate(accessPointData));

        return vo;
    }

    public List<ApAccessPointVO> createVO(final Collection<ApAccessPoint> accessPoints) {
        if (accessPoints == null) {
            return null;
        }

        if (accessPoints.size() == 0) {
            return Collections.emptyList();
        }

        List<ApAccessPointVO> result = new ArrayList<>(accessPoints.size());

        Map<Integer, List<ApExternalId>> apEidsMap = eidRepository.findByAccessPoints(accessPoints).stream()
                .collect(Collectors.groupingBy(ApExternalId::getAccessPointId));
        Map<Integer, ApDescription> apDescriptionMap = descRepository.findByAccessPoints(accessPoints).stream()
                .collect(Collectors.toMap(ApDescription::getAccessPointId, Function.identity()));
        Map<Integer, List<ApName>> apNamesMap = nameRepository.findByAccessPoints(accessPoints).stream()
                .collect(Collectors.groupingBy(ApName::getAccessPointId));

        for (ApAccessPoint accessPoint : accessPoints) {
            Integer id = accessPoint.getAccessPointId();
            List<ApExternalId> apExternalIds = apEidsMap.getOrDefault(id, Collections.emptyList());
            ApDescription apDescription = apDescriptionMap.get(id);
            List<ApName> apNames = apNamesMap.getOrDefault(id, Collections.emptyList());
            result.add(createVO(accessPoint, apDescription, apNames, apExternalIds));
        }

        return result;
    }

    public ApAccessPointNameVO createVO(ApName name) {
        StaticDataProvider staticData = staticDataService.getData();
        return ApAccessPointNameVO.newInstance(name, staticData);
    }

    public ApFragmentTypeVO createVO(ApFragmentType type) {
        return new ApFragmentTypeVO(type.getFragmentTypeId(), type.getCode(), type.getName());
    }

    public ApFragmentVO createVO(final ApFragment fragment, final boolean fillForm) {
        ApFragmentVO fragmentVO = createVO(fragment);
        if (fillForm) {
            fragmentVO.setForm(createFormVO(fragment));
        }
        return fragmentVO;
    }

    public ApAccessPointNameVO createVO(final ApName name, final boolean fillForm) {
        ApAccessPointNameVO nameVO = createVO(name);
        if (fillForm) {
            nameVO.setForm(createFormVO(name));
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

    private ApFormVO createFormVO(final ApAccessPoint accessPoint) {
        List<ApItem> bodyItems = new ArrayList<>(bodyItemRepository.findValidItemsByAccessPoint(accessPoint));
        List<RulItemTypeExt> rulItemTypes = ruleService.getApItemTypesInternal(accessPoint.getApType(), bodyItems, ApRule.RuleType.BODY_ITEMS);

        ApFormVO form = new ApFormVO();
        form.setItemTypes(createItemTypesVO(rulItemTypes));
        form.setItems(createItemsVO(bodyItems));
        return form;
    }

    private ApFormVO createFormVO(final ApName name) {
        List<ApItem> nameItems = new ArrayList<>(nameItemRepository.findValidItemsByName(name));
        List<RulItemTypeExt> rulItemTypes = ruleService.getApItemTypesInternal(name.getAccessPoint().getApType(), nameItems, ApRule.RuleType.NAME_ITEMS);

        ApFormVO form = new ApFormVO();
        form.setItemTypes(createItemTypesVO(rulItemTypes));
        form.setItems(createItemsVO(nameItems));
        return form;
    }

    private List<ApItemVO> createItemsVO(final List<ApItem> apItems) {
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
            List<ApFragmentVO> fragmentVOList = transformList(fragments, this::createVO);
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

        typeVO = ApTypeVO.newInstnace(type, staticData);
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

    public static <S, T> List<T> transformList(List<S> src, Function<S, T> transform) {
        if (src == null) {
            return null;
        }
        int size = src.size();
        if (size == 0) {
            return Collections.emptyList();
        }
        List<T> target = new ArrayList<>(src.size());
        for (S srcItem : src) {
            T targetItem = transform.apply(srcItem);
            target.add(targetItem);
        }
        return target;
    }
}
