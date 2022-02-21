package cz.tacr.elza.controller.factory;

import static cz.tacr.elza.repository.ExceptionThrow.ap;
import static cz.tacr.elza.repository.ExceptionThrow.scope;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME;
import static cz.tacr.elza.groovy.GroovyResult.SORT_NAME;

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

import cz.tacr.elza.domain.AccessPointItem;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApRevIndex;
import cz.tacr.elza.domain.ApRevItem;
import cz.tacr.elza.domain.ApRevPart;
import cz.tacr.elza.domain.ApRevision;
import cz.tacr.elza.domain.ApStateEnum;
import cz.tacr.elza.domain.ChangeType;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.repository.ApRevIndexRepository;
import cz.tacr.elza.repository.ApRevItemRepository;
import cz.tacr.elza.repository.ApRevPartRepository;
import cz.tacr.elza.service.RevisionItemService;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.CachedPart;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.common.FactoryUtils;
import cz.tacr.elza.common.ObjectListIterator;
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
import cz.tacr.elza.controller.vo.ApValidationErrorsVO;
import cz.tacr.elza.controller.vo.LanguageVO;
import cz.tacr.elza.controller.vo.PartValidationErrorsVO;
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
import cz.tacr.elza.core.ElzaLocale;
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
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.SysLanguage;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.packageimport.xml.SettingItemTypes;
import cz.tacr.elza.packageimport.xml.SettingPartsOrder;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApPartRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.UserRepository;
import cz.tacr.elza.repository.vo.TypeRuleSet;

import javax.annotation.Nullable;

@Service
public class ApFactory {

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

    private final ApChangeRepository changeRepository;

    private final ElzaLocale elzaLocale;

    private final ApRevPartRepository revPartRepository;

    private final ApRevItemRepository revItemRepository;

    private final ApRevIndexRepository revIndexRepository;

    private final RevisionItemService revisionItemService;

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
                     final UserRepository userRepository,
                     final ApChangeRepository changeRepository,
                     final ApRevPartRepository revPartRepository,
                     final ApRevItemRepository revItemRepository,
                     final ApRevIndexRepository revIndexRepository,
                     final RevisionItemService revisionItemService,
                     final ElzaLocale elzaLocale) {
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
        this.changeRepository = changeRepository;
        this.revPartRepository = revPartRepository;
        this.revItemRepository = revItemRepository;
        this.revIndexRepository = revIndexRepository;
        this.revisionItemService = revisionItemService;
        this.elzaLocale = elzaLocale;
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

            Map<Integer, List<ApIndex>> indices = ObjectListIterator.findIterable(parts, p -> indexRepository.findByPartsAndIndexType(p, DISPLAY_NAME)).stream()
                    .collect(Collectors.groupingBy(i -> i.getPart().getPartId()));

            //comments
            Integer comments = stateRepository.countCommentsByAccessPoint(ap);

            // description
            String description = getDescription(parts, items);

            // vlastník entity
            UsrUser ownerUser = userRepository.findAccessPointOwner(ap);

            // prepare bindings
            List<ApBindingState> bindingStates = bindingStateRepository.findByAccessPoint(ap);
            Map<ApBinding, ApBindingState> bindings = getBindingMap(bindingStates);
            Map<Integer, List<ApBindingItem>> bindingItemsMap = new HashMap<>();
            if (MapUtils.isNotEmpty(bindings)) {
                bindingItemsMap = bindingItemRepository.findByBindings(bindings.keySet()).stream()
                        .collect(Collectors.groupingBy(i -> i.getBinding().getBindingId()));
            }

            // prepare last change
            Integer lastChangeId = apRepository.getLastCreateChange(state.getAccessPointId());
            if (lastChangeId < state.getCreateChangeId()) {
                lastChangeId = state.getCreateChangeId();
            }
            ApChange lastChange = changeRepository.findById(lastChangeId).get();

            List<ApBindingVO> bindingsVO = Collections.emptyList();
            if (bindingStates != null) {
                bindingsVO = bindingStates.stream().map(s -> ApBindingVO.newInstance(s, lastChange)).collect(Collectors.toList()); 
            }            
            apVO.setBindings(bindingsVO);
            fillBindingUrls(bindingsVO);
            fillBindingItems(bindingsVO, bindings, bindingItemsMap);

            apVO.setParts(createVO(parts, items, indices));
            apVO.setComments(comments);
            if (description != null) {
                apVO.setDescription(description);
            }
            apVO.setPreferredPart(preferredPart.getPartId());
            apVO.setLastChange(createVO(lastChange));
            apVO.setOwnerUser(createVO(ownerUser));
        }
        return apVO;
    }

    public ApAccessPointVO createVO(CachedAccessPoint cachedAccessPoint) {
        String name = findAeCachedEntityName(cachedAccessPoint);
        ApAccessPointVO apVO = createVO(cachedAccessPoint.getApState(), getTypeRuleSetMap(), cachedAccessPoint, name);

        // description
        String description = getDescription(cachedAccessPoint);

        // prepare last change
        Integer lastChangeId = apRepository.getLastCreateChange(cachedAccessPoint.getAccessPointId());
        if (lastChangeId < cachedAccessPoint.getApState().getCreateChangeId()) {
            lastChangeId = cachedAccessPoint.getApState().getCreateChangeId();
        }
        ApChange lastChange = changeRepository.findById(lastChangeId).get();

        // prepare bindings
        List<ApBindingVO> bindingsVO = Collections.emptyList();
        if (cachedAccessPoint.getBindings() != null) {
            bindingsVO = cachedAccessPoint.getBindings().stream()
                    .map(s -> ApBindingVO.newInstance(s, cachedAccessPoint.getParts(), lastChange)).collect(Collectors.toList());
        }
        apVO.setBindings(bindingsVO);
        fillBindingUrls(bindingsVO);

        apVO.setParts(createPartsVO(cachedAccessPoint.getParts()));
        if (description != null) {
            apVO.setDescription(description);
        }
        apVO.setPreferredPart(cachedAccessPoint.getPreferredPartId());
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
        vo.setBindings(Collections.emptyList());
        vo.setErrorDescription(ap.getErrorDescription());
        if (typeRuleSetMap != null) {
            vo.setRuleSetId(typeRuleSetMap.get(apState.getApTypeId()));
        }

        vo.setState(ap.getState() == null ? null : ApStateVO.valueOf(ap.getState().name()));
        vo.setName(name);
        return vo;
    }

    public ApAccessPointVO createVO(final ApState apState,
                                    final Map<Integer, Integer> typeRuleSetMap,
                                    final CachedAccessPoint ap,
                                    final String name) {
        return createVO(apState, typeRuleSetMap, ap.getAccessPointId(), ap.getUuid(), ap.getErrorDescription(), ap.getState(), name);
    }

    public ApAccessPointVO createVO(final ApState apState,
                                    final Map<Integer, Integer> typeRuleSetMap,
                                    final Integer accessPointId,
                                    final String uuid,
                                    final String errorDescription,
                                    final ApStateEnum state,
                                    final String name) {
        // create VO
        ApAccessPointVO vo = new ApAccessPointVO();
        vo.setId(accessPointId);
        vo.setInvalid(apState.getDeleteChange() != null);
        vo.setScopeId(apState.getScopeId());
        vo.setTypeId(apState.getApTypeId());
        vo.setComment(apState.getComment());
        vo.setStateApproval(apState.getStateApproval());
        vo.setUuid(uuid);
        vo.setBindings(Collections.emptyList());
        vo.setErrorDescription(errorDescription);
        if (typeRuleSetMap != null) {
            vo.setRuleSetId(typeRuleSetMap.get(apState.getApTypeId()));
        }

        vo.setState(state == null ? null : ApStateVO.valueOf(state.name()));
        vo.setName(name);
        return vo;
    }

    private void fillBindingUrls(final List<ApBindingVO> bindings) {
        StaticDataProvider sdp = staticDataService.getData();
        if (CollectionUtils.isNotEmpty(bindings)) {
            for (ApBindingVO binding : bindings) {
                ApExternalSystem externalSystem = sdp.getApExternalSystemById(binding.getExternalSystemId());
                CamInstance camInstance = camConnector.get(externalSystem);
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

    public String findAeCachedEntityName(CachedAccessPoint entity) {
        if (CollectionUtils.isNotEmpty(entity.getParts())) {
            for (CachedPart part : entity.getParts()) {
                if (part.getPartId().equals(entity.getPreferredPartId())) {
                    if (CollectionUtils.isNotEmpty(part.getIndices())) {
                        for (ApIndex index : part.getIndices()) {
                            if (index.getIndexType().equals(DISPLAY_NAME)) {
                                return index.getValue();
                            }
                        }
                    }
                    break;
                }
            }
        }
        return null;
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

    private String getDescription(CachedAccessPoint cachedAccessPoint) {
        CachedPart body = null;
        String briefDesc = null;
        StaticDataProvider sdp = staticDataService.getData();

        if (CollectionUtils.isNotEmpty(cachedAccessPoint.getParts())) {
            for (CachedPart part : cachedAccessPoint.getParts()) {
                if (part.getPartTypeCode().equals(sdp.getDefaultBodyPartType().getCode())) {
                    body = part;
                    break;
                }
            }
        }

        if (body != null) {
            if (CollectionUtils.isNotEmpty(body.getItems())) {
                for (ApItem item : body.getItems()) {
                    ItemType itemType = sdp.getItemTypeById(item.getItemTypeId());
                    if (itemType.getCode().equals(BRIEF_DESC)) {
                        briefDesc = item.getData().getFulltextValue();
                        break;
                    }
                }
            }
        }
        return briefDesc;
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

    private List<ApPartVO> createPartsVO(List<CachedPart> parts) {
        if (CollectionUtils.isEmpty(parts)) {
            return Collections.emptyList();
        }

        Map<ApPartVO, String> sortValues = new HashMap<>(); 
        List<ApPartVO> partVOList = new ArrayList<>(parts.size());
        for (CachedPart part : parts) {
            ApPartVO partVO = createPartVO(part);
            partVOList.add(partVO);
            sortValues.put(partVO, getSortName(part));
        }

        partVOList.sort((p1, p2) -> {
            String s1 = sortValues.get(p1);
            String s2 = sortValues.get(p2);
            if (s1 == null || s2 == null) {
                return 0;
            }
            return elzaLocale.getCollator().compare(s1, s2);
        });

        return partVOList;
    }

    private ApPartVO createPartVO(CachedPart part) {
        StaticDataProvider sdp = staticDataService.getData();
        RulPartType rulPartType = sdp.getPartTypeByCode(part.getPartTypeCode());
        ApPartVO apPartVO = new ApPartVO();

        apPartVO.setId(part.getPartId());
        apPartVO.setTypeId(rulPartType.getPartTypeId());
        apPartVO.setState(part.getState() == null ? null : ApStateVO.valueOf(part.getState().name()));
        apPartVO.setErrorDescription(part.getErrorDescription());
        apPartVO.setValue(findDisplayIndexValue(part.getIndices()));
        apPartVO.setPartParentId(part.getParentPartId());
        apPartVO.setChangeType(ChangeType.ORIGINAL);
        apPartVO.setItems(CollectionUtils.isNotEmpty(part.getItems()) ? createItemsVO(part.getItems()) : null);

        return apPartVO;
    }

    public static String getSortName(CachedPart part) {
        String index = findIndexValue(part.getIndices(), SORT_NAME);
        if (StringUtils.isEmpty(index)) {
            index = "";
        }
        return index + String.format("%012d", part.getPartId());
    }

    @Nullable
    public static String findIndexValue(List<ApIndex> indices, String indexName) {
        if (indices == null) {
            return null;
        }
        for (ApIndex index : indices) {
            if (index.getIndexType().equals(indexName)) {
                return index.getValue();
            }
        }
        return null;
    }

    @Nullable
    static public String findDisplayIndexValue(List<ApIndex> indices) {
        return findIndexValue(indices, DISPLAY_NAME);
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
        apPartVO.setChangeType(ChangeType.ORIGINAL);

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

        Map<Integer, ApIndex> nameMap = ObjectListIterator.findIterable(accessPoints,
                ap -> indexRepository.findPreferredPartIndexByAccessPointsAndIndexType(ap, DISPLAY_NAME)).stream()
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

    private ApItemVO createItem(final AccessPointItem apItem) {
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
                item = new ApItemAccessPointRefVO(apItem, ((externalSystemId, value) -> {
                    ApExternalSystem externalSystem = sdp.getApExternalSystemById(externalSystemId);
                    CamInstance camInstance = camConnector.get(externalSystem);
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

        item.setChangeType(ChangeType.ORIGINAL);
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

    public ApValidationErrorsVO createValidationVO(ApAccessPoint accessPoint) {
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

    public ApAccessPointVO createVO(ApAccessPointVO vo, ApRevision revision) {
        vo.setRevStateApproval(revision.getStateApproval());
        vo.setNewTypeId(revision.getTypeId());
        vo.setRevPreferredPart(revision.getRevPreferredPartId());
        vo.setNewPreferredPart(revision.getPreferredPartId());

        // prepare parts
        List<ApRevPart> parts = revPartRepository.findByRevision(revision);
        // prepare items
        Map<Integer, List<ApRevItem>> items = ObjectListIterator.findIterable(parts, revItemRepository::findByParts).stream()
                .collect(Collectors.groupingBy(ApRevItem::getPartId));

        Map<Integer, List<ApRevIndex>> indices = ObjectListIterator.findIterable(parts, p -> revIndexRepository.findByPartsAndIndexType(p, DISPLAY_NAME)).stream()
                .collect(Collectors.groupingBy(ApRevIndex::getPartId));

        vo.setRevParts(createRevVO(parts, items, indices));

        return vo;
    }

    private List<ApPartVO> createRevVO(List<ApRevPart> parts, Map<Integer, List<ApRevItem>> items, Map<Integer, List<ApRevIndex>> indices) {
        List<ApPartVO> partVOList = new ArrayList<>();
        for (ApRevPart part : parts) {
            partVOList.add(createVO(part, items.get(part.getPartId()), indices.get(part.getPartId())));
        }
        return partVOList;
    }

    private ApPartVO createVO(ApRevPart part, List<ApRevItem> items, List<ApRevIndex> indices) {
        ApPartVO apPartVO = new ApPartVO();

        ChangeType changeType = ChangeType.NEW;
        if (part.getOriginalPartId() != null) {
            if (revisionItemService.allItemsDeleted(items)) {
                changeType = ChangeType.DELETED;
            } else {
                changeType = ChangeType.UPDATED;
            }
        }

        apPartVO.setId(part.getPartId());
        apPartVO.setTypeId(part.getPartType().getPartTypeId());
        apPartVO.setValue(CollectionUtils.isNotEmpty(indices) ? indices.get(0).getValue() : null);
        apPartVO.setPartParentId(part.getParentPart() != null ? part.getParentPartId() : null);
        apPartVO.setRevPartParentId(part.getRevParentPart() != null ? part.getRevParentPartId() : null);
        apPartVO.setOrigPartId(part.getOriginalPart() != null ? part.getOriginalPartId() : null);
        apPartVO.setChangeType(changeType);
        apPartVO.setItems(CollectionUtils.isNotEmpty(items) ? createRevItemsVO(items) : null);

        return apPartVO;
    }

    private List<ApItemVO> createRevItemsVO(List<ApRevItem> revItems) {
        List<ApItemVO> items = new ArrayList<>(revItems.size());
        for (ApRevItem item : revItems) {
            ApItemVO itemVO = createItem(item);

            ChangeType changeType = ChangeType.NEW;
            if (item.getOrigObjectId() != null) {
                changeType = ChangeType.UPDATED;
                if (item.getData() == null) {
                    changeType = ChangeType.DELETED;
                }
            }
            itemVO.setChangeType(changeType);
            itemVO.setOrigObjectId(item.getOrigObjectId());
            items.add(itemVO);
        }
        fillRefEntities(items);
        return items;
    }
}
