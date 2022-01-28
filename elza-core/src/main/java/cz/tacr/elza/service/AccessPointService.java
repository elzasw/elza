package cz.tacr.elza.service;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.constraints.NotNull;

import cz.tacr.elza.controller.vo.ExtAsyncQueueState;
import cz.tacr.elza.controller.vo.ExtSyncsQueueItemVO;
import cz.tacr.elza.controller.vo.ExtSyncsQueueResultListVO;
import cz.tacr.elza.controller.vo.SyncsFilterVO;
import cz.tacr.elza.controller.vo.SysExternalSystemVO;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.repository.ExtSyncsQueueItemRepository;
import cz.tacr.elza.repository.specification.ApStateSpecification;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.CachedPart;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import cz.tacr.elza.controller.factory.SearchFilterFactory;
import cz.tacr.elza.controller.vo.ApExternalSystemVO;
import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.ApValidationErrorsVO;
import cz.tacr.elza.controller.vo.ArchiveEntityResultListVO;
import cz.tacr.elza.controller.vo.FileType;
import cz.tacr.elza.controller.vo.PartValidationErrorsVO;
import cz.tacr.elza.controller.vo.SearchFilterVO;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.controller.vo.usage.FundVO;
import cz.tacr.elza.controller.vo.usage.NodeVO;
import cz.tacr.elza.controller.vo.usage.OccurrenceType;
import cz.tacr.elza.controller.vo.usage.OccurrenceVO;
import cz.tacr.elza.controller.vo.usage.RecordUsageVO;
import cz.tacr.elza.core.data.SearchType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.dataexchange.input.parts.context.ItemWrapper;
import cz.tacr.elza.dataexchange.input.parts.context.PartWrapper;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApScopeRelation;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApState.StateApproval;
import cz.tacr.elza.domain.ApStateEnum;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemAptype;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.SysLanguage;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ExceptionUtils;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.groovy.GroovyResult;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApPartRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FundRegisterScopeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.ItemAptypeRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.ScopeRelationRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.SysLanguageRepository;
import cz.tacr.elza.security.AuthorizationRequest;
import cz.tacr.elza.service.AccessPointItemService.ReferencedEntities;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.merge.PartWithSubParts;

/**
 * Servisní třída pro registry.
 *
 * @since 21.12.2015
 */
@Service
public class AccessPointService {

    private static final Logger logger = LoggerFactory.getLogger(AccessPointService.class);
    private static final String OBJECT_ID_SEQUENCE_NAME = "ap_name|object_id";

    @Autowired
    private ApAccessPointRepository apAccessPointRepository;

    @Autowired
    private ApTypeRepository apTypeRepository;

    @Autowired
    private ApStateRepository apStateRepository;

    @Autowired
    private DataRecordRefRepository dataRecordRefRepository;

    @Autowired
    private IEventNotificationService eventNotificationService;

    @Autowired
    private SysLanguageRepository sysLanguageRepository;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private FundRegisterScopeRepository fundRegisterScopeRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ArrangementService arrangementService;
    
    @Autowired
    private ArrangementInternalService arrangementInternalService;

    @Autowired
    private UserService userService;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private ApBindingRepository bindingRepository;

    @Autowired
    private ApBindingStateRepository bindingStateRepository;

    @Autowired
    private ApBindingItemRepository bindingItemRepository;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private AccessPointDataService apDataService;

    @Autowired
    private ApItemRepository itemRepository;

    @Autowired
    private AccessPointItemService apItemService;

    @Autowired
    private SequenceService sequenceService;

    @Autowired
    private ApStateRepository stateRepository;

    @Autowired
    private ScopeRelationRepository scopeRelationRepository;

    @Autowired
    private PartService partService;

    @Autowired
    private StructObjInternalService structObjInternalService;

    @Autowired
    private GroovyService groovyService;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private ItemAptypeRepository itemAptypeRepository;

    @Autowired
    private SearchFilterFactory searchFilterFactory;

    @Autowired
    private ApIndexRepository indexRepository;

    @Autowired
    private ApPartRepository partRepository;

    @Autowired
    private RuleService ruleService;
    
    @Autowired
    private InstitutionRepository institutionRepository;
    
    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private ExtSyncsQueueItemRepository extSyncsQueueItemRepository;

    @Autowired
    private AccessPointCacheService accessPointCacheService;
    
    @Value("${elza.scope.deleteWithEntities:false}")
    private boolean deleteWithEntities;

    /**
     * Kody tříd rejstříků nastavené v konfiguraci elzy.
     */
    private List<String> scopeCodes;

    /**
     * Nalezne takové záznamy rejstříku, které mají daný typ a jejich textová pole (record, charateristics, comment),
     * nebo pole variantního záznamu obsahují hledaný řetězec. V případě, že hledaný řetězec je null, nevyhodnocuje se.
     *
     * @param searchRecord hledaný řetězec, může být null
     * @param apTypeIds typ záznamu
     * @param firstResult index prvního záznamu, začíná od 0
     * @param maxResults počet výsledků k vrácení
     * @param fund AP, ze které se použijí třídy rejstříků
     * @param scopeId id scope, pokud je vyplněno hledají se rejstříky pouze s tímto scope
     * @return vybrané záznamy dle popisu seřazené za record, nbeo prázdná množina
     */
    public List<ApState> findApAccessPointByTextAndType(@Nullable final String searchRecord,
                                                        @Nullable final Collection<Integer> apTypeIds,
                                                        final Integer firstResult,
                                                        final Integer maxResults,
                                                        @Nullable final ArrFund fund,
                                                        @Nullable final Integer scopeId,
                                                        @Nullable final Collection<StateApproval> approvalStates,
                                                        @Nullable SearchType searchTypeName,
                                                        @Nullable SearchType searchTypeUsername) {

        Set<Integer> scopeIdsForSearch = getScopeIdsForSearch(fund, scopeId, false);

        return apAccessPointRepository.findApAccessPointByTextAndType(searchRecord, apTypeIds, firstResult, maxResults, scopeIdsForSearch, approvalStates, searchTypeName, searchTypeUsername);
    }


    /**
     * Celkový počet záznamů v DB pro funkci {@link #findApAccessPointByTextAndType}
     *
     * @param searchRecord hledaný řetězec, může být null
     * @param apTypeIds typ záznamu
     * @param fund AP, ze které se použijí třídy rejstříků
     * @param scopeId scope, pokud je vyplněno hledají se rejstříky pouze s tímto scope
     * @return celkový počet záznamů, který je v db za dané parametry
     */
    public long findApAccessPointByTextAndTypeCount(@Nullable final String searchRecord,
                                                    @Nullable final Collection<Integer> apTypeIds,
                                                    @Nullable final ArrFund fund,
                                                    @Nullable final Integer scopeId,
                                                    @Nullable final Collection<StateApproval> approvalStates,
                                                    @Nullable SearchType searchTypeName,
                                                    @Nullable SearchType searchTypeUsername) {

        Set<Integer> scopeIdsForSearch = getScopeIdsForSearch(fund, scopeId, false);

        return apAccessPointRepository.findApAccessPointByTextAndTypeCount(searchRecord, apTypeIds, scopeIdsForSearch, approvalStates, searchTypeName, searchTypeUsername);
    }

    /**
     * Získání objektu pomocí id nebo uuid
     * 
     * @param id řetězec znaků, id nebo uuid
     * @return ApAccessPoint
     */
    public ApAccessPoint getAccessPointByIdOrUuid(String id) {
        ApAccessPoint accessPoint;
        if (!StringUtils.isNumeric(id)) {
            accessPoint = apAccessPointRepository.findApAccessPointByUuid(id);
        } else {
            accessPoint = apAccessPointRepository.findById(Integer.valueOf(id)).orElse(null);
        }
        if (accessPoint == null) {
            logger.error("Přístupový bod neexistuje id={}", id);
            throw new ObjectNotFoundException("Přístupový bod neexistuje", BaseCode.ID_NOT_EXIST).set("id", id);
        }
        return accessPoint;
    }

    /**
     * Získání seznam objektu pomocí id nebo uuid
     * 
     * @param ids seznam is nebo uuid
     * @return List<ApAccessPoint>
     */
    public List<ApAccessPoint> getAccessPointsByIdOrUuid(List<String> ids) {
        List<ApAccessPoint> accessPoints;
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        if (!StringUtils.isNumeric(ids.get(0))) {
            accessPoints = apAccessPointRepository.findApAccessPointsByUuids(ids);
        } else {
            List<Integer> integerIds = ids.stream().map(p -> Integer.valueOf(p)).collect(Collectors.toList());
            accessPoints = apAccessPointRepository.findAllById(integerIds);
        }
        return accessPoints;
    }

    /**
     * Kontrola, jestli je používán přístupový bod v navázaných tabulkách.
     *
     * @param accessPoint přístupový bod
     * @throws BusinessException napojení na jinou tabulku
     */
    public void checkDeletion(final ApAccessPoint accessPoint) {
        // arr_data_record_ref
        if (institutionRepository.existsByAccessPointId(accessPoint.getAccessPointId())) {
            throw new BusinessException("Nelze smazat/zneplatnit přístupový bod, který je institucí.",
                                        RegistryCode.EXIST_INSTITUCI);
        }
        List<ArrDescItem> arrRecordItems = descItemRepository.findArrItemByRecord(accessPoint);
        if (CollectionUtils.isNotEmpty(arrRecordItems)) {
            throw new BusinessException(
                    "Nelze smazat/zneplatnit přístupový bod, který má hodnotu v jednotce archivního popisu.",
                    RegistryCode.EXIST_FOREIGN_DATA)
                            .set("accessPointId", accessPoint.getAccessPointId())
                            .set("arrItems", arrRecordItems.stream().map(ArrItem::getItemId).collect(Collectors
                                    .toList()))
                            .set("fundIds", arrRecordItems.stream().map(ArrItem::getFundId).collect(Collectors
                                    .toList()));
        }
    }

    /**
     * Smaže rej. heslo a jeho variantní hesla. Předpokládá, že již proběhlo ověření, že je možné ho smazat (vazby atd...).
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void deleteAccessPoint(@AuthParam(type = AuthParam.Type.AP_STATE) final ApState apState,
                                  final ApAccessPoint replacedBy, boolean mergeAp) {

        ApChange.Type changeType = replacedBy != null ? ApChange.Type.AP_REPLACE : ApChange.Type.AP_DELETE; 
        ApChange change = apDataService.createChange(changeType);

        apDataService.validationNotDeleted(apState);

        ApAccessPoint accessPoint = apState.getAccessPoint();

        if (replacedBy != null) {
            ApState replacementState = stateRepository.findByAccessPointId(replacedBy.getAccessPointId());
            apDataService.validationNotDeleted(replacementState);
            
            // check binding states
            // both APs cannot be binded to the same external system
            // in such case we have to solve deduplication in external system
            List<ApBindingState> srcBindings = bindingStateRepository.findByAccessPoint(accessPoint);
            List<ApBindingState> replacedByBindings = bindingStateRepository.findByAccessPoint(replacedBy);
            for(ApBindingState srcBinding: srcBindings) {
            	for(ApBindingState replacedByBinding: replacedByBindings) {
            		if(srcBinding.getExternalSystemId().equals(replacedByBinding.getExternalSystemId())) {
                        throw new BusinessException("Cannot replace entity with other entity from same external system", RegistryCode.EXT_SYSTEM_CONNECTED)
                        	.set("accessPointId", apState.getAccessPointId())
                        	.set("uuid", apState.getAccessPoint().getUuid())
                        	.set("replacedById", replacedBy.getAccessPointId())
                        	.set("externalSystemId", srcBinding.getExternalSystemId());
            		}
            	}
            }
            
            // při sloučení náhradní entita nemůže být ve stavu TO_APPROVE, APPROVED, REV_PREPARED
            if (mergeAp) {
                validationMergePossibility(replacementState);
            }
            replace(apState, replacementState);
            apState.setReplacedBy(replacedBy);

            // kopírování všechny Part z accessPoint->replacedBy
            if (mergeAp) {
                mergeParts(accessPoint, replacedBy, change);
                // vygenerování indexů a aktualizace záznamů v cache
                generateSync(replacedBy.getAccessPointId());        
                accessPointCacheService.createApCachedAccessPoint(replacedBy.getAccessPointId());
            }
        }
        deleteAccessPointPublichAndReindex(apState, accessPoint, change);
    }

    /**
     * Validace možnosti sloučení podle stavu
     * 
     * @param state stav přístupového bodu
     */
    private void validationMergePossibility(final ApState state) {
        if (state.getStateApproval() == StateApproval.TO_APPROVE
                || state.getStateApproval() == StateApproval.APPROVED
                || state.getStateApproval() == StateApproval.REV_PREPARED) {
            throw new BusinessException("Cílová entita je schválená nebo čeká na schválení a nelze ji měnit", RegistryCode.CANT_MERGE)
                .set("accessPointId", state.getAccessPointId())
                .set("stateApproval", state.getStateApproval());
        }
    }

    /**
     * Mazání seznamů objektů ApAccessPoint
     * 
     * @param apStates seznam
     */
    public void deleteAccessPoints(final List<ApState> apStates) {
        ApChange change = apDataService.createChange(ApChange.Type.AP_DELETE);
        for (ApState apState : apStates) {
            apDataService.validationNotDeleted(apState);
            ApAccessPoint accessPoint = apState.getAccessPoint();
            deleteAccessPointPublichAndReindex(apState, accessPoint, change);
        }
    }

    /**
     * Mazání ApAccessPoint
     *
     * @param apState
     * @param accessPoint
     * @param change
     */
    public void deleteAccessPoint(final ApState apState, ApAccessPoint accessPoint, final ApChange change) {
        checkDeletion(accessPoint);
        partService.deleteParts(accessPoint, change);
        apState.setDeleteChange(change);
        apStateRepository.save(apState);

        List<ApBindingState> eids = bindingStateRepository.findByAccessPoint(accessPoint);
        if (CollectionUtils.isNotEmpty(eids)) {
            eids.forEach(eid -> eid.setDeleteChange(change));
            bindingStateRepository.saveAll(eids);
            bindingItemRepository.invalidateByAccessPoint(accessPoint, change);
        }

        accessPointCacheService.deleteCachedAccessPoint(accessPoint);
    }

    /**
     * Mazání ApAccessPoint, zveřejnění a reindexování
     *
     * @param apState
     * @param accessPoint
     * @param change
     */
    private void deleteAccessPointPublichAndReindex(final ApState apState, ApAccessPoint accessPoint, final ApChange change) {
        deleteAccessPoint(apState, accessPoint, change);

        accessPoint = saveWithLock(accessPoint);
        publishAccessPointDeleteEvent(accessPoint);
        reindexDescItem(accessPoint);
    }

    /**
     * Uložení třídy rejstříku.
     *
     * @param scope třída k uložení
     * @return uložená třída
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.FUND_ADMIN})
    public ApScope saveScope(final ApScope scope) {
        Assert.notNull(scope, "Scope musí být vyplněn");
        checkScopeSave(scope);
        return scopeRepository.save(scope);
    }

    /**
     * Smazání třídy rejstříku.
     *
     * @param scope třída rejstříku
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.FUND_ADMIN})
    public void deleteScope(final ApScope scope) {
        Assert.notNull(scope, "Scope musí být vyplněn");
        Assert.notNull(scope.getScopeId(), "Identifikátor scope musí být vyplněn");

        List<ApState> apStates = apStateRepository.findByScope(scope);
        if (!deleteWithEntities) {
        	ExceptionUtils.isEmptyElseBusiness(apStates, "Nelze smazat třídu rejstříku, která je nastavena na rejstříku.", RegistryCode.USING_SCOPE_CANT_DELETE);
        } else {
            apStateRepository.deleteAllByScope(scope);
        }
        final List<ApScope> apScopes = scopeRepository.findConnectedByScope(scope);
        ExceptionUtils.isEmptyElseBusiness(apScopes, "Nelze smazat oblast obsahující návazné oblasti.", RegistryCode.CANT_DELETE_SCOPE_WITH_CONNECTED);
        final List<ApScopeRelation> apScopeRelations = scopeRelationRepository.findByConnectedScope(scope);
        ExceptionUtils.isEmptyElseBusiness(apScopeRelations, "Nelze smazat oblast která je návaznou oblastí jiné oblasti.", RegistryCode.CANT_DELETE_CONNECTED_SCOPE);

        fundRegisterScopeRepository.deleteAll(fundRegisterScopeRepository.findByScope(scope));
        scopeRepository.delete(scope);
    }

    /**
     * Provázání tříd rejstříku.
     *
     * @param scope třída rejstříku
     * @param connectedScope třída rejstříku k navázání
     */
    @Transactional
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.FUND_ADMIN})
    public void connectScope(@NotNull final ApScope scope, @NotNull final ApScope connectedScope) {
        Assert.notNull(scope, "Nebyla předána třída rejstříku");
        Assert.notNull(connectedScope, "Nebyla předána třída rejstříku k navázání");
        Assert.notNull(scope.getScopeId(), "Třída rejstříku nemá vyplněné ID");
        Assert.notNull(connectedScope.getScopeId(), "Navazovaná třída rejstříku nemá vyplněné ID");

        final List<ApScope> connectedByScope = scopeRepository.findConnectedByScope(scope);
        for (ApScope apScope : connectedByScope) {
            if (apScope.equals(connectedScope)) {
                throw new BusinessException("Třída ID=" + scope.getScopeId() + " je již navázána na třídu ID=" + connectedScope.getScopeId(), RegistryCode.SCOPES_ALREADY_CONNECTED);
            }
        }
        final ApScopeRelation apScopeRelation = new ApScopeRelation();
        apScopeRelation.setScope(scope);
        apScopeRelation.setConnectedScope(connectedScope);
        scopeRelationRepository.save(apScopeRelation);
    }

    /**
     * Zrušení provázání tříd rejstříku.
     *
     * @param scope třída rejstříku
     * @param connectedScope třída rejstříku k odpojení
     */
    @Transactional
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.FUND_ADMIN})
    public void disconnectScope(@NotNull final ApScope scope, @NotNull final ApScope connectedScope) {
        Assert.notNull(scope, "Nebyla předána třída rejstříku");
        Assert.notNull(connectedScope, "Nebyla předána třída rejstříku k navázání");
        Assert.notNull(scope.getScopeId(), "Třída rejstříku nemá vyplněné ID");
        Assert.notNull(connectedScope.getScopeId(), "Navazovaná třída rejstříku nemá vyplněné ID");

        final ApScopeRelation scopeRelation = scopeRelationRepository.findByScopeAndConnectedScope(scope, connectedScope);
        if (scopeRelation == null) {
            throw new BusinessException("Třída rejstříku ID=" + scope.getScopeId() + " není navázána na třídu ID=" + connectedScope.getScopeId(), RegistryCode.SCOPES_NOT_CONNECTED);
        }
        scopeRelationRepository.delete(scopeRelation);
    }

    /**
     * Kontrola uložení třídy rejstříku.
     *
     * @param scope ukládaná třída
     */
    private void checkScopeSave(final ApScope scope) {
        Assert.notNull(scope, "Scope musí být vyplněn");
        Assert.notNull(scope.getCode(), "Třída musí mít vyplněný kod");
        Assert.notNull(scope.getName(), "Třída musí mít vyplněný název");

        List<ApScope> scopes = scopeRepository.findByCodes(Collections.singletonList(scope.getCode()));
        if (scope.getScopeId() == null) {
            ExceptionUtils.isEmptyElseBusiness(scopes, "Třída rejstříku s daným kódem již existuje.", RegistryCode.SCOPE_EXISTS);
        } else {
            ApScope codeScope = scopes.isEmpty() ? null : scopes.get(0);
            if (codeScope != null && !codeScope.getScopeId().equals(scope.getScopeId())) {
                throw new BusinessException("Třída rejstříku s daným kódem již existuje.", RegistryCode.SCOPE_EXISTS);
            }
        }
    }

    /**
     * Načte seznam id tříd ve kterých se má vyhledávat. Výsledek je průnikem tříd
     * požadovaných a těch na které ma uživatel právo.
     *
     * @param fund
     *            AP, podle jejíž tříd se má hledat
     * @param scopeId
     *            id scope, pokud je vyplněno hledá se jen v tomto scope
     * @param includeConnetedScopes
     *            Flag to included connected scopes to scopeId. If scopeId is not
     *            defined flag is not used.
     * @return množina id tříd, podle kterých se bude hledat
     */
    public Set<Integer> getScopeIdsForSearch(@Nullable final ArrFund fund,
                                             @Nullable final Integer scopeId,
                                             boolean includeConnetedScopes) {
        boolean readAllScopes = userService.hasPermission(UsrPermission.Permission.AP_SCOPE_RD_ALL);
        UsrUser user = userService.getLoggedUser();

        Set<Integer> scopeIdsToSearch;
        if (readAllScopes || user == null) {
            scopeIdsToSearch = scopeRepository.findAllIds();
        } else {
            scopeIdsToSearch = userService.getUserScopeIds();
        }

        if (scopeIdsToSearch.isEmpty()) {
            return Collections.emptySet();
        }

        if (fund != null) {
            // connected scopes cannot be used to search in a fund 
            Validate.isTrue(!includeConnetedScopes);
            Set<Integer> fundScopeIds = scopeRepository.findIdsByFundId(fund.getFundId());
            scopeIdsToSearch.retainAll(fundScopeIds);
        }

        if (scopeId != null) {
            Set<Integer> scopeIdSet = Collections.singleton(scopeId);
            if (!includeConnetedScopes) {
                if (scopeIdsToSearch.contains(scopeId)) {
                    return scopeIdSet;
                } else {
                    return Collections.emptySet();
                }
            }

            // get connected scopes
            Set<Integer> connectedScopes = scopeRelationRepository.findConnectedScopeIdsByScopeIds(scopeIdSet);
            connectedScopes.add(scopeId);

            scopeIdsToSearch.retainAll(connectedScopes);
        }

        return scopeIdsToSearch;
    }

    /**
     * Uložení uzlu - optimistické zámky
     *
     * @param node uzel
     * @return uložený uzel
     */
    private ArrNode saveNode(final ArrNode node, final ArrChange change) {
        node.setLastUpdate(change.getChangeDate().toLocalDateTime());
        nodeRepository.save(node);
        nodeRepository.flush();
        return node;
    }

    /**
     * Najde použití rejstříku/osoby.
     *
     * @param record rejstřík
     * @return použití rejstříku/osoby
     */
    public RecordUsageVO findRecordUsage(final ApAccessPoint record) {
        List<FundVO> usedFunds = findUsedFunds(record); // výskyt v archivních souborech

        return new RecordUsageVO(usedFunds);
    }

    /**
     * Najde použité archivní soubory.
     *
     * @param record rejstřík
     * @return použití rejstříku/osoby
     */
    private List<FundVO> findUsedFunds(final ApAccessPoint record) {
        List<ArrData> dataList = new LinkedList<>(dataRecordRefRepository.findByRecord(record));
        return createFundVOList(dataList);
    }

    /**
     * Z předaných výskytů v archivních souborech vytvoří seznam {@link FundVO}.
     *
     * @param arrDataList hodnoty uzlů
     */
    private List<FundVO> createFundVOList(final List<? extends ArrData> arrDataList) {
        if (arrDataList.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, Set<Integer>> fundIdToNodeIdsMap = new HashMap<>();

        Map<Integer, ? extends ArrData> dataIdToArrDataMap = arrDataList.stream().collect(toMap(ArrData::getDataId, Function.identity()));

        Map<Integer, Set<Integer>> nodeIdToDataIdsMap = new HashMap<>();
        if (!arrDataList.isEmpty()) {
            List<Object[]> fundIdNodeIdDataIdList = descItemRepository.findFundIdNodeIdDataIdByDataAndDeleteChangeIsNull(arrDataList);

            fundIdNodeIdDataIdList.forEach(row -> {
                Integer fundId = (Integer) row[0];
                Integer nodeId = (Integer) row[1];
                Integer dataId = (Integer) row[2];

                Set<Integer> nodeIds;
                if (fundIdToNodeIdsMap.containsKey(fundId)) {
                    nodeIds = fundIdToNodeIdsMap.get(fundId);
                } else {
                    nodeIds = new HashSet<>();
                    fundIdToNodeIdsMap.put(fundId, nodeIds);
                }
                nodeIds.add(nodeId);

                Set<Integer> dataIds;
                if (nodeIdToDataIdsMap.containsKey(nodeId)) {
                    dataIds = nodeIdToDataIdsMap.get(nodeId);
                } else {
                    dataIds = new HashSet<>();
                    nodeIdToDataIdsMap.put(nodeId, dataIds);
                }
                dataIds.add(dataId);
            });
        }

        if (fundIdToNodeIdsMap.keySet().isEmpty()) {
            return Collections.emptyList();
        }

        List<ArrFundVersion> fundVersions = fundVersionRepository.findByFundIdsAndLockChangeIsNull(fundIdToNodeIdsMap.keySet());
        return fundVersions.stream()
                .map(v -> createFundVO(fundIdToNodeIdsMap, dataIdToArrDataMap, nodeIdToDataIdsMap, v))
                .collect(Collectors.toList());
    }

    /**
     * Vytvoří {@link FundVO}.
     *
     * @param fundIdToNodeIdsMap id archivního souboru na množinu použitých uzlů
     * @param dataIdToArrDataMap mapa id {@link ArrData} na konkrétní instanci
     * @param nodeIdToDataIdsMap mapa id node na množinu {@link ArrData}
     * @param fundVersion verze archivního souboru
     * @return {@link FundVO}
     */
    private FundVO createFundVO(final Map<Integer, Set<Integer>> fundIdToNodeIdsMap, final Map<Integer, ? extends ArrData> dataIdToArrDataMap,
                                final Map<Integer, Set<Integer>> nodeIdToDataIdsMap, final ArrFundVersion fundVersion) {
        Set<Integer> nodeIds = fundIdToNodeIdsMap.get(fundVersion.getFundId());
        List<Integer> nodeIdsSubList = getNodeIdsSublist(nodeIds);

        Collection<TreeNodeVO> treeNodes = levelTreeCacheService.getFaTreeNodes(fundVersion.getFundVersionId(), nodeIdsSubList);

        List<NodeVO> nodes = treeNodes.stream()
                .map(n -> createNodeVO(dataIdToArrDataMap, nodeIdToDataIdsMap, n))
                .collect(Collectors.toList());

        ArrFund fund = fundVersion.getFund();
        return new FundVO(fund.getFundId(), fund.getName(), nodeIds.size(), nodes);
    }

    /**
     * Vytvoří {@link NodeVO}.
     *
     * @param dataIdToArrDataMap mapa id {@link ArrData} na konkrétní instanci
     * @param nodeIdToDataIdsMap mapa id node na množinu {@link ArrData}
     * @param node node
     * @return {@link NodeVO}
     */
    private NodeVO createNodeVO(final Map<Integer, ? extends ArrData> dataIdToArrDataMap, final Map<Integer, Set<Integer>> nodeIdToDataIdsMap,
                                final TreeNodeVO node) {
        List<OccurrenceVO> occurrences = new LinkedList<>();

        if (nodeIdToDataIdsMap.containsKey(node.getId())) {
            occurrences.addAll(createOccurrenceVOFromData(dataIdToArrDataMap, nodeIdToDataIdsMap.get(node.getId())));
        }

        return new NodeVO(node.getId(), node.getName(), occurrences);
    }

    /**
     * Získá podmnožinu id uzlů pokud jich je celkem více než počet který chceme vracet.
     *
     * @param nodeIds množina identifikátorů uzlů
     * @return podmnožina identifikátorů uzlů
     */
    private List<Integer> getNodeIdsSublist(final Set<Integer> nodeIds) {
        List<Integer> sortedNodeIds = new LinkedList<>(nodeIds);
        Collections.sort(sortedNodeIds);
        return sortedNodeIds.subList(0, sortedNodeIds.size() > 200 ? 200 : sortedNodeIds.size());
    }

    /**
     * Vytvoří {@link OccurrenceVO} z předaných instancí {@link ArrData}.
     *
     * @param dataIdToArrDataMap mapa id {@link ArrData} na konkrétní instanci
     * @param dataIds množina identifikátorů {@link ArrData}
     * @return seznam výskytů {@link OccurrenceVO}
     */
    private List<OccurrenceVO> createOccurrenceVOFromData(final Map<Integer, ? extends ArrData> dataIdToArrDataMap,
                                                          final Set<Integer> dataIds) {
        return dataIds.stream()
                .map(dataId -> {
                    ArrData data = dataIdToArrDataMap.get(dataId);

                    OccurrenceType type;
                    if (data.getDataType().getCode().equals("PARTY_REF")) {
                        type = OccurrenceType.ARR_DATA_PARTY_REF;
                    } else {
                        type = OccurrenceType.ARR_DATA_RECORD_REF;
                    }

                    return new OccurrenceVO(dataId, type);
                }).collect(toList());
    }

    /**
     * Replace record replaced by record replacement in all usages in JP, NodeRegisters
     */
    public void replace(final ApState replacedState, final ApState replacementState) {

        final ApAccessPoint replaced = replacedState.getAccessPoint();
        final ApAccessPoint replacement = replacementState.getAccessPoint();

        final List<ArrDescItem> arrItems = descItemRepository.findArrItemByRecord(replaced);

        // ArrItems
        final Collection<Integer> fundsAll = arrItems.stream().map(ArrDescItem::getFundId).collect(Collectors.toSet());

        // fund to scopes
        Map<Integer, Set<Integer>> fundIdsToScopes = fundsAll.stream().collect(toMap(Function.identity(), scopeRepository::findIdsByFundId));

        // Oprávnění
        boolean isFundAdmin = userService.hasPermission(UsrPermission.Permission.FUND_ARR_ALL);
        if (!isFundAdmin) {
            fundsAll.forEach(i -> {
                if (!userService.hasPermission(UsrPermission.Permission.FUND_ARR, i)) {
                    throw new SystemException("Uživatel nemá oprávnění na AS.", BaseCode.INSUFFICIENT_PERMISSIONS).set("fundId", i);
                }
            });
        }


        final Map<Integer, ArrFundVersion> fundVersions;
        if (fundsAll.isEmpty()) {
            fundVersions = Collections.emptyMap();
        } else {
            fundVersions = arrangementInternalService.getOpenVersionsByFundIds(fundsAll).stream()
                    .collect(toMap(ArrFundVersion::getFundId, Function.identity()));
        }

        final ArrChange change = arrangementInternalService.createChange(ArrChange.Type.REPLACE_REGISTER);
        arrItems.forEach(i -> {
            final ArrDataRecordRef data = new ArrDataRecordRef();
            data.setRecord(replacement);
            ArrDescItem im = new ArrDescItem();
            im.setData(data);
            im.setNode(i.getNode());
            im.setCreateChange(i.getCreateChange());
            im.setDeleteChange(i.getDeleteChange());
            im.setDescItemObjectId(i.getDescItemObjectId());
            im.setItemId(i.getDescItemObjectId());
            im.setItemSpec(i.getItemSpec());
            im.setItemType(i.getItemType());
            im.setPosition(i.getPosition());

            Integer fundId = i.getFundId();
            Set<Integer> fundScopes = fundIdsToScopes.get(fundId);
            if (fundScopes == null) {
                throw new SystemException("Pro AS neexistují žádné scope.", BaseCode.INVALID_STATE)
                        .set("fundId", fundId);
            } else {
                if (!fundScopes.contains(replacementState.getScopeId())) {
                    throw new BusinessException("Nelze nahradit rejsříkové heslo v AS jelikož AS nemá scope rejstříku pomocí kterého nahrazujeme.", BaseCode.INVALID_STATE)
                            .set("fundId", fundId)
                            .set("scopeId", replacementState.getScopeId());
                }
            }
            descriptionItemService.updateDescriptionItem(im, fundVersions.get(fundId), change);
        });
    }

    public boolean canBeDeleted(ApAccessPoint record) {
        return dataRecordRefRepository.findByRecord(record).isEmpty();
    }

    /**
     * Založení nového přístupového bodu.
     *
     * @param scope třída přístupového bodu
     * @param type typ přístupového bodu
     * @param language jazyk jména
     * @param apPartFormVO preferovaná část přístupového bodu
     * @return založený přístupový bod
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    @Transactional(TxType.MANDATORY)
    public ApState createAccessPoint(@AuthParam(type = AuthParam.Type.SCOPE) final ApScope scope,
                                     final ApType type,
                                     @Nullable final SysLanguage language,
                                     final ApPartFormVO apPartFormVO) {
        Assert.notNull(scope, "Třída musí být vyplněna");
        Assert.notNull(type, "Typ musí být vyplněn");
        if (CollectionUtils.isEmpty(apPartFormVO.getItems())) {
            throw new IllegalArgumentException("Část musí mít alespoň jeden prvek popisu.");
        }
        if (apPartFormVO.getParentPartId() != null) {
            throw new IllegalArgumentException("Část nesmí být podřízená.");
        }
        RulPartType partType = structObjInternalService.getPartTypeByCode(apPartFormVO.getPartTypeCode());
        StaticDataProvider sdp = staticDataService.getData();
        RulPartType defaultPartType = sdp.getDefaultPartType();
        if (!partType.getCode().equals(defaultPartType.getCode())) {
            throw new IllegalArgumentException("Část musí být typu " + defaultPartType.getCode());
        }

        ApChange apChange = apDataService.createChange(ApChange.Type.AP_CREATE);
        ApState apState = createAccessPoint(scope, type, StateApproval.NEW, apChange, null);
        ApAccessPoint accessPoint = apState.getAccessPoint();

        ApPart apPart = partService.createPart(partType, accessPoint, apChange, null);
        accessPoint.setPreferredPart(apPart);

        apItemService.createItems(apPart, apPartFormVO.getItems(), apChange, null, null);
        generateSync(accessPoint, apPart);
        accessPointCacheService.createApCachedAccessPoint(accessPoint.getAccessPointId());

        publishAccessPointCreateEvent(accessPoint);

        return apState;
    }

    public void setAccessPointInDataRecordRefs(ApAccessPoint accessPoint, List<ArrDataRecordRef> dataRecordRefList, ApBinding binding) {
        if (CollectionUtils.isNotEmpty(dataRecordRefList)) {
            for (ArrDataRecordRef dataRecordRef : dataRecordRefList) {
                if (dataRecordRef.getBinding().getBindingId().equals(binding.getBindingId())) {
                    dataRecordRef.setRecord(accessPoint);
                }
            }
        }
    }

    /**
     * Aktualizace stavajicich propojeni
     *
     * @param dataRefList
     * @param bindingItemList
     */
    // 11.2.2021 PPy - zakomentovano, nejasna funkce
    /*
    private void createBindingForRel(final List<ReferencedEntities> dataRefList, final List<ApBindingItem> bindingItemList) {
        //TODO fantiš optimalizovat
        for (ReferencedEntities dataRef : dataRefList) {
            ApBindingItem apBindingItem = findBindingItemByUuid(bindingItemList, dataRef.getUuid());
            if (apBindingItem != null && apBindingItem.getItem() != null) {
                ArrDataRecordRef dataRecordRef = dataRef.;
                ApBinding currentEntity = apBindingItem.getBinding();
                ApScope scope = currentEntity.getScope();
                ApExternalSystem apExternalSystem = currentEntity.getApExternalSystem();
                ApBinding refBinding = externalSystemService.findByScopeAndValueAndApExternalSystem(scope,
                                                                                                    dataRef.getValue(),
                                                                                                    apExternalSystem);
                if (refBinding == null) {
                    dataRecordRef.setBinding(externalSystemService.createApBinding(scope, dataRef.getValue(), apExternalSystem));
                } else {
                    dataRecordRef.setBinding(refBinding);
    
                    ApBindingState bindingState = externalSystemService.findByBinding(refBinding);
                    if (bindingState != null) {
                        dataRecordRef.setRecord(bindingState.getAccessPoint());
                    }
                }
                dataRecordRefRepository.save(dataRecordRef);
            }
        }
    }*/

    public ApPart findParentPart(final ApBinding binding, final String parentUuid) {
        ApBindingItem apBindingItem = externalSystemService.findByBindingAndUuid(binding, parentUuid);
        return apBindingItem.getPart();
    }

    @Transactional(TxType.MANDATORY)
    public boolean updatePart(final ApAccessPoint apAccessPoint,
                           final ApPart apPart,
                           final ApPartFormVO apPartFormVO) {
        List<ApItem> deleteItems = itemRepository.findValidItemsByPart(apPart);
        List<ApBindingItem> bindingItemList = bindingItemRepository.findByItems(deleteItems);

        Map<Integer, ApItem> itemMap = deleteItems.stream().collect(Collectors.toMap(ApItem::getItemId, i -> i));

        List<ApItemVO> itemListVO = apPartFormVO.getItems();
        List<ApItemVO> createItems = new ArrayList<>();

        // určujeme, které záznamy: přidat, odstranit, nebo ponechat
        for (ApItemVO itemVO : itemListVO) {
            if (itemVO.getId() == null) {
                createItems.add(itemVO); // new -> add
            } else {
                ApItem item = itemMap.get(itemVO.getId());
                if (item != null) {
                    if (itemVO.equalsValue(item)) {
                        deleteItems.remove(item); // no change -> don't delete
                    } else {
                        createItems.add(itemVO); // changed -> add + delete
                    }
                }
            }
        }

        // pokud nedojde ke změně
        if (createItems.isEmpty() && deleteItems.isEmpty()) {
            return false;
        }

        ApChange change = apDataService.createChange(ApChange.Type.AP_UPDATE);

        apPart.setLastChange(change);

        List<ReferencedEntities> dataRefList = new ArrayList<>();
        apItemService.createItems(apPart, createItems, change, bindingItemList, dataRefList);
        bindingItemRepository.flush();

        apItemService.deleteItems(deleteItems, change);

        generateSync(apAccessPoint, apPart);

        return true;
    }

    public void changeIndicesToNewPart(ApPart apPart, ApPart newPart) {
        List<ApIndex> indices = indexRepository.findByPartId(apPart.getPartId());
        if (CollectionUtils.isNotEmpty(indices)) {
            for (ApIndex index : indices) {
                index.setPart(newPart);
            }
            indexRepository.saveAll(indices);
        }
    }

    public void changeBindingItemParts(ApPart oldPart, ApPart newPart, ApChange change) {
        List<ApBindingItem> bindingItemList = bindingItemRepository.findByPart(oldPart);
        if (CollectionUtils.isNotEmpty(bindingItemList)) {
            // We have two possibilities
            // - binding item can be updated
            // - binding item can be deleted and new one created
            // We are using first strategy - needs to consider what will be beter in future

            for (ApBindingItem bindingItem : bindingItemList) {
                bindingItem.setPart(newPart);
            }

            /*List<ApBindingItem> newBindingItems = new ArrayList<>(bindingItemList.size());
            
            for (ApBindingItem bindingItem : bindingItemList) {
                ApBindingItem newBindingItem = new ApBindingItem();
                newBindingItem.setBinding(bindingItem.getBinding());
                newBindingItem.setCreateChange(change);
                newBindingItems.add(newBindingItem);
                newBindingItem.setPart(newPart);
                newBindingItem.setValue(bindingItem.getValue());
            
                bindingItem.setDeleteChange(change);
            }*/
            bindingItemRepository.saveAll(bindingItemList);
        }
    }

    public ApPart findPreferredPart(final List<ApPart> partList) {
        StaticDataProvider sdp = StaticDataProvider.getInstance();
        RulPartType defaultPartType = sdp.getDefaultPartType();
        for (ApPart part : partList) {
            if (part.getPartType().getCode().equals(defaultPartType.getCode())) {
                return part;
            }
        }
        return null;
    }

    /**
     * Run Groovy scripts on parts
     * 
     * @param state
     * @param partList
     * @param itemMap
     * @param async
     * @return true nebo false
     */
    public boolean updatePartValues(final ApState state,
                                    final Integer prefPartId,
                                    final List<ApPart> partList,
                                    final Map<Integer, List<ApItem>> itemMap,
                                    final boolean async) {
        boolean success = true;
        for (ApPart part : partList) {
            List<ApPart> childrenParts = findChildrenParts(part, partList);
            List<ApItem> items = getItemsForParts(part, childrenParts, itemMap);

            boolean preferred = prefPartId == null || Objects.equals(prefPartId, part.getPartId());
            GroovyResult result = groovyService.processGroovy(state, part, childrenParts, items, preferred);
            if (!partService.updatePartValue(part, result, state, state.getScope(), async, preferred)) {
                success = false;
            }
        }
        return success;
    }

    private List<ApPart> findChildrenParts(final ApPart part, final List<ApPart> partList) {
        List<ApPart> childrenParts = new ArrayList<>();
        for (ApPart p : partList) {
            if (p.getParentPart() != null && p.getParentPart().getPartId().equals(part.getPartId())) {
                childrenParts.add(p);
            }
        }
        return childrenParts;
    }

    private List<ApItem> getItemsForParts(final ApPart part,
                                          final List<ApPart> childrenParts,
                                          final Map<Integer, List<ApItem>> itemMap) {

        List<ApItem> itemsSrc = itemMap.get(part.getPartId());

        List<ApItem> result;
        if (CollectionUtils.isEmpty(itemsSrc)) {
            result = new ArrayList<>();
        } else {
            result = new ArrayList<>(itemsSrc);
        }

        // get items from children parts
        if (CollectionUtils.isNotEmpty(childrenParts)) {
            for (ApPart p : childrenParts) {
                List<ApItem> childItemList = itemMap.get(p.getPartId());
                if (CollectionUtils.isNotEmpty(childItemList)) {
                    result.addAll(childItemList);
                }
            }
        }

        return result;
    }

    // TODO: move method to DataExchange package and rework to use other methods in this service
    public boolean updatePartValues(final Collection<PartWrapper> partWrappers) {
        StaticDataProvider sdp = staticDataService.getData();

        boolean success = true;
        Set<Integer> accessPointIds = new HashSet<>();

        for (PartWrapper partWrapper : partWrappers) {
            // skip ignored parts
            if (partWrapper.getSaveMethod() == SaveMethod.IGNORE) {
                continue;
            }

            ApPart apPart = partWrapper.getEntity();
            ApState state = partWrapper.getPartInfo().getApInfo().getApState();

            List<PartWrapper> childrenPartWrappers = findChildrenParts(apPart, partWrappers);
            List<ApPart> childrenParts = getChildrenParts(childrenPartWrappers);

            List<ApItem> items = getItemsFromPartWrappers(partWrapper, childrenPartWrappers);

            boolean preferred = false;
            Integer accessPointId = state.getAccessPoint().getAccessPointId();

            if (partWrapper.getPartInfo().getRulPartType().getCode().equals(sdp.getDefaultPartType().getCode()) &&
                !accessPointIds.contains(accessPointId)) {
                accessPointIds.add(accessPointId);
                preferred = true;
            }

            GroovyResult result = groovyService.processGroovy(state, apPart, childrenParts, items, preferred);

            if (!partService.updatePartValue(apPart, result, state, state.getScope(), false, preferred)) {
                success = false;
            }
        }
        return success;
    }

    private List<PartWrapper> findChildrenParts(ApPart apPart, Collection<PartWrapper> partWrappers) {
        List<PartWrapper> childrenPartWrappers = new ArrayList<>();
        for (PartWrapper partWrapper : partWrappers) {
            ApPart entity = partWrapper.getEntity();
            if (entity.getParentPart() != null && entity.getParentPart().getPartId().equals(apPart.getPartId())) {
                childrenPartWrappers.add(partWrapper);
            }
        }
        return childrenPartWrappers;
    }

    private List<ApPart> getChildrenParts(List<PartWrapper> childrenPartWrappers) {
        List<ApPart> childrenPart = new ArrayList<>();
        for (PartWrapper partWrapper : childrenPartWrappers) {
            childrenPart.add(partWrapper.getEntity());
        }
        return childrenPart;
    }

    private List<ApItem> getItemsFromPartWrappers(PartWrapper partWrapper, List<PartWrapper> childrenPartWrappers) {
        List<ApItem> items = new ArrayList<>(getItemsFromPartWrapper(partWrapper));
        for (PartWrapper pw : childrenPartWrappers) {
            items.addAll(getItemsFromPartWrapper(pw));
        }
        return items;
    }

    private List<ApItem> getItemsFromPartWrapper(PartWrapper pw) {
        List<ApItem> items = new ArrayList<>();
        for (ItemWrapper itemWrapper : pw.getItemQueue()) {
            items.add((ApItem) itemWrapper.getEntity());
        }
        return items;
    }

    public boolean updatePartValue(final ApPart apPart) {
        ApState state = getStateInternal(apPart.getAccessPoint());
        ApPart preferredNamePart = state.getAccessPoint().getPreferredPart();
        List<ApPart> childrenParts = partService.findPartsByParentPart(apPart);

        List<ApPart> parts = new ArrayList<>();
        parts.add(apPart);
        parts.addAll(childrenParts);

        List<ApItem> items = apItemService.findItemsByParts(parts);

        boolean preferred = preferredNamePart == null || Objects.equals(preferredNamePart.getPartId(), apPart.getPartId());
        GroovyResult result = groovyService.processGroovy(state, apPart, childrenParts, items, preferred);

        return partService.updatePartValue(apPart, result, state, state.getScope(), false, preferred);
    }


    /**
     * Aktualizace přístupového bodu - není verzované!
     *
     * @param accessPointId ID přístupového bodu
     * @param apTypeId měněný typ přístupového bodu
     * @return upravený přístupový bodu
     */
    @Transactional
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ApState changeApType(@AuthParam(type = AuthParam.Type.AP) final Integer accessPointId,
                                final Integer apTypeId) {
        Validate.notNull(accessPointId);
        Validate.notNull(apTypeId);

        // get ap
        ApAccessPoint accessPoint = getAccessPoint(accessPointId);
        ApState oldState = getStateInternal(accessPoint);

        // todo[ELZA-1727]
        // return updateState(accessPoint, oldState.getStateApproval(), oldState.getComment(), apTypeId, oldState.getScopeId());

        apDataService.validationNotDeleted(oldState);

        // check if modified
        if (apTypeId.equals(oldState.getApTypeId())) {
            return oldState;
        }

        // get ap type
        StaticDataProvider sdp = this.staticDataService.createProvider();
        ApType apType = sdp.getApTypeById(apTypeId);
        Validate.notNull(apType, "AP Type not found, id={}", apTypeId);

        ApChange change = apDataService.createChange(ApChange.Type.AP_UPDATE);
        oldState.setDeleteChange(change);
        apStateRepository.save(oldState);

        ApState newState = copyState(oldState, change);
        newState.setApType(apType);
        apStateRepository.save(newState);

       // accessPoint.setRuleSystem(apType.getRuleSystem());
        ApAccessPoint result = saveWithLock(accessPoint);
       /* if (result.getRuleSystem() != null) {
            //apGeneratorService.generateAndSetResult(accessPoint, change);
            apGeneratorService.generateAsyncAfterCommit(accessPointId, change.getChangeId());
        }*/

        publishAccessPointUpdateEvent(result);
        reindexDescItem(result);

        return newState;
    }

    /**
     * Nastaví pravidla přístupovému bodu podle typu.
     *
     * @param apState přístupový bod
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void setRuleAccessPoint(@AuthParam(type = AuthParam.Type.AP_STATE) final ApState apState) {
        Validate.notNull(apState, "Přístupový bod musí být vyplněn");
        apDataService.validationNotDeleted(apState);

        ApAccessPoint accessPoint = apState.getAccessPoint();

        saveWithLock(accessPoint);
        generateSync(accessPoint.getAccessPointId());
        accessPointCacheService.createApCachedAccessPoint(accessPoint.getAccessPointId());
    }

    /**
     * Získání přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu
     * @return přístupový bod
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_RD_ALL, UsrPermission.Permission.AP_SCOPE_RD})
    public ApAccessPoint getAccessPoint(@AuthParam(type = AuthParam.Type.AP) final Integer accessPointId) {
        return getAccessPointInternal(accessPointId);
    }

    /**
     * Získání přístupového bodu dle uuid
     *
     * @param uuid
     *            identifikátor přístupového bodu
     * @return přístupový bod
     */
    public ApAccessPoint getAccessPointByUuid(final String uuid) {
        ApAccessPoint accessPoint = apAccessPointRepository.findApAccessPointByUuid(uuid);
        if (accessPoint == null) {
            throw new ObjectNotFoundException("Přístupový bod neexistuje", BaseCode.ID_NOT_EXIST).setId(uuid);
        }
        return accessPoint;
    }

    /**
     * Získání přístupového bodu pro úpravu.
     *
     * @param accessPointId identifikátor přístupového bodu
     * @return přístupový bod
     */
    public ApAccessPoint getAccessPointInternal(final Integer accessPointId) {
        return apAccessPointRepository.findById(accessPointId)
                .orElseThrow(() -> new ObjectNotFoundException("Přístupový bod neexistuje", BaseCode.ID_NOT_EXIST).setId(accessPointId));
    }

    /**
     * Uložení AP s odverzováním.
     *
     * @param accessPoint přístupový bod
     * @return aktualizovaný přístupový bod
     */
    public ApAccessPoint saveWithLock(final ApAccessPoint accessPoint) {
        accessPoint.setLastUpdate(LocalDateTime.now());
        return apAccessPointRepository.saveAndFlush(accessPoint);
    }

    /**
     * Vyhledání všech jazyků seřazených podle kódu.
     *
     * @return nalezené jazyky
     */
    public List<SysLanguage> findAllLanguagesOrderByCode() {
        return sysLanguageRepository.findAll(Sort.by(Sort.Direction.ASC, SysLanguage.FIELD_CODE));
    }

    /**
     * Získání ApScope podle kódu
     * 
     * @param s kod
     * @return ApScope
     */
    public ApScope getApScope(String s) {
        ApScope entity = scopeRepository.findByCode(s);
        if(entity == null) {
            entity = new ApScope();
            entity.setCode(s);
            entity.setName(s);
        }
        return entity;
    }

    /**
     * Získání ApScope podle id
     * 
     * @param id
     * @return ApScope nebo null
     */
    public ApScope getApScope(final Integer scopeId) {
        if (scopeId == null) {
            return null;
        }
        return scopeRepository.findById(scopeId)
                .orElseThrow(() -> new ObjectNotFoundException("ApScope neexistuje", BaseCode.ID_NOT_EXIST).setId(scopeId));
    }

    /**
     * Získání ApScope podle External System 
     * 
     * @param extSystem
     * @return ApScope nebo null
     */
    public ApScope getApScope(SysExternalSystemVO extSystem) {
        if (extSystem instanceof ApExternalSystemVO) {
            return getApScope(((ApExternalSystemVO)extSystem).getScope());
        }
        return null;
    }

    /**
     * Získání typu.
     *
     * @param typeId identifikátor typu
     * @return typ
     */
    public ApType getType(final Integer typeId) {
        return apTypeRepository.findById(typeId)
                .orElseThrow(() -> new ObjectNotFoundException("Typ neexistuje", BaseCode.ID_NOT_EXIST).setId(typeId));
    }

    /**
     * Získání typů.
     */
    public List<ApType> findTypes() {
        return apTypeRepository.findAll();
    }

    /**
     * Získání jazyku podle kódu.
     *
     * @param languageCode kód jazyku
     * @return jazyk
     */
    public SysLanguage getLanguage(final String languageCode) {
        StaticDataProvider data = staticDataService.getData();
        SysLanguage language = data.getSysLanguageByCode(languageCode);
        if (language == null) {
            throw new ObjectNotFoundException("Jazyk neexistuje", BaseCode.ID_NOT_EXIST).setId(languageCode);
        }
        return language;
    }

    /**
     * Získání stavu přístupového bodu podle accessPointId.
     * 
     * Metoda neověřuje uživatelská oprávnění. Metodu je možné volat vně transakce.
     *
     * @param accessPointId
     *            přístupový bod
     * @return stav přístupového bodu
     */
    public ApState getStateInternal(final Integer accessPointId) {
        final ApState state = stateRepository.findLastByAccessPointId(accessPointId);
        if (state == null) {
            throw new ObjectNotFoundException("Stav pro přístupový bod neexistuje", BaseCode.INVALID_STATE)
                    .set("accessPointId", accessPointId);
        }
        return state;
    }

    /**
     * Získání stavu přístupového bodu podle accessPoint.
     * 
     * Metoda neověřuje uživatelská oprávnění
     *
     * @param accessPoint
     *            přístupový bod
     * @return stav přístupového bodu
     */
    @Transactional(TxType.MANDATORY)
    public ApState getStateInternal(final ApAccessPoint accessPoint) {
        final ApState state = stateRepository.findLastByAccessPoint(accessPoint);
        if (state == null) {
            throw new ObjectNotFoundException("Stav pro přístupový bod neexistuje", BaseCode.INVALID_STATE)
                    .set("accessPointId", accessPoint.getAccessPointId());
        }
        return state;
    }

    /**
     * Získání seznam stavu přístupovih bodu.
     * 
     * @param accessPoints seznam
     * @return List<ApState>
     */
    public List<ApState> getStatesInternal(final List<ApAccessPoint> accessPoints) {
        return stateRepository.findLastByAccessPoints(accessPoints);
    }

    public Map<Integer, ApState> groupStateByAccessPointId(final List<Integer> accessPointIds) {
        if (accessPointIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ApAccessPoint> accessPoints = apAccessPointRepository.findAllById(accessPointIds); // nahrat vsechny potrebne AP do Hibernate session
        Map<Integer, ApState> result = stateRepository.findLastByAccessPoints(accessPoints).stream()
                .collect(toMap(o -> o.getAccessPointId(), o -> o));
        for (ApAccessPoint accessPoint : accessPoints) {
            if (!result.containsKey(accessPoint.getAccessPointId())) {
                throw new ObjectNotFoundException("Stav pro přístupový bod neexistuje", BaseCode.INVALID_STATE)
                        .set("accessPointId", accessPoint.getAccessPointId());
            }
        }
        return result;
    }

    /**
     * Založení přístupového bodu.
     *
     * @param scope třída
     * @param type typ
     * @param change změna
     * @return přístupový bod
     */
    public ApState createAccessPoint(final ApScope scope, final ApType type, final StateApproval state,
                                     final ApChange change,
                                     String uuid) {
        ApAccessPoint accessPoint = new ApAccessPoint();
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        accessPoint.setUuid(uuid);
        accessPoint.setState(ApStateEnum.OK);
        accessPoint = saveWithLock(accessPoint);
        return createAccessPointState(accessPoint, scope, type, state, change);
    }

    public ApState createAccessPointState(ApAccessPoint ap, ApScope scope, ApType type, StateApproval state, ApChange change) {
        ApState apState = new ApState();
        apState.setAccessPoint(ap);
        apState.setScope(scope);
        apState.setApType(type);
        apState.setStateApproval(state);
        apState.setCreateChange(change);
        apState.setDeleteChange(null);
        return stateRepository.save(apState);
    }

    public ApState copyState(ApState oldState, ApChange change) {
        ApState newState = new ApState();
        newState.setAccessPoint(oldState.getAccessPoint());
        newState.setApType(oldState.getApType());
        newState.setScope(oldState.getScope());
        newState.setStateApproval(oldState.getStateApproval());
        newState.setComment(oldState.getComment());
        newState.setCreateChange(change);
        newState.setDeleteChange(null);
        return newState;
    }

    public void publishAccessPointCreateEvent(final ApAccessPoint accessPoint) {
        publishAccessPointEvent(accessPoint, EventType.ACCESS_POINT_CREATE);
    }

    public void publishAccessPointUpdateEvent(final ApAccessPoint accessPoint) {
        publishAccessPointEvent(accessPoint, EventType.ACCESS_POINT_UPDATE);
    }

    private void publishAccessPointDeleteEvent(final ApAccessPoint accessPoint) {
        publishAccessPointEvent(accessPoint, EventType.ACCESS_POINT_DELETE);
    }

    private void publishAccessPointEvent(final ApAccessPoint accessPoint, final EventType type) {
        eventNotificationService.publishEvent(EventFactory.createIdEvent(type, accessPoint.getAccessPointId()));
    }

    /**
     * @return identifikátor pro nové jméno AP
     */
    public int nextNameObjectId() {
        return sequenceService.getNext(OBJECT_ID_SEQUENCE_NAME);
    }

    @Transactional
    public void reindexDescItem(ApAccessPoint accessPoint) {
        Collection<Integer> itemIds = new HashSet<>(256);
        itemIds.addAll(apAccessPointRepository.findItemIdByAccessPointIdOverDataRecordRef(accessPoint.getAccessPointId()));
        descriptionItemService.reindexDescItem(itemIds);
    }

    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_RD_ALL, UsrPermission.Permission.AP_SCOPE_RD})
    public List<ApState> findApStates(@AuthParam(type = AuthParam.Type.AP) final ApAccessPoint apAccessPoint) {
        return apStateRepository.findByAccessPointFetch(apAccessPoint);
    }

    /**
     * Změna stavu přístupového bodu
     *
     * @param accessPoint přístupový bod
     * @param newStateApproval nový stav schvalování
     * @param newComment komentář k stavu (nepovinně)
     * @param newTypeId ID typu - <b>pokud je {@code null}, typ se nemění</b>
     * @param newScopeId ID oblasti entit - <b>pokud je {@code null}, oblast se nemění</b>
     * @return nový stav přístupového bodu (nebo starý, pokud nedošlo k žádné změně)
     */
    @Transactional
    public ApState updateApState(@NotNull ApAccessPoint accessPoint,
                                 @NotNull StateApproval newStateApproval,
                                 @Nullable String newComment,
                                 @Nullable Integer newTypeId,
                                 @Nullable Integer newScopeId) {

        Validate.notNull(newStateApproval, "AP State is null");

        ApState oldApState = getStateInternal(accessPoint);
        apDataService.validationNotDeleted(oldApState);

        boolean update = false;

        StateApproval oldStateApproval = oldApState.getStateApproval();
        if (!newStateApproval.equals(oldStateApproval)) {
            update = true;
        }

        if (!Objects.equals(newComment, oldApState.getComment())) {
            update = true;
        }

        ApScope oldApScope = oldApState.getScope();
        ApScope newApScope;

        // má uživatel oprávnění používat tyto stavy v této oblasti?
        if (!hasApPermission(oldApScope, oldStateApproval, newStateApproval)) {
            throw new SystemException("Uživatel nemá oprávnění na změnu přístupového bodu", BaseCode.INSUFFICIENT_PERMISSIONS)
                    .set("accessPointId", accessPoint.getAccessPointId())
                    .set("scopeId", oldApScope.getScopeId())
                    .set("oldState", oldStateApproval)
                    .set("newState", newStateApproval);
        }

        // má uživatel oprávnění na změnu oblasti přístupového bodu (archivní entity)?
        if (newScopeId != null && !newScopeId.equals(oldApScope.getScopeId())) {
            newApScope = getApScope(newScopeId);
            if (!hasApPermission(newApScope, oldStateApproval, newStateApproval)) {
                throw new SystemException("Uživatel nemá oprávnění na změnu oblasti přístupového bodu", BaseCode.INSUFFICIENT_PERMISSIONS)
                    .set("accessPointId", accessPoint.getAccessPointId())
                    .set("oldScopeId", oldApScope.getScopeId())
                    .set("newScopeId", newApScope.getScopeId());
            }
            if (!hasPermissionToChangeScope(oldStateApproval, oldApScope, newApScope)) {
                throw new SystemException("Uživatel nemá oprávnění na změnu oblasti přístupového bodu", BaseCode.INSUFFICIENT_PERMISSIONS)
                    .set("accessPointId", accessPoint.getAccessPointId())
                    .set("state", oldStateApproval)
                    .set("oldScopeId", oldApScope.getScopeId())
                    .set("newScopeId", newApScope.getScopeId());
            }
            update = true;
        } else {
            newApScope = null;
        }

        // má uživatel možnost nastavit požadovaný stav?
        if (!getNextStates(oldApState).contains(newStateApproval)) {
            throw new SystemException("Požadovaný stav entity nelze nastavit.", BaseCode.INSUFFICIENT_PERMISSIONS)
                .set("accessPointId", accessPoint.getAccessPointId())
                .set("scopeId", newApScope.getScopeId())
                .set("oldState", oldStateApproval)
                .set("newState", newStateApproval);
        }

        ApType newApType;

        // má uživatel oprávnění změnit třídu archivní entity?
        if (newTypeId != null && !newTypeId.equals(oldApState.getApTypeId())) {
            // nelze změnit třídu pokud existuje platná ApBindingState
            int countBinding = bindingStateRepository.countByAccessPoint(accessPoint);
            if (countBinding > 0) {
                throw new SystemException("Třídu entity z CAM nelze změnit.", BaseCode.INSUFFICIENT_PERMISSIONS)
                    .set("accessPointId", accessPoint.getAccessPointId())
                    .set("typeId", oldApState.getApTypeId());
            }
            if (!hasPermissionToChangeType(oldStateApproval, oldApScope)) {
                throw new SystemException("Požadovaný třídu entity nelze nastavit.", BaseCode.INSUFFICIENT_PERMISSIONS)
                    .set("accessPointId", accessPoint.getAccessPointId())
                    .set("state", oldStateApproval)
                    .set("scopeId", oldApScope.getScopeId());
            }

            // get ap type
            StaticDataProvider sdp = staticDataService.createProvider();
            newApType = sdp.getApTypeById(newTypeId);
            Validate.notNull(newApType, "AP Type not found, id={}", newTypeId);
            update = true;
        } else {
            newApType = null;
        }

        if (!update) {
            // nothing to update
            /*
            throw new BusinessException("Přístupový bod je v aktuálním stavu", BaseCode.INVALID_STATE)
                    .set("accessPointId", accessPoint.getAccessPointId())
                    .set("stateChange", stateChange);
            */
            return oldApState;
        }

        ApChange change = apDataService.createChange(ApChange.Type.AP_UPDATE);
        oldApState.setDeleteChange(change);
        apStateRepository.save(oldApState);

        ApState newApState = copyState(oldApState, change);
        if (newApScope != null) {
            newApState.setScope(newApScope);
        }
        if (newApType != null) {
            newApState.setApType(newApType);
        }
        newApState.setStateApproval(newStateApproval);
        newApState.setComment(newComment);
        apStateRepository.save(newApState);

        if (newApType != null) {
            saveWithLock(accessPoint);
        }

        publishAccessPointUpdateEvent(accessPoint);
        reindexDescItem(accessPoint);

        return newApState;
    }

    /**
     * Získání seznamu stavů do niž může být přístupový bod přepnut
     * 
     * @param apState
     * @return seznam stavů
     */
    public List<StateApproval> getNextStates(@NotNull ApState apState) {
        ApScope apScope = apState.getScope();
        UserDetail user = userService.getLoggedUserDetail();

        if (user.hasPermission(Permission.ADMIN)) {
            return Arrays.asList(StateApproval.values());
        }

        Set<StateApproval> result = new HashSet<>();

        // zakládání a změny nových 
        if (userService.hasPermission(Permission.AP_SCOPE_WR_ALL) 
                || userService.hasPermission(Permission.AP_SCOPE_WR, apScope.getScopeId())) {
            if (apState.getStateApproval().equals(StateApproval.NEW)) {
                result.add(StateApproval.TO_APPROVE);
                result.add(StateApproval.TO_AMEND);
            }
            if (apState.getStateApproval().equals(StateApproval.TO_AMEND)) {
                result.add(StateApproval.NEW);
                result.add(StateApproval.TO_APPROVE);
            }
            if (apState.getStateApproval().equals(StateApproval.TO_APPROVE)) {
                result.add(StateApproval.NEW);
                result.add(StateApproval.TO_AMEND);
            }
        }

        // schvalování
        if (userService.hasPermission(Permission.AP_CONFIRM_ALL) 
                || userService.hasPermission(Permission.AP_CONFIRM, apScope.getScopeId())) {
            if (apState.getStateApproval().equals(StateApproval.TO_APPROVE) ||
                    apState.getStateApproval().equals(StateApproval.REV_PREPARED)) {
                // kontrola, kdo přepnul do stavu ke schválení (nesmí být shodný uživatel)
                UsrUser prevUser = apState.getCreateChange().getUser();
                if (prevUser == null || !Objects.equals(prevUser.getUserId(), user.getId())) {
                    result.add(StateApproval.APPROVED);
                }
            }
        }

        // změna schválených
        if (userService.hasPermission(Permission.AP_EDIT_CONFIRMED_ALL) 
                || userService.hasPermission(Permission.AP_EDIT_CONFIRMED, apScope.getScopeId())) {
            if (apState.getStateApproval().equals(StateApproval.APPROVED)) {
                result.add(StateApproval.REV_NEW);
            }
            if (apState.getStateApproval().equals(StateApproval.REV_NEW)) {
                result.add(StateApproval.REV_PREPARED);
            }
            if (apState.getStateApproval().equals(StateApproval.REV_PREPARED)) {
                result.add(StateApproval.REV_AMEND);
            }
            if (apState.getStateApproval().equals(StateApproval.REV_AMEND)) {
                result.add(StateApproval.REV_PREPARED);
            }
        }

        // odstranění neplatných stavů, pokud existuje chybný stav
        if (apState.getAccessPoint().getState() == ApStateEnum.ERROR) {
            result.removeAll(Arrays.asList(StateApproval.TO_APPROVE, StateApproval.REV_PREPARED, StateApproval.APPROVED));
        }

        // zachování aktuálního stavu pro zvláštní případy
        if (apState.getStateApproval().equals(StateApproval.NEW)
                || apState.getStateApproval().equals(StateApproval.TO_AMEND)
                || apState.getStateApproval().equals(StateApproval.REV_NEW)
                || apState.getStateApproval().equals(StateApproval.REV_AMEND)) { 
            result.add(apState.getStateApproval());
        }

        return new ArrayList<>(result);
    }

    /**
     * Vyhodnocuje oprávnění přihlášeného uživatele k úpravám na přístupovém bodu dle uvedené oblasti entit.
     *
     * @param apScope oblast entit
     * @param oldStateApproval původní stav schvalování - při zakládání AP může být {@code null}
     * @param newStateApproval nový stav schvalování - pokud v rámci změny AP nedochází ke změně stavu schvalovaní, musí být stejný jako {@code oldStateApproval}
     * @return oprávění přihášeného uživatele ke změně AP
     * @throws BusinessException přechod mezi uvedenými stavy není povolen
     * @throws SystemException přechod mezi uvedenými stavy není povolen
     */
    public boolean hasApPermission(@NotNull ApScope apScope, StateApproval oldStateApproval, @NotNull StateApproval newStateApproval)
            throws BusinessException, SystemException {

        Assert.notNull(apScope, "AP Scope is null");
        Assert.notNull(newStateApproval, "New State Approval is null");

        // admin může cokoliv
        if (userService.hasPermission(Permission.ADMIN)) {
            return true;
        }

        // žádná změna stavu (pouze změny ApScope a/nebo komentáře)
        if (oldStateApproval != null && newStateApproval.equals(oldStateApproval)) {
            return true;
        }

        if (oldStateApproval != null &&
                (oldStateApproval.equals(StateApproval.APPROVED) && newStateApproval.equals(StateApproval.REV_NEW)) ||
                (oldStateApproval.equals(StateApproval.REV_NEW)
                        && newStateApproval.equals(StateApproval.REV_PREPARED)) ||
                (oldStateApproval.equals(StateApproval.REV_NEW)
                        && newStateApproval.equals(StateApproval.REV_AMEND)) ||
                (oldStateApproval.equals(StateApproval.REV_PREPARED)
                        && newStateApproval.equals(StateApproval.REV_AMEND)) ||
                (oldStateApproval.equals(StateApproval.REV_AMEND)
                        && newStateApproval.equals(StateApproval.REV_PREPARED))) {

            // k editaci již schválených přístupových bodů je potřeba "Změna schválených přístupových bodů"
            return userService.hasPermission(Permission.AP_EDIT_CONFIRMED_ALL)
                    || userService.hasPermission(Permission.AP_EDIT_CONFIRMED, apScope.getScopeId());

        } else {

            // "Schvalování přístupových bodů" může:
            // - cokoliv
            if (userService.hasPermission(Permission.AP_CONFIRM_ALL)
                    || userService.hasPermission(Permission.AP_CONFIRM, apScope.getScopeId())) {
                return true;
            }

            // "Zakládání a změny nových" může:
            // - nastavení stavu "Nový", "Ke schválení" i "K doplnění"
            if (newStateApproval.equals(StateApproval.TO_AMEND) || newStateApproval.equals(StateApproval.TO_APPROVE) || newStateApproval.equals(StateApproval.NEW)) {
                if (userService.hasPermission(Permission.AP_SCOPE_WR_ALL)
                        || userService.hasPermission(Permission.AP_SCOPE_WR, apScope.getScopeId())) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Má uživatel možnost na změnu oblasti přístupového bodu (archivní entity)?
     * 
     * @param stateApproval
     * @param oldApScope
     * @param newApScope
     * @return
     */
    private boolean hasPermissionToChangeScope(StateApproval stateApproval, ApScope oldApScope, ApScope newApScope) {
        Assert.notNull(stateApproval, "State Approval is null");
        Assert.notNull(oldApScope, "Old ApScope is null");
        Assert.notNull(newApScope, "New ApScope is null");

        // admin může cokoliv
        if (userService.hasPermission(Permission.ADMIN)) {
            return true;
        }

        if (userService.hasPermission(Permission.AP_SCOPE_WR_ALL)
                || (userService.hasPermission(Permission.AP_SCOPE_WR, oldApScope.getScopeId())
                        && userService.hasPermission(Permission.AP_SCOPE_WR, newApScope.getScopeId()))) {

            if (stateApproval.equals(StateApproval.NEW) || stateApproval.equals(StateApproval.TO_AMEND)) {
                return true;
            }
        }

        if (userService.hasPermission(Permission.AP_EDIT_CONFIRMED_ALL) 
                || (userService.hasPermission(Permission.AP_EDIT_CONFIRMED, oldApScope.getScopeId())
                        && userService.hasPermission(Permission.AP_EDIT_CONFIRMED, newApScope.getScopeId()))) {

            if (stateApproval.equals(StateApproval.REV_NEW) || stateApproval.equals(StateApproval.REV_AMEND)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Má uživatel možnost změnit třídu archivní entity?
     * 
     * @param stateApproval
     * @return
     */
    private boolean hasPermissionToChangeType(StateApproval stateApproval, ApScope apScope) {
        Assert.notNull(stateApproval, "State Approval is null");
        Assert.notNull(apScope, "ApScope is null");

        // admin může cokoliv
        if (userService.hasPermission(Permission.ADMIN)) {
            return true;
        }

        if (userService.hasPermission(Permission.AP_SCOPE_WR_ALL)
                || (userService.hasPermission(Permission.AP_SCOPE_WR, apScope.getScopeId()))) {

            if (stateApproval.equals(StateApproval.NEW) || stateApproval.equals(StateApproval.TO_AMEND)) {
                return true;
            }
        }

        if (userService.hasPermission(Permission.AP_EDIT_CONFIRMED_ALL) 
                || (userService.hasPermission(Permission.AP_EDIT_CONFIRMED, apScope.getScopeId()))) {

            if (stateApproval.equals(StateApproval.REV_NEW) || stateApproval.equals(StateApproval.REV_AMEND)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Kontrola, zda přihlášený uživatel má právo synchronizovat archivni entitu z externího systému.
     * 
     * @param state
     */
    public void hasPermissionToSynchronizeFromExternaSystem(final ApState state) {

        // Uživatel, který NEMÁ OPRÁVNĚNÍ "Zakládání a změny nových" (AP_SCOPE_WR)
        // nemůže provést operaci "Aktualizace údajů z externího systému" u entit ve stavech:
        // - Nová
        // - K doplnění
        boolean hasCreateAndChangeNewAp = 
                userService.hasPermission(Permission.AP_SCOPE_WR_ALL)
                || userService.hasPermission(Permission.AP_SCOPE_WR, state.getScopeId());
        boolean stateNewOrToAmend = 
                state.getStateApproval().equals(StateApproval.NEW)
                || state.getStateApproval().equals(StateApproval.TO_AMEND);
        if (!hasCreateAndChangeNewAp && stateNewOrToAmend) {
            throw new SystemException("Entitu v tomto stavu nelze aktualizovat z externího systému", BaseCode.INVALID_STATE)
                .set("accessPointId", state.getAccessPointId())
                .set("scopeId", state.getScopeId())
                .set("state", state.getStateApproval());
        }

        // Uživatel, který NEMÁ OPRÁVNĚNÍ "Změna schválených archivních entit" (AP_EDIT_CONFIRMED)
        // nemůže provést operaci "Aktualizace údajů z externího systému" u entit ve stavech:
        // - Schválená
        // - Příprava revize
        // - Revize k doplnění
        boolean hasEditConfirmedAp =
                userService.hasPermission(Permission.AP_EDIT_CONFIRMED_ALL) 
                || userService.hasPermission(Permission.AP_EDIT_CONFIRMED, state.getScopeId());
        boolean stateApprovedOrRewNewOrRevAmend =
                state.getStateApproval().equals(StateApproval.APPROVED)
                || state.getStateApproval().equals(StateApproval.REV_NEW)
                || state.getStateApproval().equals(StateApproval.REV_AMEND);
        if (!hasEditConfirmedAp && stateApprovedOrRewNewOrRevAmend) {
            throw new SystemException("Uživatel nemá oprávnění na synchronizaci přístupového bodu z externího systému", BaseCode.INSUFFICIENT_PERMISSIONS)
                .set("accessPointId", state.getAccessPointId())
                .set("scopeId", state.getScopeId())
                .set("state", state.getStateApproval());
        }
    }

    /**
     * Kontrola, zda přihlášený uživatel má právo upravovat schválenou entitu.
     *
     * @param state state entity
     */
    public void hasPermissionForEditingConfirmed(final ApState state) {
        if (state.getStateApproval() == StateApproval.APPROVED) {

            if (!userService.hasPermission(Permission.ADMIN)
                    && !userService.hasPermission(Permission.AP_EDIT_CONFIRMED_ALL)
                    && !userService.hasPermission(Permission.AP_EDIT_CONFIRMED, state.getScopeId())) {
                throw new SystemException("Uživatel nemá oprávnění na změnu přístupového bodu", BaseCode.INSUFFICIENT_PERMISSIONS)
                        .set("accessPointId", state.getAccessPointId())
                        .set("scopeId", state.getScopeId());
            }
        }
    }

    /**
     * Nastaví část přístupového bodu na preferovanou
     *
     * @param accessPoint přístupový bod
     * @param apPart část
     */
    public ApAccessPoint setPreferName(final ApAccessPoint accessPoint, final ApPart apPart) {
    	Integer oldPrefNameId = accessPoint.getPreferredPartId();
    	if(Objects.equals(oldPrefNameId, apPart.getPartId())) {
    		return accessPoint;
    	}
    	
        StaticDataProvider sdp = StaticDataProvider.getInstance();
        RulPartType defaultPartType = sdp.getDefaultPartType();

        if (!apPart.getPartType().getCode().equals(defaultPartType.getCode())) {
            throw new IllegalArgumentException("Preferované jméno musí být typu " + defaultPartType.getCode());
        }

        if (apPart.getParentPart() != null) {
            throw new IllegalArgumentException("Návazný part nelze změnit na preferovaný.");
        }        
       	partService.unsetPreferredPart(oldPrefNameId);
        accessPoint.setPreferredPart(apPart);

        ApAccessPoint ret = saveWithLock(accessPoint);
        generateSync(ret.getAccessPointId());
        return ret;
    }

    @AuthMethod(permission = {UsrPermission.Permission.AP_EXTERNAL_WR})
    public void disconnectAccessPoint(Integer accessPointId, String externalSystemCode) {
        ApAccessPoint accessPoint = getAccessPoint(accessPointId);
        ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);

        ApBindingState bindingState = bindingStateRepository.findByAccessPointAndExternalSystem(accessPoint, apExternalSystem);
        ApBinding binding = bindingState.getBinding();
        dataRecordRefRepository.disconnectBinding(binding);
        bindingItemRepository.deleteByBinding(binding);
        bindingStateRepository.deleteByBinding(binding);
        bindingRepository.delete(binding);
        accessPointCacheService.createApCachedAccessPoint(accessPointId);
    }

    public List<String> findRelArchiveEntities(ApAccessPoint accessPoint) {
        List<String> archiveEntityIds = new ArrayList<>();
        List<ApItem> itemList = itemRepository.findValidItemsByAccessPoint(accessPoint);

        for (ApItem item : itemList) {
            if (item.getData() instanceof ArrDataRecordRef) {
                ArrDataRecordRef dataRecordRef = (ArrDataRecordRef) item.getData();
                if (dataRecordRef.getRecord() == null) {
                    archiveEntityIds.add(dataRecordRef.getBinding().getValue());
                }
            }
        }
        return archiveEntityIds;
    }

    public void checkUniqueExtSystem(final ApAccessPoint accessPoint, final ApExternalSystem externalSystem) {
        ApBindingState bindingState = externalSystemService.findByAccessPointAndExternalSystem(accessPoint, externalSystem);
        if (bindingState != null) {
            throw new BusinessException("Tato archivní entita má jíž existující propojení s externím systémem", RegistryCode.EXT_SYSTEM_CONNECTED)
                    .level(Level.WARNING);
        }
    }

    public List<Integer> findApTypeIdsByItemTypeAndItemSpec(Integer itemTypeId, @Nullable Integer itemSpecId) {
        StaticDataProvider sdp = staticDataService.getData();
        RulItemType itemType = sdp.getItemType(itemTypeId);
        RulItemSpec itemSpec = itemSpecId != null ? sdp.getItemSpec(itemSpecId) : null;
        List<RulItemAptype> rulItemAptypeList = itemAptypeRepository.findAll();
        List<Integer> aeTypeIds = new ArrayList<>();

        for (RulItemAptype rulItemAptype : rulItemAptypeList) {
            if ((rulItemAptype.getItemType() == null || rulItemAptype.getItemType().getCode().equals(itemType.getCode()))
                    && (rulItemAptype.getItemSpec() == null || (itemSpec != null && rulItemAptype.getItemSpec().getCode().equals(itemSpec.getCode())))) {
                aeTypeIds.add(rulItemAptype.getApType().getApTypeId());
            }
        }

        return aeTypeIds;
    }

    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_RD_ALL, UsrPermission.Permission.AP_SCOPE_RD})
    public ArchiveEntityResultListVO findAccessPointsForRel(Integer from, Integer max, 
                                                            @AuthParam(type = AuthParam.Type.SCOPE) Integer scopeId, SearchFilterVO filter) {
        searchFilterFactory.completeApTypesTreeInFilter(filter);
        Set<Integer> scopeList = new HashSet<>();
        scopeList.add(scopeId);
        scopeList.addAll(scopeRelationRepository.findConnectedScopeIdsByScopeIds(Collections.singleton(scopeId)));
        List<ApState> stateList = apAccessPointRepository.findApAccessPointByTextAndType(filter.getSearch(), filter.getAeTypeIds(), from, max, scopeList, null , null, null);

//        ApStateSpecification stateSpecification = new ApStateSpecification(filter);
//        PageRequest pageRequest = new PageRequest(from, max);
//        Page<ApState> pageResult = stateRepository.findAll(stateSpecification, pageRequest);

        final List<ApAccessPoint> accessPoints = stateList.stream()
                .map(ApState::getAccessPoint)
                .collect(Collectors.toList());

        final Map<Integer, ApIndex> nameMap = findPreferredPartIndexMap(accessPoints);

        return searchFilterFactory.createArchiveEntityResultListVO(stateList, stateList.size(), nameMap);
    }

    public Resource exportCoordinates(FileType fileType, Integer itemId) {
        ApItem item = itemRepository.findById(itemId).orElseThrow(() ->
                new ObjectNotFoundException("ApItem nenalezen", BaseCode.ID_NOT_EXIST));
        String coordinates;

        if (fileType.equals(FileType.WKT)) {
            coordinates = item.getData().getFulltextValue();
        } else {
            coordinates = convertCoordinates(fileType, item.getData().getDataId());
        }
        return new ByteArrayResource(coordinates.getBytes(StandardCharsets.UTF_8));
    }

    private String convertCoordinates(FileType fileType, Integer dataId) {
        switch (fileType) {
            case KML:
                return apDataService.convertCoordinatesToKml(dataId);
            case GML:
                return apDataService.convertCoordinatesToGml(dataId);
            default:
                throw new IllegalStateException("Nepovolený typ souboru pro export souřadnic");
        }
    }

    public String importCoordinates(FileType fileType, Resource body) {
        try {
            String content = IOUtils.toString(body.getInputStream(), StandardCharsets.UTF_8);
            switch (fileType) {
                case KML:
                    content = content.substring(1, content.length() - 1);
                    return "\"" + apDataService.convertCoordinatesFromKml(content) + "\"";
                case GML:
                    content = content.substring(1, content.length() - 1);
                    return "\"" + apDataService.convertCoordinatesFromGml(content) + "\"";
                case WKT:
                    return content;
                default:
                    throw new IllegalStateException("Nepovolený typ souboru pro import souřadnic");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Chyba při importu souřadnic ze souboru");
        }
    }

    public MultiValueMap<String, String> createCoordinatesHeaders(FileType fileType) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        String extension;
        String contentType;

        switch (fileType) {
            case WKT:
                extension = "wkt";
                contentType = "application/octet-stream";
                break;
            case GML:
                extension = "gml";
                contentType = "application/gml+xml";
                break;
            case KML:
                extension = "kml";
                contentType = "application/vnd.google-earth.kml+xml";
                break;
            default:
                throw new IllegalStateException("Nepovolený typ souboru pro export souřadnic");
        }

        headers.add("Content-type",  contentType + "; charset=utf-8");
        headers.add("Content-disposition", "attachment; filename=file." + extension);
        return headers;
    }

    @Transactional(TxType.MANDATORY)
    public void generateSync(final ApAccessPoint accessPoint, final ApPart apPart) {
        boolean successfulGeneration = updatePartValue(apPart);
        validate(accessPoint, successfulGeneration);
    }

    private void validate(ApAccessPoint accessPoint, boolean successfulGeneration) {
        ApValidationErrorsVO apValidationErrorsVO = ruleService.executeValidation(accessPoint);
        updateValidationErrors(accessPoint, apValidationErrorsVO, successfulGeneration);
    }


    public void generateSync(final Integer accessPointId) {
        ApAccessPoint accessPoint = getAccessPointInternal(accessPointId);
        ApState apState = getStateInternal(accessPoint);
        List<ApPart> partList = partService.findPartsByAccessPoint(accessPoint);
        Map<Integer, List<ApItem>> itemMap = itemRepository.findValidItemsByAccessPoint(accessPoint).stream()
                .collect(Collectors.groupingBy(ApItem::getPartId));

        generateSync(accessPoint, apState, partList, itemMap, false);
    }

    @Transactional(TxType.MANDATORY)
    public void generateSync(final ApAccessPoint accessPoint,
                             final ApState apState,
                             final List<ApPart> partList,
                             final Map<Integer, List<ApItem>> itemMap,
                             boolean async) {

        Integer prefPartId = accessPoint.getPreferredPartId();
        boolean successfulGeneration = updatePartValues(apState, prefPartId, partList, itemMap, async);
        validate(accessPoint, successfulGeneration);
    }


    /**
     * Zapsání validačních chyb přístupového bodu do databáze.
     *
     * @param apValidationErrorsVO chyby přístupového bodu
     * @param successfulGeneration úspěšné generování keyValue
     */
    public void updateValidationErrors(final ApAccessPoint accessPoint,
                                       final ApValidationErrorsVO apValidationErrorsVO,
                                       final boolean successfulGeneration) {

        StringBuilder accessPointErrors = new StringBuilder();
        if (CollectionUtils.isNotEmpty(apValidationErrorsVO.getErrors())) {
            for (String error : apValidationErrorsVO.getErrors()) {
                accessPointErrors.append(error).append("\n");
            }
        }
        if (!successfulGeneration) {
            accessPointErrors.append("Duplicitní key value přístupového bodu.");
        }

        List<ApPart> partList = partService.findPartsByAccessPoint(accessPoint);
        boolean partError = false;
        if (CollectionUtils.isNotEmpty(partList)) {
            for (ApPart part : partList) {
                PartValidationErrorsVO partErrorList = findPartValidationErrors(part.getPartId(), apValidationErrorsVO);
                if (partErrorList != null && CollectionUtils.isNotEmpty(partErrorList.getErrors())) {
                    StringBuilder partErrors = new StringBuilder();
                    for (String error : partErrorList.getErrors()) {
                        partErrors.append(error).append("\n");
                    }
                    part.setErrorDescription(partErrors.toString());
                    part.setState(ApStateEnum.ERROR);
                    partError = true;
                } else {
                    part.setErrorDescription(null);
                    part.setState(ApStateEnum.OK);
                }
            }

            partRepository.saveAll(partList);
        }


        if (StringUtils.isNotEmpty(accessPointErrors.toString()) || partError) {
            accessPoint.setErrorDescription(accessPointErrors.toString());
            accessPoint.setState(ApStateEnum.ERROR);
        } else {
            accessPoint.setErrorDescription(null);
            accessPoint.setState(ApStateEnum.OK);
        }

        apAccessPointRepository.save(accessPoint);
    }

    @Nullable
    private PartValidationErrorsVO findPartValidationErrors(final Integer partId,
                                                            final ApValidationErrorsVO apValidationErrorsVO) {
        if (apValidationErrorsVO != null && CollectionUtils.isNotEmpty(apValidationErrorsVO.getPartErrors())) {
            for (PartValidationErrorsVO partValidationErrors : apValidationErrorsVO.getPartErrors()) {
                if (partValidationErrors.getId().equals(partId)) {
                    return partValidationErrors;
                }
            }
        }
        return null;
    }

    public Map<Integer, ApIndex> findPreferredPartIndexMap(Collection<ApAccessPoint> accessPoints) {
        return indexRepository.findPreferredPartIndexByAccessPointsAndIndexType(accessPoints, DISPLAY_NAME).stream()
                .collect(Collectors.toMap(i -> i.getPart().getAccessPointId(), Function.identity()));
    }

    public ApIndex findPreferredPartIndex(ApAccessPoint accessPoint) {
        return indexRepository.findPreferredPartIndexByAccessPointAndIndexType(accessPoint, DISPLAY_NAME);
    }

    public String findPreferredPartDisplayName(ApAccessPoint accessPoint) {
        CachedAccessPoint cachedAccessPoint = accessPointCacheService.findCachedAccessPoint(accessPoint.getAccessPointId());
        if (cachedAccessPoint == null || cachedAccessPoint.getParts() == null) {
            return "";
        }
        CachedPart preferredPart = cachedAccessPoint.getParts().stream()
                .filter(p -> p.getPartId().equals(cachedAccessPoint.getPreferredPartId()))
                .findAny()
                .orElse(null);
        ApIndex index = preferredPart.getIndices().stream()
                .filter(p -> p.getIndexType().equals(DISPLAY_NAME))
                .findAny()
                .orElse(null);
        return index == null ? "": index.getValue();
    }

    public ExtSyncsQueueResultListVO findExternalSyncs(Integer from, Integer max, 
                                                       String externalSystemCode, 
                                                       SyncsFilterVO filter) {
        ExtSyncsQueueResultListVO result = new ExtSyncsQueueResultListVO();
        List<cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState> states;
        if(CollectionUtils.isNotEmpty(filter.getStates())) {
            states = filter.getStates().stream()
                    .map(s -> cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState.fromValue(s.name()))
                    .collect(Collectors.toList());
        } else {
            states = null;
        }
        
        List<ExtSyncsQueueItem> items = extSyncsQueueItemRepository.findExtSyncsQueueItemsByExternalSystemAndScopesAndState(externalSystemCode, states, filter.getScopes(), from, max);

        result.setTotal(items.size());
        result.setData(createExtSyncsQueueItemVOList(items));
        return result;
    }

    private List<ExtSyncsQueueItemVO> createExtSyncsQueueItemVOList(List<ExtSyncsQueueItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return Collections.emptyList();
        }

        List<ExtSyncsQueueItemVO> extSyncsQueueItemVOList = new ArrayList<>(items.size());

        final List<ApAccessPoint> accessPoints = items.stream()
                .map(ExtSyncsQueueItem::getAccessPoint)
                .collect(Collectors.toList());
        final Map<Integer, ApIndex> nameMap = findPreferredPartIndexMap(accessPoints);
        final Map<Integer, ApState> stateMap = apStateRepository.findLastByAccessPoints(accessPoints).stream()
                .collect(Collectors.toMap(ApState::getAccessPointId, Function.identity()));

        for (ExtSyncsQueueItem extSyncsQueueItem : items) {
            String name = nameMap.get(extSyncsQueueItem.getAccessPointId()) != null ? nameMap.get(extSyncsQueueItem
                    .getAccessPointId()).getValue() : null;
            ApState state = stateMap.get(extSyncsQueueItem.getAccessPointId());
            extSyncsQueueItemVOList.add(createExtSyncsQueueItemVO(extSyncsQueueItem, name, state.getScopeId()));
        }

        return extSyncsQueueItemVOList;
    }

    private ExtSyncsQueueItemVO createExtSyncsQueueItemVO(ExtSyncsQueueItem extSyncsQueueItem,
                                                          String name, Integer scopeId) {
        ExtSyncsQueueItemVO extSyncsQueueItemVO = new ExtSyncsQueueItemVO();
        extSyncsQueueItemVO.setId(extSyncsQueueItem.getExtSyncsQueueItemId());
        extSyncsQueueItemVO.setAccessPointId(extSyncsQueueItem.getAccessPoint().getAccessPointId());
        extSyncsQueueItemVO.setAccessPointName(name);
        extSyncsQueueItemVO.setScopeId(scopeId);
        extSyncsQueueItemVO.setState(ExtAsyncQueueState.fromValue(extSyncsQueueItem.getState().name()));
        extSyncsQueueItemVO.setStateMessage(extSyncsQueueItem.getStateMessage());
        extSyncsQueueItemVO.setDate(extSyncsQueueItem.getDate().toLocalDateTime());
        return extSyncsQueueItemVO;
    }


    public Page<ApState> findApAccessPointBySearchFilter(SearchFilterVO searchFilter, Set<Integer> apTypeIdTree, Set<Integer> scopeIds,
                                                         StateApproval state, Integer from, Integer count, StaticDataProvider sdp) {
        int page = from / count;
        ApStateSpecification specification = new ApStateSpecification(searchFilter, apTypeIdTree, scopeIds, state, sdp);
        return stateRepository.findAll(specification, PageRequest.of(page, count));
    }

    public boolean isQueryComplex(SearchFilterVO searchFilter) {
        //todo fantiš definovat příliš složitý dotaz
        return false;
    }

    /**
     * Pomocná třída pro založení změny až při její první potřebě.
     */
    private class ApChangeNeed {

        /**
         * Zakládaný typ změny.
         */
        private final ApChange.Type type;

        /**
         * Založená změna.
         */
        private ApChange change;

        ApChangeNeed(final ApChange.Type type) {
            this.type = type;
        }

        /**
         * @return získání/založení změny
         */
        public ApChange get() {
            if (change == null) {
                change = apDataService.createChange(type);
            }
            return change;
        }
    }

    public void updateDataRefs(ApAccessPoint accessPoint, ApBinding binding) {
        List<ArrDataRecordRef> dataRecordRefList = dataRecordRefRepository.findByBindingIn(Collections.singletonList(
                                                                                                                     binding));
        setAccessPointInDataRecordRefs(accessPoint, dataRecordRefList, binding);

        dataRecordRefRepository.saveAll(dataRecordRefList);

        List<ApPart> partList = itemRepository.findPartsByDataRecordRefList(dataRecordRefList);
        if (CollectionUtils.isNotEmpty(partList)) {
            for (ApPart part : partList) {
                updatePartValue(part);
            }
        }
    }

    /**
     * Find access point by multiple items in one part
     *
     * @param entity
     * @param itemSpec
     * @param value
     * @return List<ApAccessPoint>
     */
    public List<ApAccessPoint> findAccessPointsBySinglePartValues(List<Object> criterias) {

        return apAccessPointRepository.findAccessPointsBySinglePartValues(criterias);
    }

    /**
     * Get access point state by string
     * 
     * @param accessPointId
     * @return ApState
     */
    public ApState getApState(String accessPointId) {

        Validate.notNull(accessPointId, "Identifikátor archivní entity musí být vyplněn");

        if (accessPointId.length() == 36) {
            ApAccessPoint ap = getAccessPointByUuid(accessPointId);
            return getApState(ap);
        } else {
            try {
                Integer apId = Integer.parseInt(accessPointId);
                return getApState(apId);
            } catch (NumberFormatException nfe) {
                throw new SystemException("Unrecognized ID format")
                        .set("ID", accessPointId);
            }
        }
    }

    /**
     * Získání stavu přístupového bodu.
     * 
     * Metoda ověřuje uživatelská oprávnění
     * 
     * @param accessPoint
     * @return
     */
    public ApState getApState(ApAccessPoint accessPoint) {
        ApState apState = getStateInternal(accessPoint);
        // check permissions
        AuthorizationRequest authRequest = AuthorizationRequest.hasPermission(UsrPermission.Permission.AP_SCOPE_RD_ALL)
                .or(UsrPermission.Permission.AP_SCOPE_RD, apState.getScopeId());
        userService.authorizeRequest(authRequest);

        return apState;

    }

    /**
     * Získání stavu přístupového bodu.
     * 
     * Metoda ověřuje uživatelská oprávnění
     * 
     * @param accessPointId
     * @return ApState
     */
    public ApState getApState(Integer accessPointId) {
        ApAccessPoint ap = getAccessPointInternal(accessPointId);
        return getApState(ap);
    }

    /**
     * Kontrola datové struktury.
     * 
     * Tato metoda se volá, pokud parametr elza.ap.checkDb má hodnotu TRUE
     */
    public void checkConsistency() {
        int partsWithChild = partRepository.countDeletedPartsWithUndeletedChild();
        if (partsWithChild > 0) {
            logger.error("Existují {} vymazané Parts s nevymazanými potomky", partsWithChild);
            throw new IllegalStateException("There are deleted Part(s) with non-deleted Children(s)");
        }
        int partsWithBindingItem = partRepository.countDeletedPartsWithUndeletedBindingItem();
        if (partsWithBindingItem > 0) {
            logger.error("Existují {} vymazané Parts s nevymazanými BindingItem", partsWithBindingItem);
            throw new IllegalStateException("There are deleted Part(s) with non-deleted BindingItem(s)");
        }
        int partsWithItem = partRepository.countDeletedPartsWithUndeletedItem();
        if (partsWithItem > 0) {
            logger.error("Existují {} vymazané Parts s nevymazanými Item", partsWithItem);
            throw new IllegalStateException("There are deleted Part(s) with non-deleted Item(s)");
        }
        int itemsWithBindingItem = itemRepository.countDeletedItemsWithUndeletedBindingItem();
        if (itemsWithBindingItem > 0) {
            logger.error("Existují {} vymazané Items s nevymazanými BindingItem", itemsWithBindingItem);
            throw new IllegalStateException("There are deleted Items(s) with non-deleted BindingItem(s)");
        }
    }

    /**
     * Sloučení Parts z accessPoint do targetAccessPoint
     * 
     * @param accessPoint
     * @param targetAccessPoint
     * @param change
     */
    private void mergeParts(ApAccessPoint accessPoint, ApAccessPoint targetAccessPoint, ApChange change) {
        List<ApPart> partsFrom = partService.findPartsByAccessPoint(accessPoint);
        Map<Integer, List<ApItem>> itemMapFrom = itemRepository.findValidItemsByAccessPoint(accessPoint).stream()
                .collect(Collectors.groupingBy(ApItem::getPartId));

        List<ApPart> partsTo = partService.findPartsByAccessPoint(targetAccessPoint);
        Map<Integer, List<ApItem>> itemMapTo = itemRepository.findValidItemsByAccessPoint(targetAccessPoint).stream()
                .collect(Collectors.groupingBy(ApItem::getPartId));

        // příprava seznamu objektů ke sloučení
        List<PartWithSubParts> partsWSFrom = prepareListPartWithSubParts(partsFrom);
        List<PartWithSubParts> partsWSTo = prepareListPartWithSubParts(partsTo);
        Map<Integer, PartWithSubParts> partsWSMapTo = partsWSTo.stream()
                .collect(Collectors.toMap(p -> p.getPart().getPartId(), p -> p));

        for (PartWithSubParts partWSFrom : partsWSFrom) {
            ApPart partFrom = partWSFrom.getPart();
            ApPart targetPart = null;
            if (!partFrom.getPartType().getRepeatable()) {
                targetPart = partService.findFirstPartByCode(partFrom.getPartType().getCode(), partsTo);
            } else {
                // try to find the same part
                PartWithSubParts targetPartWS = findPartWSInList(partWSFrom, partsWSTo, itemMapFrom, itemMapTo);
                // if exists -> merge is not needed
                if (targetPartWS != null) {
                    continue;
                }
            }

            // kopírování Part
            if (targetPart == null) {
                ApPart partTo = copyPartWithItems(partFrom, targetAccessPoint, null, itemMapFrom.get(partFrom.getPartId()), change);

                // kopírování podřízených Part
                List<ApPart> subPartsFrom = partWSFrom.getSubParts();
                for (ApPart subPart : subPartsFrom) {
                    copyPartWithItems(subPart, targetAccessPoint, partTo, itemMapFrom.get(subPart.getPartId()), change);
                }
            } else {
                // sloučení ApItems dvou Part
                mergeItems(itemMapFrom.get(partFrom.getPartId()), targetPart, itemMapTo.get(targetPart.getPartId()), change);

                // sloučení podřízených Part
                List<ApPart> targetSubParts = partsWSMapTo.get(targetPart.getPartId()).getSubParts();
                List<ApPart> subPartsFrom = partWSFrom.getSubParts();
                for (ApPart subPartFrom : subPartsFrom) {
                    ApPart findPart = findApPartInList(subPartFrom, targetSubParts, itemMapFrom.get(subPartFrom.getPartId()), itemMapTo);
                    if (findPart == null) {
                        copyPartWithItems(subPartFrom, targetAccessPoint, targetPart, itemMapFrom.get(subPartFrom.getPartId()), change);
                    }
                }
            }
        }
    }

    /**
     * Získání seznamu objektů PartWithSubParts ze seznamu objektů ApPart
     * 
     * @param parts
     * @return
     */
    private List<PartWithSubParts> prepareListPartWithSubParts(List<ApPart> parts) {
        Map<Integer, PartWithSubParts> mapPartsWithSubParts = new HashMap<>();
        for (ApPart part : parts) {
            if (part.getParentPart() != null) {
                PartWithSubParts target = mapPartsWithSubParts.computeIfAbsent(part.getParentPartId(), p -> new PartWithSubParts());
                target.addSubPart(part);
            } else {
                PartWithSubParts target = mapPartsWithSubParts.computeIfAbsent(part.getPartId(), p -> new PartWithSubParts());
                target.setPart(part);
            }
        }

        return new ArrayList<>(mapPartsWithSubParts.values());
    }

    /**
     * Vyhledavání úplně shodné ApPart v seznamu parts včetně prvků popisu
     * 
     * @param part
     * @param parts
     * @param items
     * @param itemMap
     * @return ApPart
     */
    private ApPart findApPartInList(ApPart part, List<ApPart> parts, List<ApItem> items, Map<Integer, List<ApItem>> itemMap) {
        for (ApPart p : parts) {
            if (equalsPart(part, p, items, itemMap.get(p.getPartId()))) {
                return p;
            }
        }
        return null;
    }

    /**
     * Vyhledavání úplně shodné části v seznamu parts včetně podřízených parts a prvků popisu 
     * 
     * @param partF
     * @param partsTo
     * @param itemMapFrom
     * @param itemMapTo
     * @return PartWithSubParts
     */
    private PartWithSubParts findPartWSInList(PartWithSubParts partF, List<PartWithSubParts> partsTo, Map<Integer, List<ApItem>> itemMapFrom, Map<Integer, List<ApItem>> itemMapTo) {
        for (PartWithSubParts p : partsTo) {
            if (equalsPart(partF.getPart(), p.getPart(), itemMapFrom.get(partF.getPart().getPartId()), itemMapTo.get(p.getPart().getPartId()))
                    && equalsParts(partF.getSubParts(), p.getSubParts(), itemMapFrom, itemMapTo)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Porovnání dvou seznamů ApParts
     * 
     * @param partsF
     * @param partsT
     * @param itemMapF
     * @param itemMapT
     * @return boolean 
     */
    private boolean equalsParts(List<ApPart> partsF, List<ApPart> partsT, Map<Integer, List<ApItem>> itemMapF, Map<Integer, List<ApItem>> itemMapT) {
        for (ApPart pF : partsF) {
            boolean foundPart = false;
            for (ApPart pT : partsT) {
                if (equalsPart(pF, pT, itemMapF.get(pF.getPartId()), itemMapT.get(pT.getPartId()))) {
                    foundPart = true;
                }
            }
            if (!foundPart) {
                return false;
            }
        }
        return true;
    }    

    /**
     * Porovnání dvou ApPart (s ApItems) 
     * 
     * @param partOne
     * @param partTwo
     * @param itemsOne
     * @param itemsTwo
     * @return boolean
     */
    private boolean equalsPart(ApPart partOne, ApPart partTwo, List<ApItem> itemsOne, List<ApItem> itemsTwo) {
        if (!partOne.getPartTypeId().equals(partTwo.getPartTypeId())) {
            return false;
        }
        if (itemsOne.size() != itemsTwo.size()) {
            return false;
        }
        for (ApItem itemOne : itemsOne) {
            if (!isApItemInList(itemOne, itemsTwo)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Kontrola, zda seznam ApItems obsahuje prvek ApItem
     * 
     * @param item
     * @param items
     * @return boolean
     */
    private boolean isApItemInList(ApItem item, List<ApItem> items) {
        for (ApItem i : items) {
            if (Objects.equals(item.getItemTypeId(), i.getItemTypeId())
                    && Objects.equals(item.getItemSpecId(), i.getItemSpecId())
                    && item.getData().isEqualValue(i.getData())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vytvoření kopie ApPart která patří k danému ApAccessPoint
     * 
     * @param part zdroj ke kopírování
     * @param accessPoint
     * @param mapParent
     * @param change
     * @return ApPart
     */
    private ApPart copyPart(ApPart part, ApAccessPoint accessPoint, ApPart parent, ApChange change) {
        ApPart partTo = new ApPart();
        partTo.setAccessPoint(accessPoint);
        partTo.setCreateChange(change);
        partTo.setLastChange(change);
        partTo.setKeyValue(null);
        partTo.setParentPart(parent);
        partTo.setPartType(part.getPartType());
        partTo.setState(part.getState());
        partTo = partRepository.save(partTo);        

        return partTo;
    }

    /**
     * Vytvoření kopie ApPart spolu s ApItems
     * 
     * @param part
     * @param accessPoint
     * @param parent
     * @param itemsFrom
     * @param change
     * @return ApPart
     */
    private ApPart copyPartWithItems(ApPart part, ApAccessPoint accessPoint, ApPart parent, List<ApItem> itemsFrom, ApChange change) {
        ApPart partTo = copyPart(part, accessPoint, parent, change);
        copyItems(itemsFrom, partTo, change);
        return partTo;
    }

    /**
     * Kopírování všech položek ApItem
     * 
     * @param itemsFrom prvky původní part
     * @param partTo
     * @param change
     */
    private void copyItems(List<ApItem> itemsFrom, ApPart partTo, ApChange change) {
        for (ApItem item : itemsFrom) {
            ArrData newData = ArrData.makeCopyWithoutId(item.getData());

            ApItem newItem = apItemService.createItem(partTo, newData, 
                    item.getItemType(), 
                    item.getItemSpec(), 
                    change, 
                    apItemService.nextItemObjectId(),
                    item.getPosition());

            dataRepository.save(newData);
            itemRepository.save(newItem);
        }
    }

    /**
     * Sloučení ApItem z dva Party. Kopírují se jen položky, které nemají duplicitní hodnoty
     * 
     * @param itemsFrom prvky původní part
     * @param toPart
     * @param change
     */
    private void mergeItems(List<ApItem> itemsFrom, ApPart partTo, List<ApItem> itemsTo, ApChange change) {
        int position = 0;
        for (ApItem item : itemsFrom) {
            if (item.getPosition() > position) {
                position = item.getPosition();
            }
        }

        for (ApItem item : itemsFrom) {
            if (!existsItemInPart(item, itemsTo)) {
                ArrData newData = ArrData.makeCopyWithoutId(item.getData());

                ApItem newItem = apItemService.createItem(partTo, newData, 
                    item.getItemType(), 
                    item.getItemSpec(), 
                    change, 
                    apItemService.nextItemObjectId(),
                    ++position);

                dataRepository.save(newData);
                itemRepository.save(newItem);
            }
        }
    }

    /**
     * Existuje ApItem v ApPart se stejným typem a se stejnou hodnotou?
     * 
     * @param item
     * @param itemsTo
     * @return true pokud takový ApItem již existuje
     */
    private boolean existsItemInPart(ApItem apItem, List<ApItem> itemsTo) {
        if (!CollectionUtils.isEmpty(itemsTo)) {
            for (ApItem item : itemsTo) {
                if (Objects.equals(apItem.getItemTypeId(), item.getItemTypeId())
                        && Objects.equals(apItem.getItemSpecId(), item.getItemSpecId())
                        && apItem.getData().isEqualValue(item.getData())) {
                    return true;
                }
            }
        }
        return false;
    }
}
