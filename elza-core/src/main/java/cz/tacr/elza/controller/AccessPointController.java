package cz.tacr.elza.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.factory.ApFactory;
import cz.tacr.elza.controller.vo.AccessPointBatchExportParams;
import cz.tacr.elza.controller.vo.AccessPointSearchParams;
import cz.tacr.elza.controller.vo.ApAccessPointSearchResult;
import cz.tacr.elza.controller.vo.ApAdvanceSearchFilter;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.ApStateUpdate;
import cz.tacr.elza.controller.vo.ApValidationIssues;
import cz.tacr.elza.controller.vo.AutoValue;
import cz.tacr.elza.controller.vo.CopyAccessPointDetail;
import cz.tacr.elza.controller.vo.CreatedPart;
import cz.tacr.elza.controller.vo.DeleteAccessPointDetail;
import cz.tacr.elza.controller.vo.DeleteAccessPointsDetail;
import cz.tacr.elza.controller.vo.EntityRef;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.controller.vo.InvalidatedEntities;
import cz.tacr.elza.controller.vo.Participant;
import cz.tacr.elza.controller.vo.ReplaceType;
import cz.tacr.elza.controller.vo.ResultAutoItems;
import cz.tacr.elza.controller.vo.RevStateChange;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.controller.vo.ApSearchType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.dataexchange.output.IOExportAccessPointsCsv;
import cz.tacr.elza.dataexchange.output.IOExportWorker;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemAptypeRepository;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApRevPart;
import cz.tacr.elza.domain.ApRevState;
import cz.tacr.elza.domain.ApRevision;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApState.StateApproval;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.RevStateApproval;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SyncImpossibleException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.groovy.GroovyItem;
import cz.tacr.elza.service.AccessPointConnectorService;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.GroovyService;
import cz.tacr.elza.service.PartService;
import cz.tacr.elza.service.RevisionPartService;
import cz.tacr.elza.service.RevisionService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.TaskService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.domain.UsrUser;

@RestController
@RequestMapping("/api/v1")
public class AccessPointController implements AccesspointsApi {

    @Autowired
    AccessPointConnectorService apConnectService;

    @Autowired
    AccessPointCacheService apCacheService;

    @Autowired
    RevisionPartService revisionPartService;

    @Autowired
    AccessPointService accessPointService;

    @Autowired
    StaticDataService staticDataService;

    @Autowired
    RevisionService revisionService;

	@Autowired
	private TaskService taskService;

	@Autowired
    GroovyService groovyService;

    @Autowired
    PartService partService;

    @Autowired
    RuleService ruleService;

    @Autowired
    ApFactory apFactory;

    @Autowired
    UserService userService;

    @Autowired
    IOExportWorker ioExportWorker;

    @Autowired
    ApTypeRepository apTypeRepository;

    @Autowired
    FundVersionRepository fundVersionRepository;

    @Autowired
    ItemAptypeRepository itemAptypeRepository;

    private static final Logger logger = LoggerFactory.getLogger(AccessPointController.class);

    // POST /accesspoint/search
    @Override
    @Transactional
    public ResponseEntity<ApAccessPointSearchResult> accessPointSearch(@Valid AccessPointSearchParams params) {
        if (params == null) {
            params = new AccessPointSearchParams();
        }
        StaticDataProvider sdp = staticDataService.getData();

        ArrFund fund = null;
        if (params.getVersionId() != null) {
            ArrFundVersion version = fundVersionRepository.getOneCheckExist(params.getVersionId());
            fund = version.getFund();
        }

        Set<Integer> apTypeIds = new HashSet<>();
        if (params.getApTypeId() != null) {
            apTypeIds.add(params.getApTypeId());
        }
        apTypeIds = apTypeRepository.findSubtreeIds(apTypeIds);
        apTypeIds = applyItemTypeOrSpecLimit(sdp, apTypeIds, params.getItemTypeId(), params.getItemSpecId());

        StateApproval state = params.getState() == null
                ? null
                : StateApproval.valueOf(params.getState().getValue());
        RevStateApproval revState = params.getRevState() == null
                ? null
                : RevStateApproval.valueOf(params.getRevState().getValue());

        // Start from the advanced filter (if any); top-level scalar search overrides searchFilter.search.
        ApAdvanceSearchFilter searchFilter = params.getSearchFilter();
        String searchText = params.getSearch();
        if (searchText != null && !searchText.isBlank()) {
            if (searchFilter == null) {
                searchFilter = new ApAdvanceSearchFilter();
            }
            searchFilter.setSearch(searchText);
        } else if (searchFilter != null && searchFilter.getSearch() != null && !searchFilter.getSearch().isBlank()) {
            // Allow the filter to carry the search if the top-level scalar is empty.
            searchText = searchFilter.getSearch();
        }

        Integer from = params.getFrom();
        Integer count = params.getCount();
        Integer scopeId = params.getScopeId();

        FilteredResultVO<cz.tacr.elza.controller.vo.ApAccessPointVO> result;
        if (StringUtils.isNotEmpty(searchText)) {
            result = accessPointService.findUseLuceneQueries(searchText, searchFilter, fund, apTypeIds,
                    scopeId, state, revState, from, count, sdp);
        } else {
            result = accessPointService.findUseCriteriaQuery(searchText, searchFilter,
                    params.getSearchTypeName(), params.getSearchTypeUsername(),
                    fund, apTypeIds, scopeId, state, revState, from, count, sdp);
        }

        ApAccessPointSearchResult body = new ApAccessPointSearchResult();
        body.setCount(result.getCount());
        body.setRows(result.getRows() == null ? List.of() : result.getRows());
        return ResponseEntity.ok(body);
    }

    // POST /accesspoint/export
    @Override
    @Transactional
    public ResponseEntity<Integer> accessPointBatchExport(@Valid AccessPointBatchExportParams params) {
        if (params == null) {
            params = new AccessPointBatchExportParams();
        }

        StaticDataProvider sdp = staticDataService.getData();

        ArrFund fund = null;
        if (params.getVersionId() != null) {
            ArrFundVersion version = fundVersionRepository.getOneCheckExist(params.getVersionId());
            fund = version.getFund();
        }

        Set<Integer> apTypeIds = new HashSet<>();
        if (params.getApTypeId() != null) {
            apTypeIds.add(params.getApTypeId());
        }
        apTypeIds = apTypeRepository.findSubtreeIds(apTypeIds);
        apTypeIds = applyItemTypeOrSpecLimit(sdp, apTypeIds, params.getItemTypeId(), params.getItemSpecId());

        Set<Integer> scopeIds = accessPointService.getScopeIdsForSearch(fund, params.getScopeId(), false);

        StateApproval state = params.getState() == null
                ? null
                : StateApproval.valueOf(params.getState().getValue());
        RevStateApproval revState = params.getRevState() == null
                ? null
                : RevStateApproval.valueOf(params.getRevState().getValue());

        // Start from the advanced filter (if any) and let top-level scalar fields override.
        ApAdvanceSearchFilter searchFilter = params.getSearchFilter();
        if (params.getSearch() != null && !params.getSearch().isBlank()) {
            if (searchFilter == null) {
                searchFilter = new ApAdvanceSearchFilter();
            }
            searchFilter.setSearch(params.getSearch());
        }

        UsrUser user = userService.getLoggedUser();
        final Integer userId = user == null ? null : user.getUserId();
        final String fileName = "access-points_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"))
                + ".csv";

        final ApAdvanceSearchFilter finalFilter = searchFilter;
        final Set<Integer> finalApTypeIds = apTypeIds;
        final Set<Integer> finalScopeIds = scopeIds;
        int id = ioExportWorker.enqueue(requestId -> new IOExportAccessPointsCsv(
                userId, requestId, fileName, finalFilter, finalApTypeIds, finalScopeIds, state, revState));
        return ResponseEntity.ok(id);
    }

    /**
     * Restrict the AP-type set by an item-type or item-specification filter.
     *
     * Mirrors the logic of {@code ApController.findAccessPoint} so the export endpoint resolves
     * the registry-list filters identically: the input apTypeIds (already subtree-expanded) is
     * intersected with the AP types that may carry the given item type / spec.
     */
    private Set<Integer> applyItemTypeOrSpecLimit(StaticDataProvider sdp,
                                                  Set<Integer> apTypeIdTree,
                                                  Integer itemTypeId,
                                                  Integer itemSpecId) {
        if (itemSpecId != null) {
            RulItemSpec spec = sdp.getItemSpecById(itemSpecId);
            if (spec == null) {
                throw new cz.tacr.elza.exception.ObjectNotFoundException(
                        "Specification not found", ArrangementCode.ITEM_SPEC_NOT_FOUND)
                        .setId(itemSpecId);
            }
            List<Integer> extraApTypeLimit = itemAptypeRepository.findApTypeIdsByItemSpec(spec);
            if (extraApTypeLimit.isEmpty()) {
                logger.error("Specification has no associated classes, itemSpecId={}", itemSpecId);
                throw new SystemException("Configuration error, specification without associated classes",
                        BaseCode.SYSTEM_ERROR).set("itemSpecId", itemSpecId);
            }
            return intersectWithApTypeSubtree(apTypeIdTree, extraApTypeLimit);
        }
        if (itemTypeId != null) {
            ItemType itemType = sdp.getItemTypeById(itemTypeId);
            if (itemType == null) {
                throw new cz.tacr.elza.exception.ObjectNotFoundException(
                        "Item type not found", ArrangementCode.ITEM_TYPE_NOT_FOUND)
                        .setId(itemTypeId);
            }
            if (itemType.hasSpecifications()) {
                throw new BusinessException("Item type requires specification", BaseCode.PROPERTY_NOT_EXIST)
                        .set("itemTypeId", itemTypeId)
                        .set("itemTypeCode", itemType.getCode());
            }
            List<Integer> extraApTypeLimit = itemAptypeRepository.findApTypeIdsByItemType(itemType.getEntity());
            if (extraApTypeLimit.isEmpty()) {
                logger.error("Item type has no associated classes, itemTypeId={}", itemTypeId);
                throw new SystemException("Configuration error, item type without associated classes",
                        BaseCode.SYSTEM_ERROR).set("itemTypeId", itemTypeId);
            }
            return intersectWithApTypeSubtree(apTypeIdTree, extraApTypeLimit);
        }
        return apTypeIdTree;
    }

    private Set<Integer> intersectWithApTypeSubtree(Set<Integer> apTypeIdTree, List<Integer> extraApTypeLimit) {
        Set<Integer> extraSubTree = apTypeRepository.findSubtreeIds(extraApTypeLimit);
        if (CollectionUtils.isEmpty(apTypeIdTree)) {
            return extraSubTree;
        }
        Set<Integer> intersection = new HashSet<>(apTypeIdTree);
        intersection.retainAll(extraSubTree);
        return intersection;
    }

    // PUT /accesspoint/export/{queueItemId}
    @Override
    @Transactional
    public ResponseEntity<Void> accessPointExportForceOrNo(Integer queueItemId, Boolean force) {
    	apConnectService.exportForceOrNo(queueItemId, force);
    	return ResponseEntity.ok().build();
    }

    @Override
    @Transactional
    public ResponseEntity<EntityRef> accessPointCopyAccessPoint(String id, @Valid CopyAccessPointDetail copyAccessPointDetail) {
        ApAccessPoint accessPoint = accessPointService.getAccessPointByIdOrUuid(id);
        ApScope scope = accessPointService.getApScope(copyAccessPointDetail.getScope());
        ApAccessPoint copyAccessPoint;
        try {
            copyAccessPoint = accessPointService.copyAccessPoint(accessPoint, scope, copyAccessPointDetail.getReplace(),
                                                                 copyAccessPointDetail.getSkipItems());
        } catch (SyncImpossibleException e) {
            throw new BusinessException("Failed to copy exception", e, BaseCode.INVALID_STATE);
        }
        CachedAccessPoint cachedAccessPoint = apCacheService.findCachedAccessPoint(copyAccessPoint.getAccessPointId());
        EntityRef entityRef = apCacheService.createEntityRef(cachedAccessPoint);
        return ResponseEntity.ok(entityRef);
    }

    /**
     * Odstranění (zneplatnění) nebo nahrazení archivní entity
     * 
     * @param id                      id archivní entity
     * @param deleteAccessPointDetail body třída
     */
    @Override
    @Transactional
    public ResponseEntity<Void> accessPointDeleteAccessPoint(String id, @Valid DeleteAccessPointDetail deleteAccessPointDetail) {
        ApAccessPoint accessPoint = accessPointService.getAccessPointByIdOrUuid(id);
        accessPointService.lockWrite(accessPoint);
        ApState apState = accessPointService.getStateInternal(accessPoint);
        ApAccessPoint replacedBy = null;
        boolean copyAll = false;
        if (deleteAccessPointDetail != null && deleteAccessPointDetail.getReplacedBy() != null) {
            if (deleteAccessPointDetail.getReplacedBy() != null) {
                replacedBy = accessPointService.getAccessPointByIdOrUuid(deleteAccessPointDetail.getReplacedBy());
            }
            copyAll = deleteAccessPointDetail.getReplaceType() != null
                    && deleteAccessPointDetail.getReplaceType() == ReplaceType.COPY_ALL;
        }
        ApRevision revision = revisionService.findRevisionByState(apState);
        if (revision != null) {
            revisionService.deleteRevision(apState, revision);
        }

        try {
            accessPointService.deleteAccessPoint(apState, replacedBy, copyAll);
        } catch (SyncImpossibleException e) {
            throw new BusinessException("Failed to replace access point", e, BaseCode.INVALID_STATE)
                            .set("entityId", apState.getAccessPointId());
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Odstranění (zneplatnění) archivných entit
     * 
     * @param deleteAccessPointsDetail list of id or uuid to delete
     */
    @Override
    @Transactional
    public ResponseEntity<Void> accessPointDeleteAccessPoints(@Valid DeleteAccessPointsDetail deleteAccessPointsDetail) {
        List<ApAccessPoint> accessPoints = accessPointService.getAccessPointsByIdOrUuid(deleteAccessPointsDetail.getIds());
        List<ApState> apStates = accessPointService.getStatesInternal(accessPoints);
        List<ApRevision> revisions = revisionService.findAllRevisionByStateIn(apStates);
        // TODO: Reimplement as one query/delete
        for (ApRevision revision : revisions) {
            revisionService.deleteRevision(revision.getState(), revision);
        }
        accessPointService.invalidateAccessPoints(apStates);

        return ResponseEntity.ok().build();
    }

    /**
     * Obnovení archivní entity
     * 
     * @param id identifikátor archivní entity
     */
    @Override
    @Transactional
    public ResponseEntity<Void> accessPointRestoreAccessPoint(String id) {
        ApAccessPoint accessPoint = accessPointService.getAccessPointByIdOrUuid(id);
        ApState apState = accessPointService.getStateInternal(accessPoint);

        accessPointService.restoreAccessPoint(apState);

        return ResponseEntity.ok().build();
    }

    /**
     * Získání seznamu smazaných (neplatných) entit
     * 
     * @param page
     * @param pageSize
     * @return InvalidatedEntitiesm
     */
    @Override
    @Transactional
    public ResponseEntity<InvalidatedEntities> accessPointGetInvalidatedEntities(Integer page, Integer pageSize) {
    	return ResponseEntity.ok(accessPointService.findInvalidatedEntities(page, pageSize));
    }

    /**
     * Validace archivní entity
     * 
     * @param id identifikátor archivní entity
     * @param includeRevision
     * @return ApValidationIssues
     */
    @Override
    @Transactional
    public ResponseEntity<ApValidationIssues> accessPointValidateAccessPoint(Integer id, Boolean includeRevision) {
        ApAccessPoint accessPoint = accessPointService.getAccessPoint(id);
        ApState apState = accessPointService.getStateInternal(accessPoint);

        accessPointService.checkPermissionForRead(apState);
        ApValidationIssues validationIssues = accessPointService.validate(accessPoint, apState, true, includeRevision);

        return ResponseEntity.ok(validationIssues);
    }

    /**
     * Získání seznamu zpracovatelů entity.
     * 
     * @param id identifikátor archivní entity
     */
    @Override
	@Transactional
    public ResponseEntity<List<Participant>> accessPointGetLastParticipants(Integer id) {
        ApAccessPoint accessPoint = accessPointService.getAccessPoint(id);

    	return ResponseEntity.ok(taskService.getLastParticipants(accessPoint));
    }

    /**
     * Změna stavu přístupového bodu.
     * PUT /accesspoint/{id}/state
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param stateUpdate   nový stav přístupového bodu
     * @param apVersion     verze přístupového bodu
     * @param assignTo		id uživatele
     * @return nová verze = verze + 1
     */
    @Override
    @Transactional
    public ResponseEntity<Integer> accessPointChangeState(Integer accessPointId, ApStateUpdate stateUpdate, Integer apVersion, Integer assignTo) {
        Validate.notNull(stateUpdate.getStateApproval(), "AP State is null");

        ApAccessPoint accessPoint = accessPointService.lockAccessPoint(accessPointId, apVersion);
        ApState state = accessPointService.getApState(accessPoint);
        ApRevision revision = revisionService.findRevisionByState(state);
        StateApproval newState = StateApproval.valueOf(stateUpdate.getStateApproval().toString());

        // Nelze změnit stav archivní entity, která má revizi
        if (revision != null) {
            throw new BusinessException("Nelze změnit stav archivní entity, která má revizi", RegistryCode.CANT_CHANGE_STATE_ENTITY_WITH_REVISION);
        }

        accessPointService.updateApState(accessPoint, newState, stateUpdate.getComment(), stateUpdate.getTypeId(), stateUpdate.getScopeId(), assignTo);
        accessPoint = accessPointService.updateAndValidate(accessPoint);
        if (accessPointService.isArchDescRevalidationRequired(state.getStateApproval(), newState, false, false)) {
            ruleService.revalidateNodesWithApRef(accessPoint.getAccessPointId());
        }
        apCacheService.createApCachedAccessPoint(accessPoint.getAccessPointId());

        return ResponseEntity.ok(accessPoint.getVersion());
    }

    /**
     * Založení nové části přístupového bodu.
     *
     * @param id          identifikátor přístupového bodu (PK)
     * @param apPartForm  data pro vytvoření části
     * @param apVersion   verze přístupového bodu
     * @return CreatedPart
     */
    @Override
    @Transactional
    public ResponseEntity<CreatedPart> accessPointCreatePart(Integer id, ApPartFormVO apPartForm, Integer apVersion) {

    	// nepovolujeme prázdné řádky pro ApItemStringVO i ApItemStringVO
    	apPartForm.validateItems();

        ApAccessPoint accessPoint = accessPointService.lockAccessPoint(id, apVersion);
        ApState state = accessPointService.getStateInternal(accessPoint);
        ApRevState revState = revisionService.findRevStateByState(state);
        CreatedPart createdPart = new CreatedPart();

        if (revState != null) {
            // Permission check is part of revisionService
            ApRevPart revPart = revisionService.createPart(state, revState, apPartForm);
            createdPart.setPartId(revPart.getPartId());
        } else {
            accessPointService.checkPermissionForEdit(state);

            ApPart apPart = partService.createPart(accessPoint, apPartForm);
            accessPoint = accessPointService.updateAndValidate(accessPoint);
            apCacheService.createApCachedAccessPoint(id);

            createdPart.setPartId(apPart.getPartId());
        }

        createdPart.setApVersion(accessPoint.getVersion());
        return ResponseEntity.ok(createdPart);
    }

    /**
     * Smazání části přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @param partId        identifikátor mazané části
     * @param apVersion     verze přístupového bodu
     * @return nová verze = verze + 1
     */
    @Override
    @Transactional
    public ResponseEntity<Integer> accessPointDeletePart(Integer accessPointId, Integer partId, Integer apVersion) {

        ApAccessPoint accessPoint = accessPointService.lockAccessPoint(accessPointId, apVersion);
        ApState state = accessPointService.getStateInternal(accessPoint);

        ApRevision revision = revisionService.findRevisionByState(state);
        if (revision != null) {
            revisionService.deletePart(state, revision, partId);
        } else {
            accessPointService.checkPermissionForEdit(state);
            partService.deletePart(accessPoint, partId);
            accessPoint = accessPointService.updateAndValidate(accessPoint);
            apCacheService.createApCachedAccessPoint(accessPoint.getAccessPointId());
        }

        return ResponseEntity.ok(accessPoint.getVersion());
    }

    /**
     * Úprava části přístupového bodu.
     * 
     * @param id         identifikátor přístupového bodu (PK)
     * @param partId     identifikátor upravované části
     * @param apPartForm data pro úpravu části
     * @param apVersion  verze přístupového bodu
     * @return nová verze = verze + 1
     */
    @Override
    @Transactional
    public ResponseEntity<Integer> accessPointUpdatePart(Integer id, Integer partId, ApPartFormVO apPartForm, Integer apVersion) {

    	// nepovolujeme prázdné řádky pro ApItemStringVO i ApItemStringVO
    	apPartForm.validateItems();

        ApAccessPoint accessPoint = accessPointService.lockAccessPoint(id, apVersion);
        ApState state = accessPointService.getStateInternal(accessPoint);
        ApPart apPart = partService.getPart(partId);
        ApRevision revision = revisionService.findRevisionByState(state);
        if (revision != null) {
            revisionService.updatePart(state, revision, apPart, apPartForm);
        } else {
            if (accessPointService.updatePart(accessPoint, state, apPart, apPartForm)) {
                accessPoint = accessPointService.updateAndValidate(accessPoint);
            	apCacheService.createApCachedAccessPoint(accessPoint.getAccessPointId());
            }
        }

        return ResponseEntity.ok(accessPoint.getVersion());
    }

    /**
     * Nastavení preferovaného jména přístupového bodu.
     * Možné pouze pro části typu Označení.
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @param partId        identifikátor části, kterou nastavujeme jako preferovanou
     * @param apVersion     verze přístupového bodu
     * @return nová verze = verze + 1
     */
    @Override
    @Transactional
    public ResponseEntity<Integer> accessPointSetPreferName(Integer accessPointId, Integer partId, Integer apVersion) {

        ApAccessPoint accessPoint = accessPointService.lockAccessPoint(accessPointId, apVersion);

        ApState state = accessPointService.getStateInternal(accessPoint);
        ApRevState revState = revisionService.findRevStateByState(state);
        if (revState != null) {
            revisionService.setPreferName(state, revState, partId, null);
        } else {
            accessPointService.checkPermissionForEdit(state);
            ApPart apPart = partService.getPart(partId);
            accessPoint = accessPointService.setPreferName(accessPoint, apPart);
            accessPoint = accessPointService.updateAndValidate(accessPoint);
            apCacheService.createApCachedAccessPoint(accessPoint.getAccessPointId());
        }

        return ResponseEntity.ok(accessPoint.getVersion());
    }

    /**
     * Vytvoření nové revize přístupového bodu.
     * 
     * @param id
     */
    @Override
    @Transactional
    public ResponseEntity<Void> accessPointCreateRevision(Integer id) {
        ApState state = accessPointService.getStateInternal(id);

        // Nelze vytvořit revizi, pokud má archivní entita jiný stav než NEW, TO_AMEND nebo APPROVED
        if (!Arrays.asList(StateApproval.NEW, StateApproval.TO_AMEND, StateApproval.APPROVED).contains(state.getStateApproval())) {
            throw new BusinessException("Nelze vytvořit revizi, protože archivní entita má nevhodný stav",
                    RegistryCode.CANT_CREATE_REVISION)
                            .set("state", state.getStateApproval());
        }

        revisionService.createRevision(state);
        apCacheService.createApCachedAccessPoint(state.getAccessPointId());

        return ResponseEntity.ok().build();
    }

    /**
     * Odstranění revize přístupového bodu.
     * 
     * @param id       id state
     */
    @Override
    @Transactional
    public ResponseEntity<Void> accessPointDeleteRevision(Integer id) {
        ApState state = accessPointService.getStateInternal(id);

        revisionService.deleteRevision(state);
        apCacheService.createApCachedAccessPoint(state.getAccessPointId());

        return ResponseEntity.ok().build();
    }

    /**
     * Sloučení revize a přístupového bodu.
     * POST /accesspoint/{id}/revision/merge
     * 
     * @param accessPointId identifikátor přístupového bodu
     * @param stateUpdate   nový stav revize přístupového bodu + komentář
     * @param apVersion     verze přístupového bodu
     * @return nová verze = verze + 1
     */
    @Override
    @Transactional
    public ResponseEntity<Integer> accessPointMergeRevision(Integer accessPointId, ApStateUpdate stateUpdate, Integer apVersion) {
        ApAccessPoint accessPoint = accessPointService.lockAccessPoint(accessPointId, apVersion);
        ApState apState = accessPointService.getStateInternal(accessPoint);
        StateApproval state = StateApproval.valueOf(stateUpdate.getStateApproval().toString());

        revisionService.mergeRevision(apState, state, stateUpdate.getComment());
        apCacheService.createApCachedAccessPoint(apState.getAccessPointId());

        return ResponseEntity.ok(accessPoint.getVersion());
    }

    /**
     * Změna stavu revize přístupového bodu.
     * PUT /accesspoint/{id}/revision/state
     *
     * @param accessPointId  identifikátor přístupového bodu
     * @param revStateChange nový stav rvize přístupového bodu
     * @param apVersion      verze přístupového bodu
     * @param assignTo		 id uživatele
     * @return nová verze = verze + 1
     */
    @Override
    @Transactional
    public ResponseEntity<Integer> accessPointChangeStateRevision(Integer accessPointId, RevStateChange revStateChange, Integer apVersion, Integer assignTo) {

    	ApAccessPoint accessPoint = accessPointService.lockAccessPoint(accessPointId, apVersion);
        ApState state = accessPointService.getStateInternal(accessPoint);
        RevStateApproval revNextState = RevStateApproval.valueOf(revStateChange.getState().getValue());
        Integer nextTypeId = revStateChange.getTypeId();
        if (nextTypeId == null) {
            nextTypeId = state.getApTypeId();
        }

        revisionService.changeStateRevision(state, nextTypeId, revNextState, revStateChange.getComment(), assignTo);
        apCacheService.createApCachedAccessPoint(state.getAccessPointId());

        return ResponseEntity.ok(accessPoint.getVersion());
    }

    /**
     * Úprava části přístupového bodu z revize
     *
     * @param id         identifikátor přístupového bodu (PK)
     * @param partId     identifikátor upravované části
     * @param apPartForm data pro úpravu části
     * @return nová verze = verze + 1
     */
    @Override
    @Transactional
    public ResponseEntity<Integer> accessPointUpdateRevisionPart(Integer id, Integer revPartId, ApPartFormVO apPartForm, Integer apVersion) {

        ApState state = accessPointService.getStateInternal(id);
        ApAccessPoint accessPoint = accessPointService.lockAccessPoint(state.getAccessPointId(), apVersion);
        ApRevision revision = revisionService.findRevisionByState(state);
        ApRevPart revPart = revisionPartService.findById(revPartId);
        revisionService.updatePart(state, revision, revPart, apPartForm);

        return ResponseEntity.ok(accessPoint.getVersion());
    }

    /**
     * Smazání části revize přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @param partId        identifikátor mazané části revize
     * @param apVersion     verze přístupového bodu
     * @return nová verze = verze + 1
     */
    @Override
    @Transactional
    public ResponseEntity<Integer> accessPointDeleteRevisionPart(Integer accessPointId, Integer partId, Integer apVersion) {
        ApAccessPoint accessPoint = accessPointService.lockAccessPoint(accessPointId, apVersion);
        ApState state = accessPointService.getStateInternal(accessPoint);

        revisionService.deleteRevPart(state, partId);

        return ResponseEntity.ok(accessPoint.getVersion());
    }

    /**
     * Nastavení preferovaného jména revizi přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @param partId        identifikátor části, kterou nastavujeme jako preferovanou
     * @param apVersion     verze přístupového bodu
     * @return nová verze = verze + 1
     */
    @Override
    @Transactional
    public ResponseEntity<Integer> accessPointSetPreferNameRevision(Integer accessPointId, Integer partId, Integer apVersion) {
        ApAccessPoint accessPoint = accessPointService.lockAccessPoint(accessPointId, apVersion);
        ApState state = accessPointService.getStateInternal(accessPoint);

        revisionService.setPreferName(state, partId);

        return ResponseEntity.ok(accessPoint.getVersion());
    }

    /**
     * Vrátí seznam automaticky generovaných prvků popisu.
     * 
     * @param id
     */
    @Override
    @Transactional
    public ResponseEntity<ResultAutoItems> accessPointGetAutoitems(String id) {
        Integer accessPointId = Integer.parseInt(id);
        ApState state;
        try {
            state = accessPointService.getApState(accessPointId);
        } catch (ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        List<GroovyItem> items = groovyService.getAutoItems(state);

        return ResponseEntity.ok(convertGroovyItems(items));
    }

    /**
     * Vrátí seznam automaticky generovaných prvků popisu pro revizi.
     * 
     * @param id
     */
    @Override
    @Transactional
    public ResponseEntity<ResultAutoItems> accessPointGetRevAutoitems(String id) {
        Integer accessPointId = Integer.parseInt(id);
        ApState state;
        try {
            state = accessPointService.getApState(accessPointId);
        } catch (ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        ApRevision revision = revisionService.findRevisionByState(state);
        if (revision == null) {
            return ResponseEntity.notFound().build();
        }

        List<GroovyItem> items = groovyService.getAutoItemsForRev(state, revision);

        return ResponseEntity.ok(convertGroovyItems(items));
    }

    private ResultAutoItems convertGroovyItems(List<GroovyItem> items) {
        ResultAutoItems resultAutoItems = new ResultAutoItems();

        for (GroovyItem item : items) {
            AutoValue autoValue = new AutoValue();
            autoValue.setItemTypeId(item.getItemType().getItemTypeId());
            autoValue.setItemSpecId(item.getSpecId());
            autoValue.setValue(item.getValue());

            resultAutoItems.addItemsItem(autoValue);
        }

        return resultAutoItems;
    }
}
