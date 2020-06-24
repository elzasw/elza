package cz.tacr.elza.controller.factory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.tacr.elza.connector.CamConnector;
import cz.tacr.elza.connector.CamInstance;
import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.controller.vo.ap.item.*;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.common.FactoryUtils;
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

    private final ApBindingStateRepository bindingStateRepository;

    private final ApBindingItemRepository bindingItemRepository;

    private final ScopeRepository scopeRepository;

    private final StaticDataService staticDataService;

    private final ApPartRepository partRepository;

    private final ApItemRepository itemRepository;

    private final RuleService ruleService;

    private final RuleFactory ruleFactory;

    private final CamConnector camConnector;

    @Autowired
    public ApFactory(final ApAccessPointRepository apRepository,
                     final ApStateRepository stateRepository,
                     final ScopeRepository scopeRepository,
                     final StaticDataService staticDataService,
                     final ApPartRepository partRepository,
                     final ApItemRepository itemRepository,
                     final ApBindingStateRepository bindingStateRepository,
                     final ApBindingItemRepository bindingItemRepository,
                     final RuleService ruleService,
                     final RuleFactory ruleFactory,
                     final CamConnector camConnector) {
        this.apRepository = apRepository;
        this.stateRepository = stateRepository;
        this.scopeRepository = scopeRepository;
        this.staticDataService = staticDataService;
        this.partRepository = partRepository;
        this.itemRepository = itemRepository;
        this.bindingStateRepository = bindingStateRepository;
        this.bindingItemRepository = bindingItemRepository;
        this.ruleService = ruleService;
        this.ruleFactory = ruleFactory;
        this.camConnector = camConnector;
    }

    /**
     * Creates simple value object from AP.
     */
    public ApRecordSimple createVOSimple(ApState apState) {
        ApAccessPoint ap = apState.getAccessPoint();
        // create VO
        ApRecordSimple vo = new ApRecordSimple();
        vo.setTypeId(apState.getApTypeId());
        vo.setId(ap.getAccessPointId());
        //TODO : chybí metoda pro získání preferovaného jména
        vo.setRecord(null);
        vo.setScopeName(apState.getScope().getName());
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
        // prepare external ids
        List<ApBindingState> eids = bindingStateRepository.findByAccessPoint(ap);
        return createVO(apState, eids);
    }

    private List<ApBinding> getBindingList(List<ApBindingState> eids) {
        List<ApBinding> bindings = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(eids)) {
            for (ApBindingState bindingState : eids) {
                bindings.add(bindingState.getBinding());
            }
        }
        return bindings;
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

    public ApAccessPointVO createVO(ApState state, boolean fillParts) {
        ApAccessPointVO apVO = createVO(state);
        if (fillParts) {
            ApAccessPoint ap = state.getAccessPoint();

            // prepare parts
            List<ApPart> parts = partRepository.findValidPartByAccessPoint(ap);
            // prepare items
            Map<Integer, List<ApItem>> items = itemRepository.findValidItemsByAccessPoint(ap).stream()
                    .collect(Collectors.groupingBy(i -> i.getPartId()));

            //comments
            Integer comments = stateRepository.countCommentsByAccessPoint(ap);
            //description
            String description = getDescription(parts, items);

            //items for externalSystems
            List<ApBindingState> eids = bindingStateRepository.findByAccessPoint(ap);
            List<ApBinding> bindings = getBindingList(eids);
            Map<Integer, List<ApBindingItem>> bindingItemsMap = null;
            if (CollectionUtils.isNotEmpty(bindings)) {
                bindingItemsMap = bindingItemRepository.findByBindings(bindings).stream()
                        .collect(Collectors.groupingBy(i -> i.getBinding().getBindingId()));
            }

            List<ApBindingVO> apBindingVOList = (List<ApBindingVO>) apVO.getExternalIds();
            fillBindingItems(apBindingVOList, bindingItemsMap);

            apVO.setParts(createVO(parts, items));
            apVO.setComments(comments);
            if (description != null) {
                apVO.setDescription(description);
            }
        }
        return apVO;
    }

    public ApAccessPointVO createVO(final ApState apState,
                                    final List<ApBindingState> eids) {
        ApAccessPoint ap = apState.getAccessPoint();
        ApPart preferredPart = ap.getPreferredPart();
        UserVO ownerUser = getOwnerUser(ap);
        // prepare external ids
        List<ApBindingVO> eidsVO = FactoryUtils.transformList(eids, ApBindingVO::newInstance);
        fillBindingUrls(eidsVO);

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
        vo.setErrorDescription(ap.getErrorDescription());

        vo.setState(ap.getState() == null ? null : ApStateVO.valueOf(ap.getState().name()));
        vo.setName(preferredPart != null ? preferredPart.getValue() : null);
        vo.setPreferredPart(preferredPart != null ? preferredPart.getPartId() : null);
        vo.setLastChange(createVO(apState.getCreateChange()));
        vo.setOwnerUser(ownerUser);
        return vo;
    }

    private void fillBindingUrls(final List<ApBindingVO> bindings) {
        if (CollectionUtils.isNotEmpty(bindings)) {
            for (ApBindingVO binding : bindings) {
                CamInstance camInstance = camConnector.findById(binding.getExternalSystemCode());
                if (camInstance != null) {
                    String value = binding.getValue();
                    if (StringUtils.isNotEmpty(value)) {
                        String url = camInstance.getEntityDetailUrl(Integer.parseInt(value));
                        binding.setDetailUrl(url);
                    }
                    String extReplacedBy = binding.getExtReplacedBy();
                    if (StringUtils.isNotEmpty(extReplacedBy)) {
                        String url = camInstance.getEntityDetailUrl(Integer.parseInt(extReplacedBy));
                        binding.setDetailUrlExtReplacedBy(url);
                    }
                }
            }
        }
    }

    private void fillBindingItems(List<ApBindingVO> eidsVO, Map<Integer, List<ApBindingItem>> bindingItemsMap) {
        if (CollectionUtils.isNotEmpty(eidsVO)) {
            for (ApBindingVO apBindingVO : eidsVO) {
                List<ApBindingItem> bindingItems = bindingItemsMap.getOrDefault(apBindingVO.getId(), new ArrayList<>());
                if (CollectionUtils.isNotEmpty(bindingItems)) {
                    apBindingVO.setBindingItemList(FactoryUtils.transformList(bindingItems, ApBindingItemVO::newInstance));
                }
            }
        }
    }

    private String getDescription(List<ApPart> parts, Map<Integer, List<ApItem>> items) {
        ApPart body = null;
        String briefDesc = null;

        if (CollectionUtils.isNotEmpty(parts)) {
            for (ApPart part : parts) {
                if (part.getPartType().getCode().equals("PT_BODY")) {
                    body = part;
                    break;
                }
            }
        }

        if (body != null && items != null) {
            List<ApItem> bodyItems = items.get(body.getPartId());
            if (CollectionUtils.isNotEmpty(bodyItems)) {
                for (ApItem item : bodyItems) {
                    if (item.getItemType().getCode().equals("BRIEF_DESC")) {
                        briefDesc = item.getData().getFulltextValue();
                        break;
                    }
                }
            }
        }
        return briefDesc;
    }

    private UserVO getOwnerUser(final ApAccessPoint accessPoint) {
        ApState first = stateRepository.findFirstByAccessPoint(accessPoint);
        return createVO(first.getCreateChange().getUser());
    }

    public List<ApPartVO> createVO(final List<ApPart> parts,
                                   final Map<Integer, List<ApItem>> items) {
        List<ApPartVO> partVOList = new ArrayList<>();
        for (ApPart part : parts) {
            partVOList.add(createVO(part, items.get(part.getPartId())));
        }
        return partVOList;
    }

    public ApPartVO createVO(final ApPart part,
                             final List<ApItem> apItems) {
        ApPartVO apPartVO = new ApPartVO();

        apPartVO.setId(part.getPartId());
        apPartVO.setTypeId(part.getPartType().getPartTypeId());
        apPartVO.setState(part.getState() == null ? null : ApStateVO.valueOf(part.getState().name()));
        apPartVO.setErrorDescription(part.getErrorDescription());
        apPartVO.setValue(part.getValue());
        apPartVO.setPartParentId(part.getParentPart() != null ? part.getParentPart().getPartId() : null);
        apPartVO.setItems(CollectionUtils.isNotEmpty(apItems) ? createItemsVO(apItems) : null);

        return apPartVO;
    }

    public ApChangeVO createVO(final ApChange change) {
        ApChangeVO apChangeVO = new ApChangeVO();
        apChangeVO.setId(change.getChangeId());
        apChangeVO.setChange(change.getChangeDate().toLocalDateTime());
        apChangeVO.setUser(createVO(change.getUser()));
        return apChangeVO;
    }

    public UserVO createVO(final UsrUser user) {
        if (user == null) {
            return null;
        }

        UserVO userVO = new UserVO();
        userVO.setId(user.getUserId());
        userVO.setDisplayName(user.getUsername());
        return userVO;
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
        Map<Integer, List<ApBindingState>> apEidsMap = bindingStateRepository.findByAccessPoints(accessPoints).stream()
                .collect(Collectors.groupingBy(o -> o.getAccessPointId()));
//        RulItemType rulItemType = sdp.getItemTypeByCode("BRIEF_DESC").getEntity();
//        Map<Integer, List<ApItem>> descMap = itemRepository.findItemsByAccessPointsAndItemTypeAndPartTypeCode(accessPoints, rulItemType, "PT_BODY").stream()
//                .collect(Collectors.groupingBy(o -> o.getPart().getAccessPointId()));

        for (ApAccessPoint accessPoint : accessPoints) {
            Integer accessPointId = accessPoint.getAccessPointId();
            ApState apState = apStateMap.get(accessPointId);
            List<ApBindingState> apBindings = apEidsMap.getOrDefault(accessPointId, Collections.emptyList());
            result.add(createVO(apState, apBindings));
        }

        return result;
    }

    public ApFragmentVO createVO(final ApPart fragment, final boolean fillForm) {
        ApFragmentVO fragmentVO = createVO(fragment);
        if (fillForm) {
            //TODO fantis: smazat nebo prepsat
//            fragmentVO.setForm(createFormVO(fragment));
        }
        return fragmentVO;
    }

    public ApFragmentVO createVO(final ApPart fragment) {
        return ApFragmentVO.newInstance(fragment);
    }

    //TODO fantis: smazat nebo prepsat
    /*private ApFormVO createFormVO(final ApPart fragment) {
        List<ApItem> fragmentItems = new ArrayList<>(fragmentItemRepository.findValidItemsByFragment(fragment));
        List<RulItemTypeExt> rulItemTypes = ruleService.getFragmentItemTypesInternal(fragment.getFragmentType(), fragmentItems);

        ApFormVO form = new ApFormVO();
        form.setItemTypes(createItemTypesVO(rulItemTypes));
        form.setItems(createItemsVO(fragmentItems));
        return form;
    }*/

    private ApFormVO createFormVO(ApAccessPoint accessPoint, ApType apType) {
        List<ApItem> apItems = new ArrayList<>(itemRepository.findValidItemsByAccessPoint(accessPoint));
        List<RulItemTypeExt> rulItemTypes = ruleService.getApItemTypesInternal(apType, apItems, ApRule.RuleType.BODY_ITEMS);

        ApFormVO form = new ApFormVO();
        form.setItemTypes(createItemTypesVO(rulItemTypes));
        form.setItems(createItemsVO(apItems));
        return form;
    }

    private ApFormVO createFormVO(ApType type) {
        ApFormVO form = new ApFormVO();
        return form;
    }

    public List<ApItemVO> createItemsVO(final List<ApItem> apItems) {
        List<ApItemVO> items = new ArrayList<>(apItems.size());
        for (ApItem item : apItems) {
            items.add(createItem(item));
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
        Map<Integer, List<ApItemAPFragmentRefVO>> fragmentMap = new HashMap<>();

        for (ApItemVO item : items) {
            if (item instanceof ApItemAccessPointRefVO) {
                Integer accessPointId = ((ApItemAccessPointRefVO) item).getValue();
                if (accessPointId != null) {
                    List<ApItemAccessPointRefVO> list = accessPointsMap.computeIfAbsent(accessPointId, k -> new ArrayList<>());
                    list.add((ApItemAccessPointRefVO) item);
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

        Set<Integer> fragmentIds = fragmentMap.keySet();
        if (!fragmentIds.isEmpty()) {
            List<ApPart> fragments = partRepository.findAll(fragmentIds);
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
            case URI_REF:
                item = new ApItemUriRefVO(apItem);
                break;
            case BIT:
                item = new ApItemBitVO(apItem);
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
