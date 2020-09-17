package cz.tacr.elza.controller.factory;

import static cz.tacr.elza.repository.ExceptionThrow.ap;
import static cz.tacr.elza.repository.ExceptionThrow.scope;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.tacr.elza.controller.vo.ApValidationErrorsVO;
import cz.tacr.elza.controller.vo.PartValidationErrorsVO;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.vo.TypeRuleSet;
import cz.tacr.elza.repository.UserRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.common.FactoryUtils;
import cz.tacr.elza.connector.CamConnector;
import cz.tacr.elza.connector.CamInstance;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ApBindingItemVO;
import cz.tacr.elza.controller.vo.ApBindingVO;
import cz.tacr.elza.controller.vo.ApChangeVO;
import cz.tacr.elza.controller.vo.ApEidTypeVO;
import cz.tacr.elza.controller.vo.ApPartVO;
import cz.tacr.elza.controller.vo.ApRecordSimple;
import cz.tacr.elza.controller.vo.ApStateHistoryVO;
import cz.tacr.elza.controller.vo.ApTypeVO;
import cz.tacr.elza.controller.vo.LanguageVO;
import cz.tacr.elza.controller.vo.UserVO;
import cz.tacr.elza.controller.vo.ap.ApStateVO;
import cz.tacr.elza.controller.vo.ap.ApViewSettings;
import cz.tacr.elza.controller.vo.ap.item.ApItemAccessPointRefVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemBitVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemCoordinatesVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemDateVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemDecimalVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemEnumVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemFormattedTextVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemIntVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemJsonTableVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemStringVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemTextVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemUnitdateVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemUnitidVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemUriRefVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeLiteVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApExternalIdType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.SysLanguage;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.packageimport.xml.SettingItemTypes;
import cz.tacr.elza.packageimport.xml.SettingPartsOrder;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApPartRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.ScopeRepository;

@Service
public class ApFactory {

    private static final String DISPLAY_NAME = "DISPLAY_NAME";
    private static final String BRIEF_DESC = "BRIEF_DESC";

    private final ApAccessPointRepository apRepository;

    private final ApStateRepository stateRepository;

    private final ApBindingStateRepository bindingStateRepository;

    private final ApBindingItemRepository bindingItemRepository;

    private final ScopeRepository scopeRepository;

    private final StaticDataService staticDataService;

    private final ApPartRepository partRepository;

    private final ApItemRepository itemRepository;

    private final RuleFactory ruleFactory;

    private final CamConnector camConnector;

    private final ApIndexRepository indexRepository;

    private final ApTypeRepository apTypeRepository;

    private final UserRepository userRepository;

    @Autowired
    public ApFactory(final ApAccessPointRepository apRepository,
                     final ApStateRepository stateRepository,
                     final ScopeRepository scopeRepository,
                     final StaticDataService staticDataService,
                     final ApPartRepository partRepository,
                     final ApItemRepository itemRepository,
                     final ApBindingStateRepository bindingStateRepository,
                     final ApBindingItemRepository bindingItemRepository,
                     final RuleFactory ruleFactory,
                     final CamConnector camConnector,
                     final ApIndexRepository indexRepository,
                     final ApTypeRepository apTypeRepository,
                     final UserRepository userRepository) {
        this.apRepository = apRepository;
        this.stateRepository = stateRepository;
        this.scopeRepository = scopeRepository;
        this.staticDataService = staticDataService;
        this.partRepository = partRepository;
        this.itemRepository = itemRepository;
        this.bindingStateRepository = bindingStateRepository;
        this.bindingItemRepository = bindingItemRepository;
        this.ruleFactory = ruleFactory;
        this.camConnector = camConnector;
        this.indexRepository = indexRepository;
        this.apTypeRepository = apTypeRepository;
        this.userRepository = userRepository;
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
            ApAccessPoint ap = apRepository.findById(id)
                    .orElseThrow(ap(id));
            ApState apState = stateRepository.findLastByAccessPoint(ap);
            return Validate.notNull(apState);
        }
        Validate.isTrue(!apVO.isInvalid());
        // prepare type and scope
        StaticDataProvider staticData = staticDataService.getData();
        ApType type = staticData.getApTypeById(apVO.getTypeId());
        ApScope scope = scopeRepository.findById(apVO.getScopeId())
                .orElseThrow(scope(apVO.getScopeId()));
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
        ApPart preferredPart = accessPoint.getPreferredPart();
        ApIndex preferredPartDisplayName = indexRepository.findByPartAndIndexType(preferredPart, DISPLAY_NAME);
        String name = preferredPartDisplayName != null ? preferredPartDisplayName.getValue() : null;
        return createVO(apState, getTypeRuleSetMap(), accessPoint, name);
    }

    public Map<Integer, Integer> getTypeRuleSetMap() {
        List<TypeRuleSet> typeRuleSets = apTypeRepository.findTypeRuleSets();
        Map<Integer, Integer> result = new HashMap<>(typeRuleSets.size());
        for (TypeRuleSet typeRuleSet : typeRuleSets) {
            result.put(typeRuleSet.getTypeId(), typeRuleSet.getRuleSetId());
        }
        return result;
    }

    private Map<ApBinding, ApBindingState> getBindingMap(List<ApBindingState> eids) {
        Map<ApBinding, ApBindingState> bindings = new HashMap<>();
        if (CollectionUtils.isNotEmpty(eids)) {
            for (ApBindingState bindingState : eids) {
                bindings.put(bindingState.getBinding(), bindingState);
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
        ApAccessPoint ap = state.getAccessPoint();
        ApPart preferredPart = ap.getPreferredPart();
        ApIndex preferredPartDisplayName = indexRepository.findByPartAndIndexType(preferredPart, DISPLAY_NAME);
        String name = preferredPartDisplayName != null ? preferredPartDisplayName.getValue() : null;

        ApAccessPointVO apVO = createVO(state, getTypeRuleSetMap(), ap, name);
        if (fillParts) {

            // prepare parts
            List<ApPart> parts = partRepository.findValidPartByAccessPoint(ap);
            // prepare items
            Map<Integer, List<ApItem>> items = itemRepository.findValidItemsByAccessPoint(ap).stream()
                    .collect(Collectors.groupingBy(i -> i.getPartId()));

            Map<Integer, List<ApIndex>> indices = indexRepository.findByPartsAndIndexType(parts, DISPLAY_NAME).stream()
                    .collect(Collectors.groupingBy(i -> i.getPart().getPartId()));

            //comments
            Integer comments = stateRepository.countCommentsByAccessPoint(ap);
            //description
            String description = getDescription(parts, items);

            //vlastník entity
            UsrUser ownerUser = userRepository.findAccessPointOwner(ap);

            //prepare external ids
            List<ApBindingState> eids = bindingStateRepository.findByAccessPoint(ap);
            Map<ApBinding, ApBindingState> bindings = getBindingMap(eids);
            Map<Integer, List<ApBindingItem>> bindingItemsMap = new HashMap<>();
            if (MapUtils.isNotEmpty(bindings)) {
                bindingItemsMap = bindingItemRepository.findByBindings(bindings.keySet()).stream()
                        .collect(Collectors.groupingBy(i -> i.getBinding().getBindingId()));
            }

            List<ApBindingVO> eidsVO = FactoryUtils.transformList(eids, ApBindingVO::newInstance);
            apVO.setExternalIds(eidsVO);
            fillBindingUrls(eidsVO);
            fillBindingItems(eidsVO, bindings, bindingItemsMap);

            apVO.setParts(createVO(parts, items, indices));
            apVO.setComments(comments);
            if (description != null) {
                apVO.setDescription(description);
            }
            apVO.setPreferredPart(preferredPart.getPartId());
            apVO.setLastChange(createVO(state.getCreateChange()));
            apVO.setOwnerUser(createVO(ownerUser));
        }
        return apVO;
    }

    public ApAccessPointVO createVO(final ApState apState,
                                    final Map<Integer, Integer> typeRuleSetMap,
                                    final ApAccessPoint ap,
                                    final String name) {
        // create VO
        ApAccessPointVO vo = new ApAccessPointVO();
        vo.setId(ap.getAccessPointId());
        vo.setInvalid(apState.getDeleteChange() != null);
        vo.setScopeId(apState.getScopeId());
        vo.setTypeId(apState.getApTypeId());
        vo.setComment(apState.getComment());
        vo.setStateApproval(apState.getStateApproval());
        vo.setUuid(ap.getUuid());
        vo.setExternalIds(Collections.emptyList());
        vo.setErrorDescription(ap.getErrorDescription());
        if (typeRuleSetMap != null) {
            vo.setRuleSetId(typeRuleSetMap.get(apState.getApTypeId()));
        }

        vo.setState(ap.getState() == null ? null : ApStateVO.valueOf(ap.getState().name()));
        vo.setName(name);
        return vo;
    }

    private void fillBindingUrls(final List<ApBindingVO> bindings) {
        if (CollectionUtils.isNotEmpty(bindings)) {
            for (ApBindingVO binding : bindings) {
                CamInstance camInstance = camConnector.findById(binding.getExternalSystemCode());
                if (camInstance != null) {
                    String value = binding.getValue();
                    if (StringUtils.isNotEmpty(value)) {
                        String url = camInstance.getEntityDetailUrl(value);
                        binding.setDetailUrl(url);
                    }
                    String extReplacedBy = binding.getExtReplacedBy();
                    if (StringUtils.isNotEmpty(extReplacedBy)) {
                        String url = camInstance.getEntityDetailUrl(extReplacedBy);
                        binding.setDetailUrlExtReplacedBy(url);
                    }
                }
            }
        }
    }

    private void fillBindingItems(final List<ApBindingVO> eidsVO,
                                  final Map<ApBinding, ApBindingState> bindings,
                                  final Map<Integer, List<ApBindingItem>> bindingItemsMap) {
        if (CollectionUtils.isNotEmpty(eidsVO)) {
            for (ApBindingVO apBindingVO : eidsVO) {
                ApBinding apBinding = bindings.keySet().stream().filter(b -> b.getBindingId().equals(apBindingVO.getId())).findFirst().orElse(null);
                ApBindingState state = apBinding == null ? null : bindings.get(apBinding);
                List<ApBindingItem> bindingItems = bindingItemsMap.getOrDefault(apBindingVO.getId(), new ArrayList<>());
                if (CollectionUtils.isNotEmpty(bindingItems)) {
                    apBindingVO.setBindingItemList(FactoryUtils.transformList(bindingItems, i -> ApBindingItemVO.newInstance(state, i)));
                }
            }
        }
    }

    private String getDescription(List<ApPart> parts, Map<Integer, List<ApItem>> items) {
        ApPart body = null;
        String briefDesc = null;
        StaticDataProvider sdp = staticDataService.getData();

        if (CollectionUtils.isNotEmpty(parts)) {
            for (ApPart part : parts) {
                if (part.getPartType().getCode().equals(sdp.getDefaultBodyPartType().getCode())) {
                    body = part;
                    break;
                }
            }
        }

        if (body != null && items != null) {
            List<ApItem> bodyItems = items.get(body.getPartId());
            if (CollectionUtils.isNotEmpty(bodyItems)) {
                for (ApItem item : bodyItems) {
                    if (item.getItemType().getCode().equals(BRIEF_DESC)) {
                        briefDesc = item.getData().getFulltextValue();
                        break;
                    }
                }
            }
        }
        return briefDesc;
    }

    public List<ApPartVO> createVO(final List<ApPart> parts,
                                   final Map<Integer, List<ApItem>> items,
                                   final Map<Integer, List<ApIndex>> indices) {
        List<ApPartVO> partVOList = new ArrayList<>();
        for (ApPart part : parts) {
            partVOList.add(createVO(part, items.get(part.getPartId()), indices.get(part.getPartId())));
        }
        return partVOList;
    }

    public ApPartVO createVO(final ApPart part,
                             final List<ApItem> apItems,
                             final List<ApIndex> indices) {
        ApPartVO apPartVO = new ApPartVO();

        apPartVO.setId(part.getPartId());
        apPartVO.setTypeId(part.getPartType().getPartTypeId());
        apPartVO.setState(part.getState() == null ? null : ApStateVO.valueOf(part.getState().name()));
        apPartVO.setErrorDescription(part.getErrorDescription());
        apPartVO.setValue(CollectionUtils.isNotEmpty(indices) ? indices.get(0).getValue() : null);
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

        Map<Integer, ApIndex> nameMap = indexRepository.findPreferredPartIndexByAccessPointsAndIndexType(accessPoints, DISPLAY_NAME).stream()
                .collect(Collectors.toMap(i -> i.getPart().getAccessPointId(), Function.identity()));

        for (ApAccessPoint accessPoint : accessPoints) {
            Integer accessPointId = accessPoint.getAccessPointId();
            ApState apState = apStateMap.get(accessPointId);
            ApIndex indexName = nameMap.get(accessPointId);
            String name = indexName != null ? indexName.getValue() : null;
            result.add(createVO(apState, getTypeRuleSetMap(), accessPoint, name));
        }

        return result;
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

        for (ApItemVO item : items) {
            if (item instanceof ApItemAccessPointRefVO) {
                Integer accessPointId = ((ApItemAccessPointRefVO) item).getValue();
                if (accessPointId != null) {
                    List<ApItemAccessPointRefVO> list = accessPointsMap.computeIfAbsent(accessPointId, k -> new ArrayList<>());
                    list.add((ApItemAccessPointRefVO) item);
                }
            }
        }

        Set<Integer> accessPointIds = accessPointsMap.keySet();
        if (!accessPointIds.isEmpty()) {
            List<ApAccessPoint> accessPoints = apRepository.findAllById(accessPointIds);
            List<ApAccessPointVO> accessPointVOList = createVO(accessPoints);
            for (ApAccessPointVO accessPointVO : accessPointVOList) {
                List<ApItemAccessPointRefVO> accessPointRefVOS = accessPointsMap.get(accessPointVO.getId());
                for (ApItemAccessPointRefVO accessPointRefVO : accessPointRefVOS) {
                    accessPointRefVO.setAccessPoint(accessPointVO);
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
                item = new ApItemAccessPointRefVO(apItem, ((externalSystem, value) -> {
                    CamInstance camInstance = camConnector.getByCode(externalSystem.getCode());
                    return camInstance.getEntityDetailUrl(value);
                }));
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

    public ApViewSettings.ApViewSettingsRule createApTypeViewSettings(final RulRuleSet ruleRule, final List<UISettings> itemTypesSettings, final List<UISettings> partsOrderSettings) {
        ApViewSettings.ApViewSettingsRule result = new ApViewSettings.ApViewSettingsRule();
        result.setItemTypes(itemTypesSettings.size() > 0
                ? SettingItemTypes.newInstance(itemTypesSettings.get(0)).getItemTypes()
                : Collections.emptyList());
        result.setPartsOrder(partsOrderSettings.size() > 0
                ? SettingPartsOrder.newInstance(partsOrderSettings.get(0)).getParts()
                : Collections.emptyList());
        result.setCode(ruleRule.getCode());
        result.setRuleSetId(ruleRule.getRuleSetId());
        return result;
    }

    public ApValidationErrorsVO createVO(Integer accessPointId) {
        ApAccessPoint accessPoint = apRepository.findById(accessPointId)
                .orElseThrow(() -> new ObjectNotFoundException("Přístupový bod neexistuje", BaseCode.ID_NOT_EXIST).setId(accessPointId));
        List<ApPart> partList = partRepository.findValidPartByAccessPoint(accessPoint);

        String[] errorsArray = StringUtils.split(accessPoint.getErrorDescription(), "\n");
        List<String> errors = new ArrayList<>();

        if (errorsArray != null) {
            errors.addAll(Arrays.asList(errorsArray));
        }

        List<PartValidationErrorsVO> partValidationErrorsVOList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(partList)) {
            for (ApPart part : partList) {
                if (StringUtils.isNotEmpty(part.getErrorDescription())) {
                    String[] partErrorsArray = StringUtils.split(part.getErrorDescription(), "\n");
                    if (partErrorsArray != null) {
                        List<String> partErrors = new ArrayList<>(Arrays.asList(partErrorsArray));
                        partValidationErrorsVOList.add(createVO(part.getPartId(), partErrors));
                    }
                }
            }
        }

        return createVO(errors, partValidationErrorsVOList);
    }

    private PartValidationErrorsVO createVO(final Integer id, final List<String> errors) {
        PartValidationErrorsVO partValidationErrorsVO = new PartValidationErrorsVO();
        partValidationErrorsVO.setId(id);
        partValidationErrorsVO.setErrors(errors);
        return partValidationErrorsVO;
    }

    private ApValidationErrorsVO createVO(final List<String> errors, final List<PartValidationErrorsVO> partErrors) {
        ApValidationErrorsVO apValidationErrorsVO = new ApValidationErrorsVO();
        apValidationErrorsVO.setErrors(errors);
        apValidationErrorsVO.setPartErrors(partErrors);
        return apValidationErrorsVO;
    }
}
