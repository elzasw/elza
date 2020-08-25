package cz.tacr.elza.service;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import cz.tacr.cam.schema.cam.BatchEntityRecordRevXml;
import cz.tacr.cam.schema.cam.BatchInfoXml;
import cz.tacr.cam.schema.cam.BatchUpdateErrorXml;
import cz.tacr.cam.schema.cam.BatchUpdateResultXml;
import cz.tacr.cam.schema.cam.BatchUpdateSavedXml;
import cz.tacr.cam.schema.cam.BatchUpdateXml;
import cz.tacr.cam.schema.cam.EntityRecordStateXml;
import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.cam.schema.cam.ErrorMessageXml;
import cz.tacr.cam.schema.cam.LongStringXml;
import cz.tacr.cam.schema.cam.PartXml;
import cz.tacr.cam.schema.cam.UuidXml;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.controller.factory.SearchFilterFactory;
import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.ArchiveEntityResultListVO;
import cz.tacr.elza.controller.vo.FileType;
import cz.tacr.elza.controller.vo.SearchFilterVO;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.ap.item.ApUpdateItemVO;
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
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApExternalSystem;
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
import cz.tacr.elza.domain.SyncState;
import cz.tacr.elza.domain.SysLanguage;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ExceptionUtils;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.ExternalCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.groovy.GroovyResult;
import cz.tacr.elza.packageimport.xml.SettingRecord;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FundRegisterScopeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemAptypeRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.ScopeRelationRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.SysLanguageRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.cam.CamHelper;
import cz.tacr.elza.service.cam.CreateEntityBuilder;
import cz.tacr.elza.service.cam.ProcessingContext;
import cz.tacr.elza.service.cam.SyncEntityRequest;
import cz.tacr.elza.service.cam.UpdateEntityBuilder;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.vo.DataRef;


/**
 * Servisní třída pro registry.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
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
    private UserService userService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private ArrangementCacheService arrangementCacheService;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private ApChangeRepository apChangeRepository;

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
    private AccessPointGeneratorService apGeneratorService;

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

        Set<Integer> scopeIdsForSearch = getScopeIdsForSearch(fund, scopeId);

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

        Set<Integer> scopeIdsForSearch = getScopeIdsForSearch(fund, scopeId);

        return apAccessPointRepository.findApAccessPointByTextAndTypeCount(searchRecord, apTypeIds, scopeIdsForSearch, approvalStates, searchTypeName, searchTypeUsername);
    }

    /**
     * Kontrola, jestli je používán přístupový bod v navázaných tabulkách.
     *
     * @param accessPoint přístupový bod
     * @throws BusinessException napojení na jinou tabulku
     */
    public void checkDeletion(final ApAccessPoint accessPoint) {
        // arr_data_record_ref
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
    public void deleteAccessPoint(@AuthParam(type = AuthParam.Type.AP_STATE) final ApState apState, final boolean checkUsage) {

        apDataService.validationNotDeleted(apState);

        ApAccessPoint accessPoint = apState.getAccessPoint();
        if (accessPoint.getState() == ApStateEnum.TEMP) {
            removeTempAccessPoint(accessPoint);
        } else {

            if (checkUsage) {
                checkDeletion(accessPoint);
            }

            ApChange change = apDataService.createChange(ApChange.Type.AP_DELETE);
            apState.setDeleteChange(change);
            apStateRepository.save(apState);

            saveWithLock(accessPoint);

            List<ApBindingState> eids = bindingStateRepository.findByAccessPoint(accessPoint);
            eids.forEach(eid -> eid.setDeleteChange(change));
            bindingStateRepository.saveAll(eids);

            publishAccessPointDeleteEvent(accessPoint);
            reindexDescItem(accessPoint);
        }
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
     * Načte seznam id tříd ve kterých se má vyhledávat. Výsledek je průnikem tříd požadovaných a těch na které ma uživatel právo.
     *
     * @param fund AP, podle jejíž tříd se má hledat
     * @param scopeId id scope, pokud je vyplněno hledá se jen v tomto scope
     * @return množina id tříd, podle kterých se bude hledat
     */
    public Set<Integer> getScopeIdsForSearch(@Nullable final ArrFund fund, @Nullable final Integer scopeId) {
        boolean readAllScopes = userService.hasPermission(UsrPermission.Permission.AP_SCOPE_RD_ALL);
        UsrUser user = userService.getLoggedUser();

        Set<Integer> scopeIdsToSearch;
        if (readAllScopes || user == null) {
            scopeIdsToSearch = scopeRepository.findAllIds();
        } else {
            scopeIdsToSearch = userService.getUserScopeIds();
        }

        if (!scopeIdsToSearch.isEmpty()) {
            if (fund != null) {
                Set<Integer> fundScopeIds = scopeRepository.findIdsByFund(fund);
                scopeIdsToSearch.retainAll(fundScopeIds);
            }

            if (scopeId != null) {
                scopeIdsToSearch.removeIf(id -> !id.equals(scopeId));
            }
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

    public List<String> getScopeCodes() {
        if (scopeCodes == null) {
            SettingRecord setting = settingsService.readSettings(UISettings.SettingsType.RECORD.toString(),
                    null,
                    SettingRecord.class);
            if (setting != null) {
                List<SettingRecord.ScopeCode> scopeCodes = setting.getScopeCodes();
                if (CollectionUtils.isEmpty(scopeCodes)) {
                    this.scopeCodes = new ArrayList<>();
                } else {
                    this.scopeCodes = scopeCodes.stream().map(SettingRecord.ScopeCode::getValue).collect(toList());
                }
            }
        }
        return scopeCodes;
    }

    public List<ApScope> findDefaultScopes() {
        List<String> scopeCodes = getScopeCodes();
        List<ApScope> defaultScopes;
        if (CollectionUtils.isEmpty(scopeCodes)) {
            defaultScopes = Collections.emptyList();
        } else {
            defaultScopes = scopeRepository.findByCodes(scopeCodes);
        }

        return defaultScopes;
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
            fundVersions = arrangementService.getOpenVersionsByFundIds(fundsAll).stream()
                    .collect(toMap(ArrFundVersion::getFundId, Function.identity()));
        }

        final ArrChange change = arrangementService.createChange(ArrChange.Type.REPLACE_REGISTER);
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
        ApState apState = createAccessPoint(scope, type, apChange, null);
        ApAccessPoint accessPoint = apState.getAccessPoint();

        ApPart apPart = partService.createPart(partType, accessPoint, apChange, null);
        accessPoint.setPreferredPart(apPart);

        partService.createPartItems(apChange, apPart, apPartFormVO, null, null);
        updatePartValue(apPart);

        publishAccessPointCreateEvent(accessPoint);

        return apState;
    }

    public List<ApState> createAccessPoints(final ProcessingContext procCtx,
                                            final List<EntityXml> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }
        ApExternalSystem apExternalSystem = procCtx.getApExternalSystem();

        List<ApState> states = new ArrayList<>();

        Function<EntityXml, String> idGetter;

        // prepare list of already used ids
        Map<String, EntityXml> uuids = CamHelper.getEntitiesByUuid(entities);
        List<ApAccessPoint> existingAps = apAccessPointRepository.findApAccessPointsByUuids(uuids.keySet());
        Set<String> usedUuids = existingAps.stream().map(ApAccessPoint::getUuid).collect(Collectors.toSet());

        // Read existing binding from DB
        List<String> values;
        if (apExternalSystem.getType() == ApExternalSystemType.CAM) {
            values = CamHelper.getEids(entities);
            idGetter = CamHelper::getEntityId;
        } else if (apExternalSystem.getType() == ApExternalSystemType.CAM_UUID) {
            values = CamHelper.getEuids(entities);
            idGetter = CamHelper::getEntityUuid;
        } else {
            throw new IllegalStateException("Unkonw external system type: " + apExternalSystem.getType());
        }
        List<ApBinding> bindingList = bindingRepository.findByScopeAndValuesAndExternalSystem(procCtx.getScope(),
                                                                                              values,
                                                                                              apExternalSystem);
        procCtx.addBindings(bindingList);

        // get list of connected records
        List<ArrDataRecordRef> dataRecordRefList = dataRecordRefRepository.findByBindingIn(bindingList);

        for (EntityXml entity : entities) {
            String bindingValue = idGetter.apply(entity);

            ApBinding binding = procCtx.getBindingByValue(bindingValue);
            if (binding == null) {
                binding = externalSystemService.createApBinding(procCtx.getScope(), bindingValue, apExternalSystem);
                procCtx.addBinding(binding);
            }

            // prepare uuid
            String srcUuid = CamHelper.getEntityUuid(entity);
            String apUuid = usedUuids.contains(srcUuid) ? null : srcUuid;

            ApState state = createAccessPoint(procCtx, entity, binding, apUuid);
            states.add(state);
            setAccessPointInDataRecordRefs(state.getAccessPoint(), dataRecordRefList, binding);
        }
        dataRecordRefRepository.saveAll(dataRecordRefList);
        List<ApPart> partList = itemRepository.findPartsByDataRecordRefList(dataRecordRefList);
        if (CollectionUtils.isNotEmpty(partList)) {
            for (ApPart part : partList) {
                updatePartValue(part);
            }
        }

        return states;
    }

    private void setAccessPointInDataRecordRefs(ApAccessPoint accessPoint, List<ArrDataRecordRef> dataRecordRefList, ApBinding binding) {
        if (CollectionUtils.isNotEmpty(dataRecordRefList)) {
            for (ArrDataRecordRef dataRecordRef : dataRecordRefList) {
                if (dataRecordRef.getBinding().getBindingId().equals(binding.getBindingId())) {
                    dataRecordRef.setRecord(accessPoint);
                }
            }
        }
    }

    public ApState createAccessPoint(final ProcessingContext procCtx,
                                     final EntityXml entity,
                                     ApBinding binding,
                                     String uuid) {
        Validate.notNull(procCtx, "Context cannot be null");

        StaticDataProvider sdp = staticDataService.getData();

        ApType type = sdp.getApTypeByCode(entity.getEnt().getValue());
        ApChange apChange = apDataService.createChange(ApChange.Type.AP_CREATE);
        ApState apState = createAccessPoint(procCtx.getScope(), type, apChange, uuid);
        ApAccessPoint accessPoint = apState.getAccessPoint();

        createPartsFromEntityXml(procCtx, entity, accessPoint, apChange, sdp, apState, binding);
        partService.validationNameUnique(apState.getScope(), accessPoint.getPreferredPart().getValue());

        publishAccessPointCreateEvent(accessPoint);

        return apState;
    }

    public void connectAccessPoint(final ApState state, final EntityXml entity,
                                   final ProcessingContext procCtx, final boolean replace) {
        StaticDataProvider sdp = staticDataService.getData();
        ApAccessPoint accessPoint = state.getAccessPoint();
        ApChange apChange = apDataService.createChange(ApChange.Type.AP_UPDATE);
        ApType type = sdp.getApTypeByCode(entity.getEnt().getValue());

        state.setDeleteChange(apChange);
        stateRepository.save(state);
        ApState stateNew = copyState(state, apChange);
        stateNew.setApType(type);
        stateNew.setStateApproval(StateApproval.NEW);
        stateRepository.save(stateNew);

        if (replace) {
            partService.deleteParts(accessPoint, apChange);
        }


        ApBinding binding = externalSystemService.createApBinding(procCtx.getScope(),
                                                                Long.toString(entity.getEid().getValue()),
                                                                procCtx.getApExternalSystem());

        createPartsFromEntityXml(procCtx, entity, accessPoint, apChange, sdp, stateNew, binding);
        partService.validationNameUnique(procCtx.getScope(), accessPoint.getPreferredPart().getValue());

        publishAccessPointUpdateEvent(accessPoint);
    }

    private void createPartsFromEntityXml(final ProcessingContext procCtx,
                                   final EntityXml entity,
                                   final ApAccessPoint accessPoint,
                                   final ApChange apChange,
                                   final StaticDataProvider sdp,
                                   final ApState apState,
                                   final ApBinding binding) {
        Validate.notNull(binding);

        externalSystemService.createApBindingState(binding, accessPoint, apChange,
                                                       entity.getEns().value(), entity.getRevi().getRid().getValue(),
                                                       entity.getRevi().getUsr().getValue(),
                                                       entity.getReid() != null ? entity.getReid().getValue() : null);
        List<ApPart> partList = new ArrayList<>();
        Map<Integer, List<ApItem>> itemMap = new HashMap<>();

        List<DataRef> dataRefList = new ArrayList<>();

        for (PartXml part : entity.getPrts().getList()) {
            RulPartType partType = sdp.getPartTypeByCode(part.getT().value());
            ApPart parentPart = part.getPrnt() != null ? findParentPart(binding, part.getPrnt().getValue()) : null;

            ApPart apPart = partService.createPart(partType, accessPoint, apChange, parentPart);
            externalSystemService.createApBindingItem(binding, part.getPid().getValue(), apPart, null);

            List<ApItem> itemList = partService.createPartItems(apChange, apPart, part.getItms().getItems(), binding, dataRefList);

            itemMap.put(apPart.getPartId(), itemList);
            partList.add(apPart);
        }

        createBindingForRel(dataRefList, binding, procCtx);

        accessPoint.setPreferredPart(findPreferredPart(partList));

        updatePartValues(apState, partList, itemMap);
    }

    /**
     * Vytvoreni novych propojeni (binding)
     *
     * @param dataRefList
     * @param binding
     * @param procCtx
     */
    private void createBindingForRel(final List<DataRef> dataRefList, final ApBinding binding,
                                     final ProcessingContext procCtx) {
        //TODO fantiš optimalizovat
        for (DataRef dataRef : dataRefList) {
            // Tento kod je divny, co to dela?
            ApBindingItem apBindingItem = externalSystemService.findByBindingAndUuid(binding, dataRef.getUuid());
            // Co kdyz vazba neni nalezena?
            if (apBindingItem.getItem() != null) {
                ArrDataRecordRef dataRecordRef = (ArrDataRecordRef) apBindingItem.getItem().getData();
                ApBinding refBinding = externalSystemService.findByScopeAndValueAndApExternalSystem(procCtx.getScope(),
                                                                                                    dataRef.getValue(),
                                                                                                    procCtx.getApExternalSystem());
                if (refBinding == null) {
                    // check if not in the processing context
                    refBinding = procCtx.getBindingByValue(dataRef.getValue());
                    if (refBinding == null) {
                        // we can create new - last resort
                        refBinding = externalSystemService.createApBinding(procCtx.getScope(),
                                                                           dataRef.getValue(),
                                                                           procCtx.getApExternalSystem());
                        procCtx.addBinding(refBinding);
                    }
                } else {
                    // proc se dela toto?
                    ApBindingState bindingState = externalSystemService.findByBinding(refBinding);
                    if (bindingState != null) {
                        dataRecordRef.setRecord(bindingState.getAccessPoint());
                    }
                }
                dataRecordRef.setBinding(refBinding);
                dataRecordRefRepository.save(dataRecordRef);
            }
        }
    }

    /**
     * Aktualizace stavajicich propojeni
     *
     * @param dataRefList
     * @param bindingItemList
     */
    private void createBindingForRel(final List<DataRef> dataRefList, final List<ApBindingItem> bindingItemList) {
        //TODO fantiš optimalizovat
        for (DataRef dataRef : dataRefList) {
            ApBindingItem apBindingItem = findBindingItemByUuid(bindingItemList, dataRef.getUuid());
            if (apBindingItem != null && apBindingItem.getItem() != null) {
                ArrDataRecordRef dataRecordRef = (ArrDataRecordRef) apBindingItem.getItem().getData();
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
    }

    private ApPart findParentPart(final ApBinding binding, final String parentUuid) {
        ApBindingItem apBindingItem = externalSystemService.findByBindingAndUuid(binding, parentUuid);
        return apBindingItem.getPart();
    }

    public void updatePart(final ApAccessPoint apAccessPoint,
                           final ApPart apPart,
                           final ApPartFormVO apPartFormVO) {
//        if (areItemsChanged(apPart, apPartFormVO)) {
            ApChange change = apDataService.createChange(ApChange.Type.AP_UPDATE);
            List<ApItem> itemList = itemRepository.findValidItemsByPart(apPart);
            List<ApBindingItem> bindingItemList = bindingItemRepository.findByItems(itemList);

            apItemService.deletePartItems(apPart, change);
            partService.deletePart(apPart, change);

            ApPart newPart = partService.createPart(apPart, change);
            changeBindingItemParts(apPart, newPart);

            List<DataRef> dataRefList = new ArrayList<>();

            partService.createPartItems(change, newPart, apPartFormVO, bindingItemList, dataRefList);
            createBindingForRel(dataRefList, bindingItemList);

            partService.changeParentPart(apPart, newPart);

            if (apAccessPoint.getPreferredPart().getPartId().equals(apPart.getPartId())) {
                apAccessPoint.setPreferredPart(newPart);
                saveWithLock(apAccessPoint);
            }
            updatePartValue(newPart);
//        }
    }

    private void changeBindingItemParts(ApPart oldPart, ApPart newPart) {
        List<ApBindingItem> bindingItemList = bindingItemRepository.findByPart(oldPart);
        if (CollectionUtils.isNotEmpty(bindingItemList)) {
            for (ApBindingItem bindingItem : bindingItemList) {
                bindingItem.setPart(newPart);
            }
        }
    }

    private ApPart findPreferredPart(final List<ApPart> partList) {
        StaticDataProvider sdp = StaticDataProvider.getInstance();
        RulPartType defaultPartType = sdp.getDefaultPartType();
        for (ApPart part : partList) {
            if (part.getPartType().getCode().equals(defaultPartType.getCode())) {
                return part;
            }
        }
        return null;
    }

    private void updatePartValues(final ApState state,
                                  final List<ApPart> partList,
                                  final Map<Integer, List<ApItem>> itemMap) {
        for (ApPart part : partList) {
            List<ApPart> childrenParts = findChildrenParts(part, partList);
            List<ApItem> items = getItemsForParts(part, childrenParts, itemMap);

            GroovyResult result = groovyService.processGroovy(state, part, childrenParts, items);
            partService.updatePartValue(part, result);
        }
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
        List<ApItem> itemList = new ArrayList<>(itemMap.get(part.getPartId()));

        for (ApPart p : childrenParts) {
            itemList.addAll(itemMap.get(p.getPartId()));
        }

        return itemList;
    }

    public void updatePartValues(final Collection<PartWrapper> partWrappers) {
        for (PartWrapper partWrapper : partWrappers) {
            ApPart apPart = partWrapper.getEntity();
            ApState state = partWrapper.getPartInfo().getApInfo().getApState();

            List<PartWrapper> childrenPartWrappers = findChildrenParts(apPart, partWrappers);
            List<ApPart> childrenParts = getChildrenParts(childrenPartWrappers);

            List<ApItem> items = getItemsFromPartWrappers(partWrapper, childrenPartWrappers);

            GroovyResult result = groovyService.processGroovy(state, apPart, childrenParts, items);

            partService.updatePartValue(apPart, result);
        }
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

    public void updatePartValue(final ApPart apPart) {
        ApState state = getState(apPart.getAccessPoint());
        List<ApPart> childrenParts = partService.findPartsByParentPart(apPart);

        List<ApPart> parts = new ArrayList<>();
        parts.add(apPart);
        parts.addAll(childrenParts);

        List<ApItem> items = apItemService.findItemsByParts(parts);

        GroovyResult result = groovyService.processGroovy(state, apPart, childrenParts, items);

        partService.updatePartValue(apPart, result);
    }

    public void updateBindingAfterSave(BatchUpdateResultXml batchUpdateResult, Integer accessPointId, String externalSystemCode) {
        ApAccessPoint accessPoint = getAccessPoint(accessPointId);
        ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);
        ApBindingState bindingState = bindingStateRepository.findByAccessPointAndExternalSystem(accessPoint, apExternalSystem);
        ApBinding binding = bindingState.getBinding();

        if (batchUpdateResult instanceof BatchUpdateSavedXml) {
            BatchUpdateSavedXml batchUpdateSaved = (BatchUpdateSavedXml) batchUpdateResult;
            BatchEntityRecordRevXml batchEntityRecordRev = batchUpdateSaved.getRevisions().get(0);

            bindingState.setExtRevision(batchEntityRecordRev.getRev().getValue());
            binding.setValue(String.valueOf(batchEntityRecordRev.getEid().getValue()));

            bindingStateRepository.save(bindingState);
            bindingRepository.save(binding);
        } else {
            BatchUpdateErrorXml batchUpdateErrorXml = (BatchUpdateErrorXml) batchUpdateResult;
            bindingItemRepository.deleteByBinding(binding);
            bindingStateRepository.delete(bindingState);
            bindingRepository.delete(binding);

            if (CollectionUtils.isNotEmpty(batchUpdateErrorXml.getMessages())) {
                StringBuilder message = new StringBuilder();
                for (ErrorMessageXml errorMessage : batchUpdateErrorXml.getMessages()) {
                    message.append(errorMessage.getMsg().getValue()).append("\n");
                }
                throw new SystemException(message.toString(), ExternalCode.EXTERNAL_SYSTEM_ERROR);
            }
        }
    }

    public void updateBindingAfterUpdate(BatchUpdateResultXml batchUpdateResult, ApAccessPoint accessPoint, ApExternalSystem apExternalSystem) {
        ApBindingState bindingState = bindingStateRepository.findByAccessPointAndExternalSystem(accessPoint, apExternalSystem);
        ApBinding binding = bindingState.getBinding();

        if (batchUpdateResult instanceof BatchUpdateSavedXml) {
            BatchUpdateSavedXml batchUpdateSaved = (BatchUpdateSavedXml) batchUpdateResult;
            BatchEntityRecordRevXml batchEntityRecordRev = batchUpdateSaved.getRevisions().get(0);

            ApChange change = apDataService.createChange(ApChange.Type.AP_UPDATE);
            externalSystemService.createNewApBindingState(bindingState, change, batchEntityRecordRev.getRev().getValue());
        } else {
            BatchUpdateErrorXml batchUpdateErrorXml = (BatchUpdateErrorXml) batchUpdateResult;
            List<ApBindingItem> bindingItemList = bindingItemRepository.findByBinding(binding);

            if (CollectionUtils.isNotEmpty(bindingItemList)) {
                for (ApBindingItem bindingItem : bindingItemList) {
                    if ((bindingItem.getPart() != null && bindingItem.getPart().getCreateChange().getChangeId() <= bindingState.getSyncChange().getChangeId()) ||
                        bindingItem.getItem() != null && bindingItem.getItem().getCreateChange().getChangeId() <= bindingState.getSyncChange().getChangeId()) {
                        bindingItemList.remove(bindingItem);
                    }
                }
                if (CollectionUtils.isNotEmpty(bindingItemList)) {
                    bindingItemRepository.deleteAll(bindingItemList);
                }
            }

            if (CollectionUtils.isNotEmpty(batchUpdateErrorXml.getMessages())) {
                StringBuilder message = new StringBuilder();
                for (ErrorMessageXml errorMessage : batchUpdateErrorXml.getMessages()) {
                    message.append(errorMessage.getMsg().getValue()).append("\n");
                }
                throw new SystemException(message.toString(), ExternalCode.EXTERNAL_SYSTEM_ERROR);
            }
        }
    }

    public BatchUpdateXml createCreateEntityBatchUpdate(final Integer accessPointId, final String externalSystemCode) {
        ApAccessPoint accessPoint = getAccessPoint(accessPointId);
        ApState state = getState(accessPoint);
        ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);
        ApChange change = apDataService.createChange(ApChange.Type.AP_CREATE);
        UserDetail userDetail = userService.getLoggedUserDetail();

        List<ApPart> partList = partService.findPartsByAccessPoint(state.getAccessPoint());
        Map<Integer, List<ApItem>> itemMap = itemRepository.findValidItemsByAccessPoint(accessPoint).stream()
                .collect(Collectors.groupingBy(i -> i.getPartId()));

        ApBinding binding = externalSystemService.createApBinding(state.getScope(), null, apExternalSystem);
        ApBindingState bindingState = externalSystemService.createApBindingState(binding, accessPoint, change,
                EntityRecordStateXml.ERS_NEW.value(), null, userDetail.getUsername(), null);

        BatchUpdateXml batchUpdate = new BatchUpdateXml();
        batchUpdate.setInf(createBatchInfo(userDetail));
        CreateEntityBuilder ceb = new CreateEntityBuilder(this.externalSystemService,
                this.staticDataService.getData(),
                accessPoint, binding, state);
        batchUpdate.getChanges().add(ceb.build(partList, itemMap));
        return batchUpdate;
    }

    public BatchUpdateXml createUpdateEntityBatchUpdate(final ApAccessPoint accessPoint,
                                                        final ApBindingState bindingState,
                                                        final EntityXml entityXml) {
        ApState state = getState(accessPoint);
        UserDetail userDetail = userService.getLoggedUserDetail();

        List<ApPart> partList = partService.findPartsByAccessPoint(state.getAccessPoint());
        Map<Integer, List<ApItem>> itemMap = itemRepository.findValidItemsByAccessPoint(accessPoint).stream()
                .collect(Collectors.groupingBy(i -> i.getPartId()));
        List<ApBindingItem> bindingParts = bindingItemRepository.findPartsByBinding(bindingState.getBinding());

        BatchUpdateXml batchUpdate = new BatchUpdateXml();
        batchUpdate.setInf(createBatchInfo(userDetail));

        UpdateEntityBuilder ueb = new UpdateEntityBuilder(this.externalSystemService,
                this.bindingItemRepository,
                this.staticDataService.getData(),
                state,
                bindingState);

        ueb.build(batchUpdate.getChanges(), entityXml, partList, itemMap, bindingParts);
        return batchUpdate;
    }

    private BatchInfoXml createBatchInfo(UserDetail userDetail) {
        BatchInfoXml batchInfo = new BatchInfoXml();
        batchInfo.setBatchUserInfo(new LongStringXml(userDetail.getUsername()));
        batchInfo.setBid(new UuidXml(UUID.randomUUID().toString()));
        return batchInfo;
    }

    public void synchronizeAccessPoint(ProcessingContext procCtx, ApState state, EntityXml entity,
                                       ApBindingState bindingState, boolean syncQueue) {
        //TODO fantiš other external systems
        if (syncQueue && checkLocalChanges(state, bindingState)) {
            bindingState.setSyncOk(SyncState.NOT_SYNCED);
            bindingStateRepository.save(bindingState);
        } else {
            StaticDataProvider sdp = staticDataService.getData();
            ApBinding binding = bindingState.getBinding();
            ApAccessPoint accessPoint = state.getAccessPoint();
            ApChange apChange = apDataService.createChange(ApChange.Type.AP_UPDATE);

            List<ApBindingItem> bindingParts = bindingItemRepository.findPartsByBinding(binding);
            List<ApBindingItem> newBindingParts = new ArrayList<>();
            Map<Integer, List<ApBindingItem>> bindingItemMap = bindingItemRepository.findItemsByBinding(binding).stream()
                    .collect(Collectors.groupingBy(i -> i.getItem().getPartId()));

            externalSystemService.createNewApBindingState(bindingState, apChange, entity.getEns().value(),
                    entity.getRevi().getRid().getValue(),  entity.getRevi().getUsr().getValue(),
                    entity.getReid() != null ? entity.getReid().getValue() : null);

            List<ApPart> partList = new ArrayList<>();
            Map<Integer, List<ApItem>> itemMap = new HashMap<>();

            List<DataRef> dataRefList = new ArrayList<>();

            for (PartXml part : entity.getPrts().getList()) {
                ApBindingItem bindingItem = findBindingItemByUuid(bindingParts, part.getPid().getValue());
                if (bindingItem != null) {
                    List<ApBindingItem> bindingItems = bindingItemMap.getOrDefault(bindingItem.getPart().getPartId(), new ArrayList<>());
                    List<ApBindingItem> notChangeItems = new ArrayList<>();
                    List<Object> newItems = apItemService.findNewOrChangedItems(part.getItms().getItems(), bindingItems, notChangeItems);

                    if (CollectionUtils.isNotEmpty(newItems) || CollectionUtils.isNotEmpty(bindingItems)) {
                        //nové nebo změněné itemy z externího systému
                        deleteChangedOrRemovedItems(bindingItems, apChange);

                        ApPart oldPart = bindingItem.getPart();
                        partService.deletePart(oldPart, apChange);
                        ApPart apPart = partService.createPart(oldPart, apChange);
                        bindingItem.setPart(apPart);
                        bindingItemRepository.save(bindingItem);

                        changePartInItems(apPart, notChangeItems, apChange);
                        changePartInItems(apPart, apChange, oldPart);

                        partService.createPartItems(apChange, apPart, newItems, binding, dataRefList);

                        itemMap.put(apPart.getPartId(), itemRepository.findValidItemsByPart(apPart));
                        partList.add(apPart);
                    }
                    newBindingParts.add(bindingItem);
                    bindingParts.remove(bindingItem);
                } else {
                    //nový part v externím systému
                    RulPartType partType = sdp.getPartTypeByCode(part.getT().value());
                    ApPart parentPart = part.getPrnt() != null ? findBindingItemByUuid(newBindingParts, part.getPrnt().getValue()).getPart() : null;

                    ApPart apPart = partService.createPart(partType, accessPoint, apChange, parentPart);
                    newBindingParts.add(externalSystemService.createApBindingItem(binding, part.getPid().getValue(), apPart, null));
                    List<ApItem> itemList = partService.createPartItems(apChange, apPart, part.getItms().getItems(), binding, dataRefList);

                    itemMap.put(apPart.getPartId(), itemList);
                    partList.add(apPart);
                }
            }
            deleteParts(bindingParts, apChange);

            createBindingForRel(dataRefList, binding, procCtx);

            accessPoint.setPreferredPart(findPreferredPart(entity.getPrts().getList(), newBindingParts));

            updatePartValues(state, partList, itemMap);
            partService.validationNameUnique(state.getScope(), accessPoint.getPreferredPart().getValue());
        }
    }

    private void deleteParts(List<ApBindingItem> bindingParts, ApChange apChange) {
        if (CollectionUtils.isNotEmpty(bindingParts)) {
            List<ApPart> partList = new ArrayList<>();
            for (ApBindingItem bindingItem : bindingParts) {
                partList.add(bindingItem.getPart());
            }
            apItemService.deletePartsItems(partList, apChange);
            partService.deleteParts(partList, apChange);

            bindingItemRepository.deleteAll(bindingParts);
        }
    }

    private ApPart findPreferredPart(List<PartXml> partList, List<ApBindingItem> bindingParts) {
        StaticDataProvider sdp = StaticDataProvider.getInstance();
        RulPartType defaultPartType = sdp.getDefaultPartType();
        for (PartXml part : partList) {
            if (part.getT().value().equals(defaultPartType.getCode())) {
                ApBindingItem bindingPart = findBindingItemByUuid(bindingParts, part.getPid().getValue());
                if (bindingPart != null) {
                    return bindingPart.getPart();
                }
            }
        }
        return null;
    }

    private void changePartInItems(ApPart apPart, List<ApBindingItem> notChangeItems, ApChange apChange) {
        if (CollectionUtils.isNotEmpty(notChangeItems)) {
            List<ApItem> itemList = new ArrayList<>();
            for (ApBindingItem bindingItem : notChangeItems) {
                ApItem item = bindingItem.getItem();
                item.setDeleteChange(apChange);
                itemList.add(item);

                ApItem newItem = apItemService.createItem(item, apChange, apPart);
                itemList.add(newItem);

                bindingItem.setItem(newItem);
            }
            bindingItemRepository.saveAll(notChangeItems);
            itemRepository.saveAll(itemList);
        }
    }

    private void changePartInItems(ApPart apPart, ApChange apChange, ApPart oldPart) {
        List<ApItem> notConnectedItems = itemRepository.findValidItemsByPart(oldPart);
        if (CollectionUtils.isNotEmpty(notConnectedItems)) {
            List<ApItem> itemList = new ArrayList<>();
            for (ApItem item : notConnectedItems) {
                item.setDeleteChange(apChange);
                itemList.add(item);

                ApItem newItem = apItemService.createItem(item, apChange, apPart);
                itemList.add(newItem);
            }
            itemRepository.saveAll(itemList);
        }
    }

    private void deleteChangedOrRemovedItems(List<ApBindingItem> bindingItemsInPart, ApChange apChange) {
        if (CollectionUtils.isNotEmpty(bindingItemsInPart)) {
            List<ApItem> itemList = new ArrayList<>();
            for (ApBindingItem bindingItem : bindingItemsInPart) {
                ApItem item = bindingItem.getItem();
                item.setDeleteChange(apChange);
                itemList.add(item);
            }
            bindingItemRepository.deleteAll(bindingItemsInPart);
            itemRepository.saveAll(itemList);
        }
    }


    @Nullable
    private ApBindingItem findBindingItemByUuid(final List<ApBindingItem> bindingItemList, final String pid) {
        if (CollectionUtils.isNotEmpty(bindingItemList)) {
            for (ApBindingItem bindingItem : bindingItemList) {
                if (bindingItem.getValue().equals(pid)) {
                    return bindingItem;
                }
            }
        }
        return null;
    }

    private boolean checkLocalChanges(final ApState state, final ApBindingState bindingState) {
        //TODO fantiš možná není nutné koukat na party
        List<ApPart> partList = partService.findNewerPartsByAccessPoint(state.getAccessPoint(), bindingState.getSyncChange().getChangeId());
        if (CollectionUtils.isNotEmpty(partList)) {
            return CollectionUtils.isNotEmpty(bindingItemRepository.findByParts(partList));
        }
        List<ApItem> itemList = itemRepository.findNewerValidItemsByAccessPoint(state.getAccessPoint(), bindingState.getSyncChange().getChangeId());
        if (CollectionUtils.isNotEmpty(itemList)) {
            return CollectionUtils.isNotEmpty(bindingItemRepository.findByItems(itemList));
        }

        return false;
    }

    /**
     * Založení strukturovaného přístupového bodu.
     *
     * @param scope třída
     * @param type typ přístupového bodu
     * @param language jazyk hlavního jména
     * @return založený strukturovaný přístupový bod
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ApState createStructuredAccessPoint(@AuthParam(type = AuthParam.Type.SCOPE) final ApScope scope, final ApType type, final SysLanguage language) {
        Assert.notNull(scope, "Třída musí být vyplněna");
        Assert.notNull(type, "Typ musí být vyplněn");

        ApChange change = apDataService.createChange(ApChange.Type.AP_CREATE);
        ApState apState = createStrucuredAccessPoint(scope, type, change, null);

        // založení strukturovaného hlavního jména
        ApAccessPoint accessPoint = apState.getAccessPoint();
        reindexDescItem(accessPoint);
        return apState;
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
        ApState oldState = getState(accessPoint);

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
     * Změna atributů přístupového bodu.
     *
     * @param apState přístupový bod
     * @param items položky změny
     * @return nové položky, které ze vytvořili při změně
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public List<ApItem> changeApItems(@AuthParam(type = AuthParam.Type.AP_STATE) final ApState apState,
                                      final List<ApUpdateItemVO> items) {
        Validate.notNull(apState, "Přístupový bod musí být vyplněn");
        Validate.notEmpty(items, "Musí být alespoň jedna položka ke změně");

        apDataService.validationNotDeleted(apState);

        ApAccessPoint accessPoint = apState.getAccessPoint();
        List<ApItem> itemsDb = itemRepository.findValidItemsByAccessPoint(accessPoint);

        ApChange change;
        if (accessPoint.getState() == ApStateEnum.TEMP) {
            change = apState.getCreateChange();
        } else {
            change = apDataService.createChange(ApChange.Type.AP_UPDATE);
        }
        List<ApItem> itemsCreated = apItemService.changeItems(items, new ArrayList<>(itemsDb), change, (RulItemType it, RulItemSpec is, ApChange c, int objectId, int position)
                -> createApItem(accessPoint, it, is, c, objectId, position));

        //apGeneratorService.generateAndSetResult(accessPoint, change);
        apGeneratorService.generateAsyncAfterCommit(accessPoint.getAccessPointId(), change.getChangeId());

        return itemsCreated;
    }

    /**
     * Smazání hodnot ap podle typu.
     *
     * @param apState přístupový bod
     * @param itemType typ atributu
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void deleteApItemsByType(@AuthParam(type = AuthParam.Type.AP_STATE) final ApState apState,
                                    final RulItemType itemType) {
        Validate.notNull(apState, "Přístupový bod musí být vyplněn");
        Validate.notNull(itemType, "Typ musí být vyplněn");
        apDataService.validationNotDeleted(apState);

        ApAccessPoint accessPoint = apState.getAccessPoint();

        ApChange change;
        if (accessPoint.getState() == ApStateEnum.TEMP) {
            change = apState.getCreateChange();
        } else {
            change = apDataService.createChange(ApChange.Type.AP_UPDATE);
        }
        //TODO fantis
//        apItemService.deleteItemsByType(itemRepository, accessPoint, itemType, change);

        //apGeneratorService.generateAndSetResult(accessPoint, change);
        apGeneratorService.generateAsyncAfterCommit(accessPoint.getAccessPointId(), change.getChangeId());
    }

    /**
     * Potvrzení dočasného přístupového bodu a jeho převalidování.
     *
     * @param apState přístupový bod
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void confirmAccessPoint(@AuthParam(type = AuthParam.Type.AP_STATE) final ApState apState) {

        Validate.notNull(apState);
        apDataService.validationNotDeleted(apState);

        ApAccessPoint accessPoint = apState.getAccessPoint();
        if (accessPoint.getState() == ApStateEnum.TEMP) {
            accessPoint.setState(ApStateEnum.INIT);
            saveWithLock(accessPoint);

            //apGeneratorService.generateAndSetResult(accessPoint, accessPoint.getCreateChange());
            apGeneratorService.generateAsyncAfterCommit(accessPoint.getAccessPointId(), apState.getCreateChangeId());
        } else {
            throw new BusinessException("Nelze potvrdit přístupový bod, který není dočasný", BaseCode.INVALID_STATE);
        }
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

        ApChange change = apDataService.createChange(ApChange.Type.AP_UPDATE);
        //apGeneratorService.generateAndSetResult(accessPoint, change);
        apGeneratorService.generateAsyncAfterCommit(accessPoint.getAccessPointId(), change.getChangeId());
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
     * Získání třídy.
     *
     * @param scopeId identifikátor třídy
     * @return třída
     */
    public ApScope getScope(final Integer scopeId) {
        return scopeRepository.findById(scopeId)
                .orElseThrow(() -> new ObjectNotFoundException("Třída neexistuje", BaseCode.ID_NOT_EXIST).setId(scopeId));
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
     * Získání stavu přístupového bodu.
     *
     * @param accessPoint přístupový bod
     * @return stav přístupového bodu
     */
    public ApState getState(final ApAccessPoint accessPoint) {
        final ApState state = stateRepository.findLastByAccessPoint(accessPoint);
        if (state == null) {
            throw new ObjectNotFoundException("Stav pro přístupový bod neexistuje", BaseCode.INVALID_STATE)
                    .set("accessPointId", accessPoint.getAccessPointId());
        }
        return state;
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
     * Odstranění dočasných AP včetně návazných objektů.
     */
    public void removeTempAccessPoints() {
        apItemService.removeTempItems();
        List<ApState> states = apStateRepository.findTempStates();
        List<ApChange> changes = new ArrayList<>();
        Set<ApAccessPoint> aps = new HashSet<>();
        for (ApState state : states) {
            changes.add(state.getCreateChange());
            // Delete Change u temporary nejspis nebude
            if (state.getDeleteChange() != null) {
                changes.add(state.getDeleteChange());
            }
            aps.add(state.getAccessPoint());
        }
        if (!states.isEmpty()) {
            apStateRepository.deleteAll(states);
        }
        if (!changes.isEmpty()) {
            apChangeRepository.deleteAll(changes);
        }
        apAccessPointRepository.deleteAll(aps);
        apAccessPointRepository.removeTemp();
    }

    /**
     * Odstranění dočasných AP včetně návazných objektů u AP.
     */
    private void removeTempAccessPoint(final ApAccessPoint ap) {
        apItemService.removeTempItems(ap);
        List<ApState> states = apStateRepository.findAllByAccessPoint(ap);
        List<ApChange> changes = new ArrayList<>();
        for (ApState state : states) {
            changes.add(state.getCreateChange());
            // Delete Change u temporary nejspis nebude
            if (state.getDeleteChange() != null) {
                changes.add(state.getDeleteChange());
            }
        }
        if (!states.isEmpty()) {
            apStateRepository.deleteAll(states);
        }
        if (!changes.isEmpty()) {
            apChangeRepository.deleteAll(changes);
        }
        apAccessPointRepository.delete(ap);
    }

    /**
     * Založení přístupového bodu.
     *
     * @param scope třída
     * @param type typ
     * @param change změna
     * @return přístupový bod
     */
    private ApState createAccessPoint(final ApScope scope, final ApType type,
                                      final ApChange change,
                                      final String uuid) {
        ApAccessPoint accessPoint = createAccessPointEntity(scope, type, change, uuid);
        accessPoint.setState(ApStateEnum.OK);
        return createAccessPointState(saveWithLock(accessPoint), scope, type, change);
    }

    /**
     * Založení dočasného strukturovaného přístupového bodu.
     *
     * @param scope třída
     * @param type typ přístupového bodu
     * @param change změna
     * @return založený a uložený AP
     */
    private ApState createStrucuredAccessPoint(final ApScope scope, final ApType type, final ApChange change,
                                               final String uuid) {
        ApAccessPoint accessPoint = createAccessPointEntity(scope, type, change, uuid);
        accessPoint.setState(ApStateEnum.TEMP);
        return createAccessPointState(saveWithLock(accessPoint), scope, type, change);
    }

    private ApState createAccessPointState(ApAccessPoint ap, ApScope scope, ApType type, ApChange change) {
        ApState apState = new ApState();
        apState.setAccessPoint(ap);
        apState.setApType(type);
        apState.setScope(scope);
        apState.setStateApproval(StateApproval.NEW);
        // apState.setComment(comment);
        apState.setCreateChange(change);
        apState.setDeleteChange(null);
        return stateRepository.save(apState);
    }

    private ApState copyState(ApState oldState, ApChange change) {
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

    /**
     * Vytvoření entity přístupového bodu.
     *
     * @param scope
     *            třída
     * @param type
     *            typ přístupového bodu
     * @param change
     *            změna
     * @param uuid
     *            UUID
     * @return vytvořená entita AP
     */
    public static ApAccessPoint createAccessPointEntity(final ApScope scope, final ApType type, final ApChange change,
                                                        String uuid) {
        ApAccessPoint accessPoint = new ApAccessPoint();
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        accessPoint.setUuid(uuid);
        return accessPoint;
    }

    /**
     * Vytvoření entity hodnoty atributu přístupového bodu.
     *
     * @param accessPoint přístupový bod pro který atribut tvoříme
     * @param it typ atributu
     * @param is specifikace atribututu
     * @param c změna
     * @param objectId jednoznačný identifikátor položky (nemění se při odverzování)
     * @param position pozice
     * @return vytvořená položka
     */
    private ApItem createApItem(final ApAccessPoint accessPoint, final RulItemType it, final RulItemSpec is, final ApChange c, final int objectId, final int position) {
        ApItem item = new ApItem();
        item.setItemType(it);
        item.setItemSpec(is);
        item.setCreateChange(c);
        item.setObjectId(objectId);
        item.setPosition(position);
        return item;
    }

    private void publishAccessPointCreateEvent(final ApAccessPoint accessPoint) {
        publishAccessPointEvent(accessPoint, EventType.ACCESS_POINT_CREATE);
    }

    private void publishAccessPointUpdateEvent(final ApAccessPoint accessPoint) {
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

        ApState oldApState = getState(accessPoint);
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
        if (!hasApPermission(oldApScope, oldStateApproval, newStateApproval)) {
            throw new SystemException("Uživatel nemá oprávnění na změnu přístupového bodu", BaseCode.INSUFFICIENT_PERMISSIONS)
                    .set("accessPointId", accessPoint.getAccessPointId())
                    .set("scopeId", oldApScope.getScopeId());
        }

        ApScope newApScope;
        if (newScopeId != null && !newScopeId.equals(oldApScope.getScopeId())) {
            newApScope = getScope(newScopeId);
            if (!hasApPermission(newApScope, oldStateApproval, newStateApproval)) {
                throw new SystemException("Uživatel nemá oprávnění na změnu přístupového bodu", BaseCode.INSUFFICIENT_PERMISSIONS)
                        .set("accessPointId", accessPoint.getAccessPointId())
                        .set("scopeId", newApScope.getScopeId());
            }
            update = true;
        } else {
            newApScope = null;
        }

        ApType newApType;
        if (newTypeId != null && !newTypeId.equals(oldApState.getApTypeId())) {
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
        apGeneratorService.generateAsyncAfterCommit(accessPoint.getAccessPointId(), change.getChangeId());

        if (newApScope != null) {
            partService.validationNameUnique(newApScope, accessPoint.getPreferredPart().getValue());
        }

        publishAccessPointUpdateEvent(accessPoint);
        reindexDescItem(accessPoint);

        return newApState;
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

        if (oldStateApproval != null && oldStateApproval.equals(StateApproval.APPROVED) && newStateApproval.equals(StateApproval.APPROVED)) {

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
     * Nastaví část přístupového bodu na preferovanou
     *
     * @param accessPoint přístupový bod
     * @param apPart část
     */
    public void setPreferName(final ApAccessPoint accessPoint, final ApPart apPart) {
        StaticDataProvider sdp = StaticDataProvider.getInstance();
        RulPartType defaultPartType = sdp.getDefaultPartType();

        if (!apPart.getPartType().getCode().equals(defaultPartType.getCode())) {
            throw new IllegalArgumentException("Preferované jméno musí být typu " + defaultPartType.getCode());
        }

        if (apPart.getParentPart() != null) {
            throw new IllegalArgumentException("Návazný part nelze změnit na preferovaný.");
        }

        accessPoint.setPreferredPart(apPart);
        saveWithLock(accessPoint);
        updatePartValue(apPart);
    }

    public void checkUniqueBinding(ApScope scope, String archiveEntityId, String externalSystemCode) {
        ApBinding apBinding = externalSystemService.findByScopeAndValueAndApExternalSystem(scope, archiveEntityId, externalSystemCode);
        if (apBinding != null) {
            throw new IllegalArgumentException("Tato archivní entita již je v tomto scope.");
        }
    }

    public void disconnectAccessPoint(Integer accessPointId, String externalSystemCode) {
        ApAccessPoint accessPoint = getAccessPoint(accessPointId);
        ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);

        ApBindingState bindingState = bindingStateRepository.findByAccessPointAndExternalSystem(accessPoint, apExternalSystem);
        ApBinding binding = bindingState.getBinding();
        dataRecordRefRepository.disconnectBinding(binding);
        bindingItemRepository.deleteByBinding(binding);
        bindingStateRepository.delete(bindingState);
        bindingRepository.delete(binding);
    }

    public List<Integer> findRelArchiveEntities(ApAccessPoint accessPoint) {
        List<Integer> archiveEntityIds = new ArrayList<>();
        List<ApItem> itemList = itemRepository.findValidItemsByAccessPoint(accessPoint);

        for (ApItem item : itemList) {
            if (item.getData() instanceof ArrDataRecordRef) {
                ArrDataRecordRef dataRecordRef = (ArrDataRecordRef) item.getData();
                if (dataRecordRef.getRecord() == null) {
                    archiveEntityIds.add(Integer.parseInt(dataRecordRef.getBinding().getValue()));
                }
            }
        }
        return archiveEntityIds;
    }

    public void checkUniqueExtSystem(final ApAccessPoint accessPoint, final String externalSystemCode) {
        ApExternalSystem externalSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);
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

    public ArchiveEntityResultListVO findAccessPoints(Integer from, Integer max, Integer scopeId, SearchFilterVO filter) {
        searchFilterFactory.completeApTypesTreeInFilter(filter);
        Set<Integer> scopeList = new HashSet<>();
        if (scopeId != null) {
            scopeList.add(scopeId);
        } else {
            scopeList.add(1);
        }
        List<ApState> stateList = apAccessPointRepository.findApAccessPointByTextAndType(filter.getSearch(), filter.getAeTypeIds(), from, max, scopeList, null , null, null);


//        ApStateSpecification stateSpecification = new ApStateSpecification(filter);
//        PageRequest pageRequest = new PageRequest(from, max);
//        Page<ApState> pageResult = stateRepository.findAll(stateSpecification, pageRequest);

        return searchFilterFactory.createArchiveEntityResultListVO(stateList, stateList.size());
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
                    return apDataService.convertCoordinatesFromKml(content);
                case GML:
                    return apDataService.convertCoordinatesFromGml(content);
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
     * @return
     */
    public List<ApAccessPoint> findAccessPointsBySinglePartValues(List<Object> criterias) {

        return apAccessPointRepository.findAccessPointsBySinglePartValues(criterias);
    }

    public void updateAccessPoints(final ProcessingContext procCtx,
                                   final List<SyncEntityRequest> syncRequests) {
        if (syncRequests == null || syncRequests.isEmpty()) {
            return;
        }

        for (SyncEntityRequest syncReq : syncRequests) {
            synchronizeAccessPoint(procCtx, syncReq.getState(),
                                   syncReq.getEntityXml(), syncReq.getBindingState(), false);
        }
    }
}
