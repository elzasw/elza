package cz.tacr.elza.controller;

import static cz.tacr.elza.repository.ExceptionThrow.ap;
import static cz.tacr.elza.repository.ExceptionThrow.scope;
import static cz.tacr.elza.repository.ExceptionThrow.version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.transaction.Transactional;

import cz.tacr.elza.controller.vo.MapLayerVO;
import cz.tacr.elza.domain.ApCachedAccessPoint;
import cz.tacr.elza.domain.ApRevision;
import cz.tacr.elza.domain.RevStateApproval;
import cz.tacr.elza.repository.ApCachedAccessPointRepository;
import cz.tacr.elza.service.RevisionService;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.layers.LayersConfig;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.cam.client.ApiException;
import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.cam.schema.cam.QueryResultXml;
import cz.tacr.elza.common.FactoryUtils;
import cz.tacr.elza.common.db.QueryResults;
import cz.tacr.elza.connector.CamConnector;
import cz.tacr.elza.controller.factory.ApFactory;
import cz.tacr.elza.controller.factory.SearchFilterFactory;
import cz.tacr.elza.controller.vo.ApAccessPointCreateVO;
import cz.tacr.elza.controller.vo.ApAccessPointEditVO;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ApAttributesInfoVO;
import cz.tacr.elza.controller.vo.ApCreateTypeVO;
import cz.tacr.elza.controller.vo.ApEidTypeVO;
import cz.tacr.elza.controller.vo.ApExternalSystemSimpleVO;
import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.controller.vo.ApScopeWithConnectedVO;
import cz.tacr.elza.controller.vo.ApStateChangeVO;
import cz.tacr.elza.controller.vo.ApStateHistoryVO;
import cz.tacr.elza.controller.vo.ApTypeVO;
import cz.tacr.elza.controller.vo.ApValidationErrorsVO;
import cz.tacr.elza.controller.vo.ArchiveEntityResultListVO;
import cz.tacr.elza.controller.vo.ArchiveEntityVO;
import cz.tacr.elza.controller.vo.ExtSyncsQueueResultListVO;
import cz.tacr.elza.controller.vo.FileType;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.controller.vo.LanguageVO;
import cz.tacr.elza.controller.vo.RequiredType;
import cz.tacr.elza.controller.vo.SearchFilterVO;
import cz.tacr.elza.controller.vo.SyncsFilterVO;
import cz.tacr.elza.controller.vo.ap.ApViewSettings;
import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.controller.vo.usage.RecordUsageVO;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.SearchType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApExternalIdType;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.SysLanguage;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.ApState.StateApproval;
import cz.tacr.elza.drools.model.ItemSpec;
import cz.tacr.elza.drools.model.ModelAvailable;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.ExternalCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemAptypeRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.PartService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.SettingsService;
import cz.tacr.elza.service.cam.CamService;
import cz.tacr.elza.service.cam.ProcessingContext;


/**
 * REST kontroler pro registry.
 */
@RestController
@RequestMapping(value = "/api/registry", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class ApController {

    private static final Logger logger = LoggerFactory.getLogger(ApController.class);

    @Autowired
    private ApAccessPointRepository accessPointRepository;

    @Autowired
    private ApTypeRepository apTypeRepository;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private ItemAptypeRepository itemAptypeRepository;

    @Autowired
    private ApFactory apFactory;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private PartService partService;

    @Autowired
    private SearchFilterFactory searchFilterFactory;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private CamConnector camConnector;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private CamService camService;

    @Autowired
    private ApCachedAccessPointRepository apCachedAccessPointRepository;

    @Autowired
    private AccessPointCacheService accessPointCacheService;

    @Autowired
    private RevisionService revisionService;

    @Autowired
    private LayersConfig layersConfig;

    /**
     * Nalezne takové záznamy rejstříku, které mají daný typ a jejich textová pole (heslo, popis, poznámka),
     * nebo pole variantního záznamu obsahují hledaný řetězec. V případě, že hledaný řetězec je null, nevyhodnocuje se.
     *
     * @param search            hledaný řetězec, může být null či prázdný (pak vrací vše)
     * @param from              index prvního záznamu, začíná od 0
     * @param count             počet výsledků k vrácení
     * @param apTypeId   IDčka typu záznamu, může být null
     * @param versionId   id verze, podle které se budou filtrovat třídy rejstříků, null - výchozí třídy
     * @param itemSpecId   id specifikace
     * @param state             stav schválení přístupového bodu
     * @param scopeId           id scope, pokud je vyplněn vrací se jen rejstříky s tímto scope
     * @return                  vybrané záznamy dle popisu seřazené za text hesla, nebo prázdná množina
     */
	@Transactional
    @RequestMapping(value = "/search", method = RequestMethod.POST)
    public FilteredResultVO<ApAccessPointVO> findAccessPoint(@RequestParam(required = false) @Nullable final String search,
                                                             @RequestParam final Integer from,
                                                             @RequestParam final Integer count,
                                                             @RequestParam(required = false) @Nullable final Integer apTypeId,
                                                             @RequestParam(required = false) @Nullable final Integer versionId,
                                                             @RequestParam(required = false) @Nullable final Integer itemSpecId,
                                                             @RequestParam(required = false) @Nullable final Integer itemTypeId,
                                                             @RequestParam(required = false) @Nullable final ApState.StateApproval state,
                                                             @RequestParam(required = false) @Nullable final Integer scopeId,
                                                             @RequestParam(required = false) @Nullable final Integer lastRecordNr,
                                                             @RequestParam(required = false) @Nullable final SearchType searchTypeName,
                                                             @RequestParam(required = false) @Nullable final SearchType searchTypeUsername,
                                                             @RequestParam(required = false) @Nullable final RevStateApproval revState,
                                                             @RequestBody(required = false)@Nullable final SearchFilterVO searchFilter) {
        final long foundRecordsCount;
        final List<ApState> foundRecords;

        StaticDataProvider sdp = staticDataService.getData();

        ArrFund fund;
        if (versionId == null) {
            fund = null;
        } else {
            ArrFundVersion version = fundVersionRepository.getOneCheckExist(versionId);
            fund = version.getFund();
        }

        // TODO: Use StaticDataProvider
        //
        Set<Integer> apTypeIds = new HashSet<>();
        if (apTypeId != null) {
            apTypeIds.add(apTypeId);
        }
        apTypeIds = apTypeRepository.findSubtreeIds(apTypeIds);

        if (itemSpecId != null) {
            RulItemSpec spec = sdp.getItemSpecById(itemSpecId);
            if (spec == null) {
                throw new ObjectNotFoundException("Specification not found", ArrangementCode.ITEM_SPEC_NOT_FOUND)
                        .setId(itemSpecId);
            }
            List<Integer> extraApTypeLimit = itemAptypeRepository.findApTypeIdsByItemSpec(spec);
            if (extraApTypeLimit.size() == 0) {
                logger.error("Specification has no associated classes, itemSpecId={}", itemSpecId);
                throw new SystemException("Configuration error, specification without associated classes",
                        BaseCode.SYSTEM_ERROR).set("itemSpecId", itemSpecId);
            }
            apTypeIds = applyApTypeFilter(sdp, apTypeIds, extraApTypeLimit);
        } else if (itemTypeId != null) {
            ItemType itemType = sdp.getItemTypeById(itemTypeId);
            if (itemType == null) {
                throw new ObjectNotFoundException("Item type not found", ArrangementCode.ITEM_TYPE_NOT_FOUND)
                        .setId(itemTypeId);
            }
            List<Integer> extraApTypeLimit = itemAptypeRepository.findApTypeIdsByItemType(itemType.getEntity());
            if (extraApTypeLimit.size() == 0) {
                logger.error("Item type has no associated classes, itemTypeId={}", itemTypeId);
                throw new SystemException("Configuration error, item type without associated classes",
                        BaseCode.SYSTEM_ERROR).set("itemTypeId", itemTypeId);
            }
            apTypeIds = applyApTypeFilter(sdp, apTypeIds, extraApTypeLimit);
        }

        if (StringUtils.isNotEmpty(search) && (!accessPointService.isQueryComplex(searchFilter))) {
            return findAccessPointFulltext(search, from, count, fund, apTypeIds, state, scopeId, searchFilter, sdp);
        }

        if (searchFilter == null && revState == null) {

            Set<ApState.StateApproval> states = state != null ? EnumSet.of(state) : null;

            SearchType searchTypeNameFinal = searchTypeName != null ? searchTypeName : SearchType.FULLTEXT;
            SearchType searchTypeUsernameFinal = searchTypeUsername != null ? searchTypeUsername : SearchType.DISABLED;

            foundRecordsCount = accessPointService.findApAccessPointByTextAndTypeCount(search, apTypeIds, fund, scopeId,
                                                                                       states, searchTypeNameFinal,
                                                                                       searchTypeUsernameFinal);

            foundRecords = accessPointService.findApAccessPointByTextAndType(search, apTypeIds, from, count, fund,
                                                                             scopeId, states, searchTypeNameFinal,
                                                                             searchTypeUsernameFinal);

        } else {

            Set<Integer> scopeIds = accessPointService.getScopeIdsForSearch(fund, scopeId, false);

            Page<ApState> page = accessPointService.findApAccessPointBySearchFilter(searchFilter, apTypeIds, scopeIds,
                                                                                    state, revState, from, count, sdp);
            foundRecords = page.getContent();
            foundRecordsCount = page.getTotalElements();
        }

        final List<ApAccessPoint> accessPoints = foundRecords.stream()
                .map(ApState::getAccessPoint)
                .collect(Collectors.toList());

        final Map<Integer, Integer> typeRuleSetMap = apFactory.getTypeRuleSetMap();

        final Map<Integer, ApIndex> nameMap = accessPointService.findPreferredPartIndexMap(accessPoints);

        return new FilteredResultVO<>(foundRecords, apState ->
                apFactory.createVO(apState,
                        typeRuleSetMap,
                        apState.getAccessPoint(),
                        nameMap.get(apState.getAccessPointId()) != null ? nameMap.get(apState.getAccessPointId()).getValue() : null),
                foundRecordsCount);
    }

    private Set<Integer> applyApTypeFilter(StaticDataProvider sdp, Set<Integer> apTypeIdTree, List<Integer> extraApTypeLimit) {
        if (CollectionUtils.isEmpty(extraApTypeLimit)) {
            return apTypeIdTree;
        }
        // TODO: use StaticDataProvider
        Set<Integer> extraSubTree = apTypeRepository.findSubtreeIds(extraApTypeLimit);
        if (CollectionUtils.isEmpty(apTypeIdTree)) {
            // no limits till now -> apply this subtree
            return extraSubTree;
        } else {
            // remove all except data in extraSubTree
            Set<Integer> result = new HashSet<>(apTypeIdTree);
            for (Integer val : new ArrayList<Integer>(apTypeIdTree)) {
                if (!extraSubTree.contains(val)) {
                    result.remove(val);
                }
            }
            return result;
        }
    }

    private FilteredResultVO<ApAccessPointVO> findAccessPointFulltext(String search,
                                                                      Integer from,
                                                                      Integer count,
                                                                      ArrFund fund,
                                                                      Set<Integer> apTypeIds,
                                                                      ApState.StateApproval state,
                                                                      Integer scopeId,
                                                                      SearchFilterVO searchFilter,
                                                                      StaticDataProvider sdp) {

        Set<Integer> scopeIds = accessPointService.getScopeIdsForSearch(fund, scopeId, false);

        final Map<Integer, Integer> typeRuleSetMap = apFactory.getTypeRuleSetMap();

        QueryResults<ApCachedAccessPoint> cachedAccessPointResult = apCachedAccessPointRepository
                .findApCachedAccessPointisByQuery(search, searchFilter, apTypeIds, scopeIds,
                state, from, count, sdp);

        List<ApAccessPointVO> accessPointVOList = new ArrayList<>();

        for (ApCachedAccessPoint cachedAccessPoint : cachedAccessPointResult.getRecords()) {
            CachedAccessPoint entity = accessPointCacheService.deserialize(cachedAccessPoint.getData());
            String name = apFactory.findAeCachedEntityName(entity);
            accessPointVOList.add(apFactory.createVO(entity.getApState(), typeRuleSetMap, entity, name));
        }

        return new FilteredResultVO<>(accessPointVOList, cachedAccessPointResult.getRecordCount());
    }

    /**
     * Vytvoření přístupového bodu.
     *
     * @param accessPoint zakládaný přístupový bod
     * @return přístupový bod
     */
    @Transactional
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ApAccessPointVO createAccessPoint(@RequestBody final ApAccessPointCreateVO accessPoint) {
        Integer typeId = accessPoint.getTypeId();
        Integer scopeId = accessPoint.getScopeId();

        ApScope scope = accessPointService.getApScope(scopeId);
        ApType type = accessPointService.getType(typeId);
        SysLanguage language = StringUtils.isEmpty(accessPoint.getLanguageCode()) ? null : accessPointService.getLanguage(accessPoint.getLanguageCode());

        ApState apState = accessPointService.createAccessPoint(scope, type, language, accessPoint.getPartForm());
        CachedAccessPoint cachedAccessPoint = accessPointCacheService.findCachedAccessPoint(apState.getAccessPointId());
        if (cachedAccessPoint != null) {
            return apFactory.createVO(cachedAccessPoint);
        }
        return apFactory.createVO(apState, true);
    }

    /**
     * Vytvoření přístupového bodu s přesměrováním
     *
     * @param accessPoint zakládaný přístupový bod
     * @return přístupový bod nebo přesměrování
     */
    /*@Transactional
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ModelAndView createAccessPointWithRedirect(@RequestBody final ApAccessPointCreateVO accessPoint) {
        Integer typeId = accessPoint.getTypeId();
        Integer scopeId = accessPoint.getScopeId();

        ApScope scope = accessPointService.getScope(scopeId);
        ApType type = accessPointService.getType(typeId);
        SysLanguage language = StringUtils.isEmpty(accessPoint.getLanguageCode()) ? null : accessPointService.getLanguage(accessPoint.getLanguageCode());

        ApState apState = accessPointService.createAccessPoint(scope, type, language, accessPoint.getPartForm());
        ApAccessPointVO apAccessPointVO = apFactory.createVO(apState, true);

        if (createEntityRequest.getEntityClass() != null) {
            String response = createEntityRequest.getResponse();
            if (response != null) {
                response = response.replace("{status}", "SUCCESS")
                            .replace("{entityUuid}", apAccessPointVO.getUuid())
                            .replace("{entityId}", String.valueOf(apAccessPointVO.getId()));
            }
            createEntityRequest.setEntityClass(null);
            createEntityRequest.setResponse(null);
            return new ModelAndView("redirect:" + response);
        }

        ModelAndView modelAndView = new ModelAndView("viewPage");
        modelAndView.addObject("ApAccessPointVO", apAccessPointVO);
        return modelAndView;
    }*/

    /**
     * Nastaví pravidla přístupovému bodu podle typu.
     *
     * @param accessPointId identifikátor přístupového bodu
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/setRule", method = RequestMethod.POST)
    public void setRuleAccessPoint(@PathVariable final Integer accessPointId) {
        Assert.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState apState = accessPointService.getStateInternal(accessPoint);
        accessPointService.setRuleAccessPoint(apState);
    }

    /**
     * Vrátí jedno heslo (s variantními hesly) dle id.
     * @param accessPointId      id požadovaného hesla
     * @return              heslo s vazbou na var. hesla
     */
	@Transactional
    @RequestMapping(value = "/{accessPointId}", method = RequestMethod.GET)
    public ApAccessPointVO getAccessPoint(@PathVariable final String accessPointId) {
        ApState apState = accessPointService.getApState(accessPointId);

        ApAccessPointVO vo;
        CachedAccessPoint cachedAccessPoint = accessPointCacheService.findCachedAccessPoint(apState.getAccessPointId());
        if (cachedAccessPoint != null) {
            vo = apFactory.createVO(cachedAccessPoint);
        } else {
            vo = apFactory.createVO(apState, true);
        }

        ApRevision revision = revisionService.findRevisionByState(apState);
        if (revision != null) {
            vo = apFactory.createVO(vo, revision, apState.getAccessPoint());
        }

        return vo;
    }

    /**
     * Aktualizace přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param editVo upravovaná data přístupového bodu
     * @return aktualizovaný záznam
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}", method = RequestMethod.PUT)
    public ApAccessPointVO updateAccessPoint(@PathVariable final Integer accessPointId,
                                             @RequestBody final ApAccessPointEditVO editVo) {
        Validate.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        Validate.notNull(editVo);

        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState oldState = accessPointService.getStateInternal(accessPoint);
        ApState newState = accessPointService.changeApType(accessPointId, editVo.getTypeId());
        accessPointService.generateSync(accessPointId);
        accessPointCacheService.createApCachedAccessPoint(accessPointId);
        CachedAccessPoint cachedAccessPoint = accessPointCacheService.findCachedAccessPoint(accessPointId);
        if (cachedAccessPoint != null) {
            return apFactory.createVO(cachedAccessPoint);
        }
        return apFactory.createVO(newState, true);
    }

    /**
     * Získání seznamu stavů do niž může být přístupový bod přepnut
     * 
     * @param accessPointId
     * @return seznam stavů
     */
    @RequestMapping(value = "/{accessPointId}/nextStates", method = RequestMethod.GET)
    @Transactional
    public List<String> getStateApproval(@PathVariable final Integer accessPointId) {
        Validate.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");

        ApState apState = accessPointService.getStateInternal(accessPointId);
        List<StateApproval> states = accessPointService.getNextStates(apState);

        return states.stream().map(p -> p.name()).collect(Collectors.toList());
    }

    @RequestMapping(value = "/{accessPointId}/nextStatesRevision", method = RequestMethod.GET)
    @Transactional
    public List<String> getStateApprovalRevision(@PathVariable final Integer accessPointId) {
        Validate.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");

        ApState apState = accessPointService.getStateInternal(accessPointId);
        List<StateApproval> states = accessPointService.getNextStatesRevision(apState);

        return states.stream().map(p -> p.name()).collect(Collectors.toList());
    }

    /**
     * Vrátí seznam typů rejstříku (typů hesel).
     *
     * @return  seznam typů rejstříku (typů hesel)
     */
    @RequestMapping(value = "/recordTypes", method = RequestMethod.GET)
	@Transactional
    public List<ApTypeVO> getApTypes() {
        List<ApType> allTypes = apTypeRepository.findAllOrderByNameAsc();

        return apFactory.createTypesWithHierarchy(allTypes);
    }

    /**
     * Vrací všechny jazyky.
     */
    @RequestMapping(value = "/languages", method = RequestMethod.GET)
    @Transactional
    public List<LanguageVO> getAllLanguages() {
        List<SysLanguage> languages = accessPointService.findAllLanguagesOrderByCode();
        return FactoryUtils.transformList(languages, apFactory::createVO);
    }

    /**
     * Vrací typy externích identifikátorů.
     */
    @RequestMapping(value = "/eidTypes", method = RequestMethod.GET)
    @Transactional
    public List<ApEidTypeVO> getAllExternalIdTypes() {
        StaticDataProvider data = staticDataService.getData();
        List<ApExternalIdType> types = data.getApEidTypes();
        return FactoryUtils.transformList(types, apFactory::createVO);
    }

    /**
     * Vrací všechny třídy rejstříků z databáze.
     */
    @RequestMapping(value = "/scopes", method = RequestMethod.GET)
	@Transactional
    public List<ApScopeVO> getAllScopes(){
        List<ApScope> apScopes = scopeRepository.findAllOrderByCode();
        StaticDataProvider staticData = staticDataService.getData();
        return FactoryUtils.transformList(apScopes, s -> ApScopeVO.newInstance(s, staticData));
    }

    /**
     * Vrátí třídu restříku, včetně na ni navázaných tříd.
     */
    @RequestMapping(value = "/scopes/{scopeId}/withConnected", method = RequestMethod.GET)
    @Transactional
    public ApScopeWithConnectedVO getScopeWithConnected(@PathVariable("scopeId") Integer scopeId) {
        ApScope apScope = scopeRepository.findById(scopeId)
                .orElseThrow(scope(scopeId));
        List<ApScope> connectedScopes = scopeRepository.findConnectedByScope(apScope);
        StaticDataProvider staticData = staticDataService.getData();
        return ApScopeWithConnectedVO.newInstance(apScope, staticData, connectedScopes);
    }

    /**
     * Pokud je nastavená verze, vrací třídy napojené na verzi, jinak vrací třídy nastavené v konfiguraci elzy (YAML).
     * @param versionId id verze nebo null
     * @return seznam tříd
     */
    @RequestMapping(value = "/fundScopes", method = RequestMethod.GET)
	@Transactional
    public List<ApScopeVO> getScopeIdsByVersion(@RequestParam(required = false) @Nullable final Integer versionId) {

        ArrFund fund;
        if (versionId == null) {
            fund = null;
        } else {
            ArrFundVersion version = fundVersionRepository.findById(versionId)
                    .orElseThrow(version(versionId));
            fund = version.getFund();
        }

        Set<Integer> scopeIdsByFund = accessPointService.getScopeIdsForSearch(fund, null, false);
        if (CollectionUtils.isEmpty(scopeIdsByFund)) {
            return Collections.emptyList();
        }

        List<ApScope> apScopes = scopeRepository.findAllById(scopeIdsByFund);
        StaticDataProvider staticData = staticDataService.getData();
        List<ApScopeVO> result = FactoryUtils.transformList(apScopes, s -> ApScopeVO.newInstance(s, staticData));
        result.sort(Comparator.comparing(ApScopeVO::getCode));
        return result;
    }

    /**
     * Vložení nové třídy.
     *
     * @return nový objekt třídy
     */
    @Transactional
    @RequestMapping(value = "/scopes", method = RequestMethod.POST)
    public ApScopeVO createScope() {
        List<RulRuleSet> rulRuleSets = ruleService.findAllApRules();
        if (CollectionUtils.isEmpty(rulRuleSets)) {
            throw new SystemException("Neexistují žádná pravidla pro archivní entity");
        }

        ApScope apScope = new ApScope();
        apScope.setCode(UUID.randomUUID().toString());
        apScope.setName("NewScope");
        apScope.setRulRuleSet(rulRuleSets.get(0));
        apScope = accessPointService.saveScope(apScope);
        StaticDataProvider staticData = staticDataService.getData();
        return ApScopeVO.newInstance(apScope, staticData);
    }

    /**
     * Aktualizace třídy.
     *
     * @param scopeId id třídy
     * @param scopeVO objekt třídy
     * @return aktualizovaný objekt třídy
     */
    @Transactional
    @RequestMapping(value = "/scopes/{scopeId}", method = RequestMethod.PUT)
    public ApScopeVO updateScope(@PathVariable final Integer scopeId, @RequestBody final ApScopeVO scopeVO) {
        Assert.notNull(scopeId, "Identifikátor scope musí být vyplněn");
        Assert.notNull(scopeVO, "Scope musí být vyplněn");

        Assert.isTrue(
                scopeId.equals(scopeVO.getId()),
                "V url požadavku je odkazováno na jiné ID (" + scopeId + ") než ve VO (" + scopeVO.getId() + ")."
        );

        StaticDataProvider staticData = staticDataService.getData();
        ApScope apScope = scopeVO.createEntity(staticData);
        apScope = accessPointService.saveScope(apScope);
        return ApScopeVO.newInstance(apScope, staticData);
    }

    /**
     * Provázání tříd.
     *
     * @param scopeId ID třídy ke které se bude vázat
     * @param connectedScopeId ID třídy která bude provázána
     */
    @Transactional
    @RequestMapping(value = "/scopes/{scopeId}/connect", method = RequestMethod.POST)
    public void connectScope(@PathVariable("scopeId") final Integer scopeId, @RequestBody final Integer connectedScopeId) {
        Assert.notNull(connectedScopeId, "Identifikátor vázaného scope musí být vyplněn");

        if (scopeId.equals(connectedScopeId)) {
            throw new BusinessException("Třídu rejstříku nelze navázat sama na sebe", RegistryCode.CANT_CONNECT_SCOPE_TO_SELF);
        }

        final ApScope scope = scopeRepository.findById(scopeId)
                .orElseThrow(scope(scopeId));
        final ApScope connectedScope = scopeRepository.findById(connectedScopeId)
                .orElseThrow(scope(connectedScopeId));
        accessPointService.connectScope(scope, connectedScope);
    }

    /**
     * Zrušení provázání tříd.
     *
     * @param scopeId ID třídy
     * @param connectedScopeId ID třídy k odpojení
     */
    @Transactional
    @RequestMapping(value = "/scopes/{scopeId}/disconnect", method = RequestMethod.POST)
    public void disconnectScope(@PathVariable("scopeId") final Integer scopeId, @RequestBody final Integer connectedScopeId) {
        Assert.notNull(connectedScopeId, "Identifikátor vázané třídy musí být vyplněn");

        final ApScope scope = scopeRepository.findById(scopeId)
                .orElseThrow(scope(scopeId));
        final ApScope connectedScope = scopeRepository.findById(connectedScopeId)
                .orElseThrow(scope(connectedScopeId));
        accessPointService.disconnectScope(scope, connectedScope);
    }

    /**
     * Smazání třídy. Třída nesmí být napojena na rejstříkové heslo.
     *
     * @param scopeId id třídy.
     */
    @Transactional
    @RequestMapping(value = "/scopes/{scopeId}", method = RequestMethod.DELETE)
    public void deleteScope(@PathVariable final Integer scopeId) {
        ApScope scope = scopeRepository.findById(scopeId)
                .orElseThrow(scope(scopeId));
        accessPointService.deleteScope(scope);
    }

    /**
     * Vyhledá všechny externí systémy.
     *
     * @return seznam externích systémů
     */
    @RequestMapping(value = "/externalSystems", method = RequestMethod.GET)
	@Transactional
    public List<ApExternalSystemSimpleVO> findAllExternalSystems() {
		List<ApExternalSystem> extSystems = externalSystemService.findAllApSystem();
		return FactoryUtils.transformList(extSystems, ApExternalSystemSimpleVO::newInstance);
    }

    /**
     * Najde použití rejstříku.
     *
     * @param accessPointId identifikátor rejstříku
     *
     * @return použití rejstříku
     */
    @RequestMapping(value = "/{accessPointId}/usage", method = RequestMethod.GET)
    @Transactional
    public RecordUsageVO findUsage(@PathVariable final Integer accessPointId) {
    	ApAccessPoint apAccessPoint = accessPointRepository.getOneCheckExist(accessPointId);
    	return accessPointService.findRecordUsage(apAccessPoint);
    }

    /**
     * Nahrazení rejstříku
     *
     * @param accessPointId ID nahrazovaného rejstříku
     * @param replacedId ID rejstříku kterým budeme nahrazovat
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/replace", method = RequestMethod.POST)
    public void replace(@PathVariable final Integer accessPointId, @RequestBody final Integer replacedId) {

        final ApAccessPoint replaced = accessPointService.getAccessPointInternal(accessPointId);
        final ApAccessPoint replacement = accessPointService.getAccessPointInternal(replacedId);

        ApState replacedState = accessPointService.getStateInternal(replaced);
        ApState replacementState = accessPointService.getStateInternal(replacement);
        accessPointService.replace(replacedState, replacementState);
    }

    /**
     * Vyhledání historie stavů seřazené sestupně dle stáří (první je tedy aktuální).
     *
     * @param accessPointId identifikátor přístupového bodu
     * @return seznam stavů v historii
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/history", method = RequestMethod.GET)
    public List<ApStateHistoryVO> findStateHistories(@PathVariable("accessPointId") final Integer accessPointId) {
        ApAccessPoint apAccessPoint = accessPointService.getAccessPoint(accessPointId);
        List<ApState> states = accessPointService.findApStates(apAccessPoint);
        return apFactory.createStateHistoriesVO(states);
    }

    /**
     * Změna stavu přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/state", method = RequestMethod.POST)
    public void changeState(@PathVariable("accessPointId") final Integer accessPointId,
                            @RequestBody ApStateChangeVO stateChange) {
        Validate.notNull(stateChange.getState(), "AP State is null");

        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);
        ApState state = accessPointService.getApState(accessPoint);
        ApRevision revision = revisionService.findRevisionByState(state);

        // Nelze změnit stav archivní entity, která má revizi
        if (revision != null) {
            throw new BusinessException("Nemůžeme změnit stav entity, která má revizi", RegistryCode.CANT_CHANGE_STATE_ENTITY_WITH_REVISION);
        }

        accessPointService.updateApState(accessPoint, stateChange.getState(), stateChange.getComment(), stateChange.getTypeId(), stateChange.getScopeId());
        accessPointService.generateSync(accessPointId);
        accessPointCacheService.createApCachedAccessPoint(accessPointId);
    }

    /**
     * Vyhledání přístupových bodů pro návazný vztah
     *
     * @param from od které položky vyhledávat
     * @param max maximální počet záznamů, které najednou vrátit
     * @param itemTypeId identifikátor typu vztahu
     * @param itemSpecId identifikátor specifikace vztahu
     * @param scopeId identifikátor oblasti
     * @param filter parametry hledání
     * @return výsledek hledání
     *
     */
    @Transactional
    @RequestMapping(value = "/search/rel", method = RequestMethod.POST)
    public ArchiveEntityResultListVO findAccessPointForRel(@RequestParam(name = "from", defaultValue = "0", required = false) final Integer from,
                                                           @RequestParam(name = "max", defaultValue = "50", required = false) final Integer max,
                                                           @RequestParam(name = "itemTypeId") final Integer itemTypeId,
                                                           @RequestParam(name = "itemSpecId", required = false) final Integer itemSpecId,
                                                           @RequestParam(name = "scopeId", required = true) final Integer scopeId,
                                                           @RequestBody final SearchFilterVO filter) {
        if (from < 0) {
            throw new SystemException("Parametr from musí být >=0", BaseCode.PROPERTY_IS_INVALID);
        }
        StaticDataProvider sdp = staticDataService.getData();
        List<Integer> apTypes = accessPointService.findApTypeIdsByItemTypeAndItemSpec(itemTypeId, itemSpecId);
        Set<Integer> apTypeIds = apTypeRepository.findSubtreeIds(apTypes);
        Set<Integer> scopeIds = accessPointService.getScopeIdsForSearch(null, scopeId, true);
        QueryResults<ApCachedAccessPoint> cachedAccessPointResult = apCachedAccessPointRepository
                .findApCachedAccessPointisByQuery(null, filter, apTypeIds, scopeIds,
                                                  null, from, max, sdp);

        /*
        filter.setAeTypeIds(apTypes);
        return accessPointService.findAccessPointsForRel(from, max, scopeId, filter);
        */
        ArchiveEntityResultListVO ret = new ArchiveEntityResultListVO();
        ret.setTotal(cachedAccessPointResult.getRecordCount());
        List<ArchiveEntityVO> data;
        List<ApCachedAccessPoint> records = cachedAccessPointResult.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            data = Collections.emptyList();
        } else {
            data = new ArrayList(records.size());
            for (ApCachedAccessPoint record : records) {
                CachedAccessPoint entity = accessPointCacheService.deserialize(record.getData());
                ArchiveEntityVO ae = ArchiveEntityVO.valueOf(entity);
                data.add(ae);
            }
        }
        ret.setData(data);
        return ret;
    }

    /**
     * Založení nové části přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @param apPartFormVO data pro vytvoření části
     */
    @Transactional
    @RequestMapping(value = "{accessPointId}/part", method = RequestMethod.POST)
    public void createPart(@PathVariable final Integer accessPointId,
                           @RequestBody final ApPartFormVO apPartFormVO) {
        ApAccessPoint apAccessPoint = accessPointRepository.findById(accessPointId)
                .orElseThrow(ap(accessPointId));
        ApState state = accessPointService.getStateInternal(apAccessPoint);
        accessPointService.hasPermissionForEditingConfirmed(state);
        ApRevision revision = revisionService.findRevisionByState(state);
        if (revision != null) {
            revisionService.createPart(revision, apPartFormVO);
        } else {
            ApPart apPart = partService.createPart(apAccessPoint, apPartFormVO);
            accessPointService.generateSync(apAccessPoint, apPart);
            accessPointCacheService.createApCachedAccessPoint(accessPointId);
        }
    }

    /**
     * Úprava části přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @param partId identifikátor upravované části
     * @param apPartFormVO data pro úpravu části
     */
    @Transactional
    @RequestMapping(value = "{accessPointId}/part/{partId}", method = RequestMethod.POST)
    public void updatePart(@PathVariable final Integer accessPointId,
                           @PathVariable final Integer partId,
                           @RequestBody final ApPartFormVO apPartFormVO) {
        ApAccessPoint apAccessPoint = accessPointRepository.findById(accessPointId).orElseThrow(ap(accessPointId));
        ApState state = accessPointService.getStateInternal(apAccessPoint);
        accessPointService.hasPermissionForEditingConfirmed(state);
        ApPart apPart = partService.getPart(partId);
        ApRevision revision = revisionService.findRevisionByState(state);
        if (revision != null) {
            revisionService.updatePart(revision, apPart, apPartFormVO);
        } else {
            if (accessPointService.updatePart(apAccessPoint, apPart, apPartFormVO)) {
                accessPointCacheService.createApCachedAccessPoint(accessPointId);
            }
        }
    }

    /**
     * Úprava části přístupového bodu.
     *
     * @param id identifikátor přístupového bodu (PK)
     * @param partId identifikátor upravované části
     * @param apPartFormVO data pro úpravu části
     */
    @Transactional
    @RequestMapping(value = "/revision/{id}/part/{partId}", method = RequestMethod.POST)
    public void updateRevisionPart(@PathVariable final Integer id,
                              @PathVariable final Integer partId,
                              @RequestBody final ApPartFormVO apPartFormVO) {
        ApState state = accessPointService.getStateInternal(id);
        ApRevision revision = revisionService.findRevisionByState(state);
        revisionService.updatePart(revision, partId, apPartFormVO);
    }

    /**
     * Úprava části přístupového bodu.
     *
     * @param id identifikátor přístupového bodu (PK)
     * @param state stav do kterého se má entita po merge uvést
     */
    @Transactional
    @RequestMapping(value = "/revision/{id}/merge", method = RequestMethod.POST)
    public void mergeRevision(@PathVariable final Integer id,
                              @RequestParam(required = false) @Nullable final ApState.StateApproval state) {
        ApState apState = accessPointService.getStateInternal(id);
        revisionService.mergeRevision(apState, state);
    }


    /**
     * Smazání části přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @param partId identifikátor mazané části
     */
    @Transactional
    @RequestMapping(value = "{accessPointId}/part/{partId}", method = RequestMethod.DELETE)
    public void deletePart(@PathVariable final Integer accessPointId,
                           @PathVariable final Integer partId) {
        ApAccessPoint apAccessPoint = accessPointRepository.findById(accessPointId)
                .orElseThrow(ap(accessPointId));
        ApState state = accessPointService.getStateInternal(apAccessPoint);
        accessPointService.hasPermissionForEditingConfirmed(state);

        ApRevision revision = revisionService.findRevisionByState(state);
        if (revision != null) {
            revisionService.deletePart(revision, partId);
        } else {
            partService.deletePart(apAccessPoint, partId);
            accessPointService.generateSync(accessPointId);
            accessPointCacheService.createApCachedAccessPoint(accessPointId);
        }
    }

    /**
     * Nastavení preferovaného jména přístupového bodu.
     * Možné pouze pro části typu Označení.
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @param partId identifikátor části, kterou nastavujeme jako preferovanou
     */
    @Transactional
    @RequestMapping(value = "{accessPointId}/part/{partId}/prefer-name", method = RequestMethod.PUT)
    public void setPreferName(@PathVariable final Integer accessPointId,
                              @PathVariable final Integer partId) {
        ApAccessPoint apAccessPoint = accessPointRepository.findById(accessPointId)
                .orElseThrow(ap(accessPointId));
        ApState state = accessPointService.getStateInternal(apAccessPoint);
        accessPointService.hasPermissionForEditingConfirmed(state);
        ApRevision revision = revisionService.findRevisionByState(state);
        if (revision != null) {
            revisionService.setPreferName(revision, partId);
        } else {
            ApPart apPart = partService.getPart(partId);
            accessPointService.setPreferName(apAccessPoint, apPart);
            accessPointCacheService.createApCachedAccessPoint(accessPointId);
        }
    }

    /**
     * Validace přístupového bodu
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @return validační chyby přístupového bodu
     */
    @Transactional
    @RequestMapping(value = "{accessPointId}/validate", method = RequestMethod.GET)
    public ApValidationErrorsVO validateAccessPoint(@PathVariable final Integer accessPointId) {
        ApState apState = accessPointService.getApState(accessPointId);

        return apFactory.createValidationVO(apState.getAccessPoint());
    }

    /**
     * Zjištění povinných a možných atributů pro zakládání nového přístupového bodu nebo nové části
     *
     * @param apAccessPointCreateVO průběžná data pro založení
     * @return vyhodnocené typy a specifikace atributů, které jsou třeba pro založení přístupového bodu nebo části
     */
    @Transactional
    @RequestMapping(value = "/available/items", method = RequestMethod.POST)
    public ApAttributesInfoVO getAvailableItems(@RequestBody final ApAccessPointCreateVO apAccessPointCreateVO) {
        if (false) {
            boolean hasHlavniCast = false;
            for (ApItemVO item : apAccessPointCreateVO.getPartForm().getItems()) {
                if (item.getTypeId() == 11) {
                    hasHlavniCast = true;
                    break;
                }
            }

            final boolean hasHlavniCast2 = hasHlavniCast;
            List<ApCreateTypeVO> list = staticDataService.getData().getItemTypes().stream()
                    .map(x -> {
                        ApCreateTypeVO vo = new ApCreateTypeVO();
                        vo.setItemTypeId(x.getItemTypeId());
                        vo.setRequiredType(RequiredType.POSSIBLE);
                        vo.setRepeatable(false);

                        if (hasHlavniCast2 && x.getItemTypeId() < 10) {
                            vo.setRequiredType(RequiredType.REQUIRED);
                        }
                        return vo;
                    })
                    .collect(Collectors.toList());
            ApAttributesInfoVO aeAttributesInfoVO = new ApAttributesInfoVO();
            aeAttributesInfoVO.setAttributes(list);
            aeAttributesInfoVO.setErrors(Collections.emptyList());
            return aeAttributesInfoVO;
        }

        List<ApCreateTypeVO> result = new ArrayList<>();
        ModelAvailable modelAvailable = ruleService.executeAvailable(apAccessPointCreateVO);
        for (cz.tacr.elza.drools.model.ItemType itemType : modelAvailable.getItemTypes()) {
            if (itemType.getRequiredType() != cz.tacr.elza.drools.model.RequiredType.IMPOSSIBLE) {
                ApCreateTypeVO createTypeVO = new ApCreateTypeVO();
                createTypeVO.setItemTypeId(itemType.getId());
                createTypeVO.setRepeatable(itemType.isRepeatable());
                createTypeVO.setRequiredType(RequiredType.fromValue(itemType.getRequiredType().name()));
                List<Integer> specIds = itemType.getSpecs().stream()
                        .filter(s -> s.getRequiredType() != cz.tacr.elza.drools.model.RequiredType.IMPOSSIBLE)
                        .map(ItemSpec::getId)
                        .collect(Collectors.toList());
                createTypeVO.setItemSpecIds(specIds);
                result.add(createTypeVO);
            }
        }

        List<String> errors = ruleService.validateAvailableItems(modelAvailable);

        ApAttributesInfoVO apAttributesInfoVO = new ApAttributesInfoVO();
        apAttributesInfoVO.setAttributes(result);
        apAttributesInfoVO.setErrors(errors);

        return apAttributesInfoVO;
    }

    /**
     * Vyhledání archivních entit v externím systému
     *
     * @param from od které položky vyhledávat
     * @param max maximální počet záznamů, které najednou vrátit
     * @param externalSystemCode kód externího systému
     * @param filter parametry hledání
     * @return výsledek hledání
     */
    @Transactional
    @RequestMapping(value = "/external/search", method = RequestMethod.POST)
    public ArchiveEntityResultListVO findArchiveEntitiesInExternalSystem(@RequestParam(name = "from", defaultValue = "0", required = false) final Integer from,
                                                                         @RequestParam(name = "max", defaultValue = "50", required = false) final Integer max,
                                                                         @RequestParam(name = "externalSystemCode") final String externalSystemCode,
                                                                         @RequestBody final SearchFilterVO filter) {
        if (from < 0) {
            throw new SystemException("Parametr from musí být >=0", BaseCode.PROPERTY_IS_INVALID);
        }
        int fromPage = from / max;

        ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);
        QueryResultXml result;
        try {
            result = camConnector.search(fromPage + 1, max, searchFilterFactory.createQueryParamsDef(filter), apExternalSystem);
        } catch (ApiException e) {
            throw prepareSystemException(e);
        }
        return searchFilterFactory.createArchiveEntityVoListResult(result);
    }

    /**
     * Vyhledání položek ve frontě na synchronizaci.
     *
     * @param from od které položky vyhledávat
     * @param max maximální počet záznamů, které najednou vrátit
     * @param externalSystemCode kód externího systému
     * @param filter parametry hledání
     * @return výsledek hledání
     */
    @Transactional
    @RequestMapping(value = "/external/syncs", method = RequestMethod.POST)
    public ExtSyncsQueueResultListVO findExternalSyncs(@RequestParam(name = "from", defaultValue = "0", required = false) final Integer from,
                                                       @RequestParam(name = "max", defaultValue = "50", required = false) final Integer max,
                                                       @RequestParam(name = "externalSystemCode") final String externalSystemCode,
                                                       @RequestBody final SyncsFilterVO filter) {
        if (from < 0) {
            throw new SystemException("Parametr from musí být >=0", BaseCode.PROPERTY_IS_INVALID);
        }
        return accessPointService.findExternalSyncs(from, max, externalSystemCode, filter);
    }

    /**
     * Převzetí entity z externího systému
     *
     * @param archiveEntityId identifikátor entity v externím systému
     * @param scopeId identifikátor třídy rejstříku
     * @param externalSystemCode kód externího systému
     * @return identifikátor přístupového bodu
     */
    @Transactional
    @RequestMapping(value = "/external/{archiveEntityId}/take", method = RequestMethod.POST)
    public Integer takeArchiveEntity(@PathVariable("archiveEntityId") final String archiveEntityId,
                                     @RequestParam final Integer scopeId,
                                     @RequestParam final String externalSystemCode) {
        StaticDataProvider sdp = this.staticDataService.getData();
        ApExternalSystem apExternalSystem = sdp.getApExternalSystemByCode(externalSystemCode);
        Validate.notNull(apExternalSystem, "External system code is incorrect: {}", externalSystemCode);

        EntityXml entity;
        try {
            entity = camConnector.getEntity(archiveEntityId, apExternalSystem);
        } catch (ApiException e) {
            throw prepareSystemException(e);
        }

        ApBinding binding = externalSystemService.findByValueAndExternalSystem(archiveEntityId, apExternalSystem);
        if (binding != null) {
            // check state
            Optional<ApBindingState> bindingState = externalSystemService.getBindingState(binding);
            bindingState.ifPresent(bs -> {
                throw new SystemException("Archival entity already imported", ExternalCode.ALREADY_IMPORTED)
                        .set("externalSystemCode", externalSystemCode)
                        .set("archiveEntityId", archiveEntityId)
                        .set("bindingStateId", bs.getBindingStateId())
                        .set("accessPointId", bs.getAccessPointId());

            });
        }

        ApScope scope = accessPointService.getApScope(scopeId);
        ProcessingContext procCtx = new ProcessingContext(scope, apExternalSystem, staticDataService);
        List<ApState> apStates = camService.createAccessPoints(procCtx, Collections.singletonList(entity));
        if (apStates.size() != 1) {
            throw new BusinessException("Failed to create accesspoint from entity", BaseCode.IMPORT_FAILED);
        }

        ApState apState = apStates.get(0);
        return apState.getAccessPointId();
    }

    /**
     * Propojení archivní entity z externího systém na existující přístupový bod
     *
     * @param archiveEntityId identifikátor entity v externím systému
     * @param accessPointId identifikátor přístupového bodu
     * @param externalSystemCode kód externího systému
     */
    @Transactional
    @RequestMapping(value = "/external/{archiveEntityId}/connect/{accessPointId}", method = RequestMethod.POST)
    public void connectArchiveEntity(@PathVariable("archiveEntityId") final String archiveEntityId,
                                     @PathVariable("accessPointId") final Integer accessPointId,
                                     @RequestParam("externalSystemCode") final String externalSystemCode,
                                     @RequestParam("replace") final Boolean replace) {
        Validate.notNull(accessPointId, "Identifikátor přístupového bodu není vyplněn");

        ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);

        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);
        ApState state = accessPointService.getStateInternal(accessPoint);
        ApScope scope = state.getScope();
        accessPointService.checkUniqueExtSystem(accessPoint, apExternalSystem);
        
        ProcessingContext procCtx = new ProcessingContext(scope, apExternalSystem, staticDataService);

        EntityXml entity;
        try {
            entity = camConnector.getEntity(archiveEntityId, apExternalSystem);
        } catch (ApiException e) {
            throw prepareSystemException(e);
        }
        camService.connectAccessPoint(state, entity, procCtx, replace);
    }

    /**
     * Zápis přistupového bodu do externího systému
     * 
     * Metoda zapíš nový AP nebo aktualizuje stávající.
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param externalSystemCode kód externího systému
     */
    @Transactional
    @RequestMapping(value = {"/external/save/{accessPointId}",
    		"/external/update/{accessPointId}"}, method = RequestMethod.POST)
    public void saveAccessPoint(@PathVariable("accessPointId") final Integer accessPointId,
                                @RequestParam final String externalSystemCode) {
        camService.createExtSyncsQueueItem(accessPointId, externalSystemCode);
    }

    /**
     * Synchronizace přístupového bodu z externího systému
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param externalSystemCode kód externího systému
     */
    @Transactional
    @RequestMapping(value = "/external/synchronize/{accessPointId}", method = RequestMethod.POST)
    public void synchronizeAccessPoint(@PathVariable("accessPointId") final Integer accessPointId,
                                       @RequestParam final String externalSystemCode) {
        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);
        ApState state = accessPointService.getStateInternal(accessPoint);

        // kontrola přístupových práv a možností synchronizace 
        accessPointService.hasPermissionToSynchronizeFromExternaSystem(state);

        ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);
        ApBindingState bindingState = externalSystemService.findByAccessPointAndExternalSystem(accessPoint, apExternalSystem);
        ApBinding binding = bindingState.getBinding();

        EntityXml entity;
        try {
            entity = camConnector.getEntity(binding.getValue(), apExternalSystem);
        } catch (ApiException e) {
            throw prepareSystemException(e);
        }
        ProcessingContext procCtx = new ProcessingContext(state.getScope(), apExternalSystem, staticDataService);
        camService.synchronizeAccessPoint(procCtx, state, bindingState, binding, entity, false);
    }

    private AbstractException prepareSystemException(ApiException e) {
        return new SystemException("Došlo k chybě při komunikaci s externím systémem.", e)
                .set("responseBody", e.getResponseBody())
                .set("responseCode", e.getCode())
                .set("responseHeaders", e.getResponseHeaders());
    }

    /**
     * Zrušení vazby na externí systém
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param externalSystemCode kód externího systému
     */
    @Transactional
    @RequestMapping(value = "/external/disconnect/{accessPointId}", method = RequestMethod.POST)
    public void disconnectAccessPoint(@PathVariable("accessPointId") final Integer accessPointId,
                                      @RequestParam final String externalSystemCode) {
        accessPointService.disconnectAccessPoint(accessPointId, externalSystemCode);
    }

    /**
     * Založení přístupových bodů z návazných entit z externího systému
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param externalSystemCode kód externího systému
     */
    @Transactional
    @RequestMapping(value = "/external/take-rel/{accessPointId}", method = RequestMethod.POST)
    public void takeRelArchiveEntities(@PathVariable("accessPointId") final Integer accessPointId,
                                       @RequestParam final String externalSystemCode) {
        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);
        ApState state = accessPointService.getStateInternal(accessPoint);
        ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);

        List<String> archiveEntities = accessPointService.findRelArchiveEntities(accessPoint);
        List<EntityXml> entities = new ArrayList<>();

        try {
            if (CollectionUtils.isNotEmpty(archiveEntities)) {
                for (String archiveEntityId : archiveEntities) {
                    entities.add(camConnector.getEntity(archiveEntityId, apExternalSystem));
                }
            }
        } catch (ApiException e) {
            throw prepareSystemException(e);
        }
        ProcessingContext procCtx = new ProcessingContext(state.getScope(), apExternalSystem, staticDataService);
        camService.createAccessPoints(procCtx, entities);
    }

    @Transactional
    @RequestMapping(value = "/ap-types/view-settings", method = RequestMethod.GET)
    public ApViewSettings getApTypeViewSettings() {
        UISettings.SettingsType itemTypes = UISettings.SettingsType.ITEM_TYPES;
        UISettings.SettingsType partsOrder = UISettings.SettingsType.PARTS_ORDER;

        List<RulRuleSet> ruleRules = ruleService.findAllApRules();

        ApViewSettings result = new ApViewSettings();
        Map<Integer, ApViewSettings.ApViewSettingsRule> map = new HashMap<>();
        for (RulRuleSet ruleRule : ruleRules) {
            List<UISettings> itemTypesSettings = settingsService.getGlobalSettings(itemTypes.toString(), itemTypes.getEntityType(), ruleRule.getRuleSetId());
            List<UISettings> partsOrderSettings = settingsService.getGlobalSettings(partsOrder.toString(), partsOrder.getEntityType(), ruleRule.getRuleSetId());
            ApViewSettings.ApViewSettingsRule settings = apFactory.createApTypeViewSettings(ruleRule, itemTypesSettings, partsOrderSettings);
            map.put(settings.getRuleSetId(), settings);
        }
        result.setRules(map);
        result.setTypeRuleSetMap(apFactory.getTypeRuleSetMap());

        return result;
    }

    /**
     * Export souřadnic do formátu KML/GML
     *
     * @param fileType Typ souboru
     * @param itemId Identifikátor itemu
     * @return Soubor se souřadnicemi
     */
    @Transactional
    @RequestMapping(value = "/export/coordinates/{itemId}",
            consumes = {"*/*"},
            produces = { "application/octet-stream", "application/gml+xml", "application/vnd.google-earth.kml+xml" },
            method = RequestMethod.GET)
    public ResponseEntity<Resource> exportCoordinates(@RequestParam final FileType fileType,
                                                      @PathVariable("itemId") final Integer itemId) {
        return new ResponseEntity<>(accessPointService.exportCoordinates(fileType, itemId), accessPointService.createCoordinatesHeaders(fileType), HttpStatus.OK);
    }

    /**
     * Import souřadnic ve formátu KML/GML
     *
     * @param fileType Typ souboru
     * @param body Soubor se souřadnicemi
     * @return Souřadnice převedené do řetězce
     */
    @Transactional
    @RequestMapping(value = "/import/coordinates", method = RequestMethod.POST)
    public String importCoordinates(@RequestParam final FileType fileType,
                                    @RequestBody(required = false) Resource body) {
        return accessPointService.importCoordinates(fileType, body);
    }

    /**
     * Odstranění záznamu z tabulky ExtSyncsQueueItem
     *
     * @param itemId
     */
    @Transactional
    @RequestMapping(value = "/external/syncs/{extSyncItemId}", method = RequestMethod.DELETE) 
    public void deleteExternalSync(@PathVariable("extSyncItemId") final Integer itemId) {
        externalSystemService.deleteQueueItem(itemId);
    }

    @RequestMapping(value = "/layer/configuration", method = RequestMethod.GET)
    public List<MapLayerVO> mapLayerConfiguration() {
        return layersConfig.getLayers();
    }

}
