package cz.tacr.elza.service;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.math.BigInteger;
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

import cz.tacr.cam._2019.*;
import cz.tacr.elza.common.GeometryConvertor;
import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.SearchType;
import cz.tacr.elza.dataexchange.input.parts.context.ItemWrapper;
import cz.tacr.elza.dataexchange.input.parts.context.PartWrapper;
import cz.tacr.elza.groovy.GroovyResult;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.vo.DataRef;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.ap.item.ApUpdateItemVO;
import cz.tacr.elza.controller.vo.usage.*;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ExceptionUtils;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.packageimport.xml.SettingRecord;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FundRegisterScopeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.ScopeRelationRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.SysLanguageRepository;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.vo.ImportAccessPoint;

import static cz.tacr.elza.domain.ApState.StateApproval;


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
    private StructObjService structObjService;

    @Autowired
    private GroovyService groovyService;

    @Autowired
    private ExternalSystemService externalSystemService;

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
        long countDataRecordRef = dataRecordRefRepository.countAllByRecord(accessPoint);
        if (countDataRecordRef > 0) {
            throw new BusinessException("Nalezeno použití AP v tabulce ArrDataRecordRef.", RegistryCode.EXIST_FOREIGN_DATA).set("table", "ArrDataRecordRef");
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
            bindingStateRepository.save(eids);

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
        ExceptionUtils.isEmptyElseBusiness(apStates, "Nelze smazat třídu rejstříku, která je nastavena na rejstříku.", RegistryCode.USING_SCOPE_CANT_DELETE);
        final List<ApScope> apScopes = scopeRepository.findConnectedByScope(scope);
        ExceptionUtils.isEmptyElseBusiness(apScopes, "Nelze smazat oblast obsahující návazné oblasti.", RegistryCode.CANT_DELETE_SCOPE_WITH_CONNECTED);
        final List<ApScopeRelation> apScopeRelations = scopeRelationRepository.findByConnectedScope(scope);
        ExceptionUtils.isEmptyElseBusiness(apScopeRelations, "Nelze smazat oblast která je návaznou oblastí jiné oblasti.", RegistryCode.CANT_DELETE_CONNECTED_SCOPE);

        fundRegisterScopeRepository.delete(fundRegisterScopeRepository.findByScope(scope));
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
        RulPartType partType = structObjService.getPartTypeByCode(apPartFormVO.getPartTypeCode());
        if (!partType.getCode().equals("PT_NAME")) {
            throw new IllegalArgumentException("Část musí být typu PT_NAME");
        }

        ApChange apChange = apDataService.createChange(ApChange.Type.AP_CREATE);
        ApState apState = createAccessPoint(scope, type, apChange);
        ApAccessPoint accessPoint = apState.getAccessPoint();

        ApPart apPart = partService.createPart(partType, accessPoint, apChange, null);
        accessPoint.setPreferredPart(apPart);

        partService.createPartItems(apChange, apPart, apPartFormVO);
        updatePartValue(apPart);

        publishAccessPointCreateEvent(accessPoint);

        return apState;
    }

    public ApState createAccessPoint(final ApScope scope, final Entity entity, final String externalSystemCode) {
        Assert.notNull(scope, "Třída musí být vyplněna");
        StaticDataProvider sdp = staticDataService.getData();

        ApType type = sdp.getApTypeByCode(entity.getEnt());
        ApChange apChange = apDataService.createChange(ApChange.Type.AP_CREATE);
        ApState apState = createAccessPoint(scope, type, apChange);
        ApAccessPoint accessPoint = apState.getAccessPoint();

        createAccessPoint(scope, entity, accessPoint, apChange, sdp, externalSystemCode, apState);

        publishAccessPointCreateEvent(accessPoint);

        return apState;
    }

    public void connectAccessPoint(final ApState state, final Entity entity, final String externalSystemCode) {
        StaticDataProvider sdp = staticDataService.getData();
        ApScope scope = state.getScope();
        ApAccessPoint accessPoint = state.getAccessPoint();
        ApChange apChange = apDataService.createChange(ApChange.Type.AP_UPDATE);
        ApType type = sdp.getApTypeByCode(entity.getEnt());

        state.setDeleteChange(apChange);
        stateRepository.save(state);
        ApState stateNew = copyState(state, apChange);
        stateNew.setApType(type);
        stateNew.setStateApproval(StateApproval.NEW);
        stateRepository.save(stateNew);

        partService.deleteParts(accessPoint, apChange);

        createAccessPoint(scope, entity, accessPoint, apChange, sdp, externalSystemCode, stateNew);

        publishAccessPointUpdateEvent(accessPoint);
    }

    private void createAccessPoint(final ApScope scope,
                                   final Entity entity,
                                   final ApAccessPoint accessPoint,
                                   final ApChange apChange,
                                   final StaticDataProvider sdp,
                                   final String externalSystemCode,
                                   final ApState apState) {
        ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);
        ApBinding binding = externalSystemService.createApBinding(scope, entity.getEid(), apExternalSystem);
        externalSystemService.createApBindingState(binding, accessPoint, apChange,
                entity.getEns().value(), entity.getRevi().getRid(),  entity.getRevi().getUsr(), entity.getReid());

        List<ApPart> partList = new ArrayList<>();
        Map<Integer, List<ApItem>> itemMap = new HashMap<>();

        List<DataRef> dataRefList = new ArrayList<>();

        for (Part part : entity.getPrts().getP()) {
            RulPartType partType = sdp.getPartTypeByCode(part.getT().value());
            ApPart parentPart = part.getPrnt() != null ? findParentPart(binding, part.getPrnt()) : null;

            ApPart apPart = partService.createPart(partType, accessPoint, apChange, parentPart);
            externalSystemService.createApBindingItem(binding, part.getPid(), apPart, null);
            List<ApItem> itemList = partService.createPartItems(apChange, apPart, part.getItms().getBiOrAiOrEi(), binding, dataRefList);

            itemMap.put(apPart.getPartId(), itemList);
            partList.add(apPart);
        }
        createBindingForRel(dataRefList, binding, scope, apExternalSystem);

        accessPoint.setPreferredPart(findPreferredPart(partList));

        updatePartValues(apState, partList, itemMap);
    }

    private void createBindingForRel(final List<DataRef> dataRefList, final ApBinding binding, final ApScope scope, final ApExternalSystem apExternalSystem) {
        //TODO fantiš optimalizovat
        for (DataRef dataRef : dataRefList) {
            ApBindingItem apBindingItem = externalSystemService.findByBindingAndUuid(binding, dataRef.getUuid());
            if (apBindingItem.getItem() != null) {
                ArrDataRecordRef dataRecordRef = (ArrDataRecordRef) apBindingItem.getItem().getData();
                ApBinding refBinding = externalSystemService.findByScopeAndValueAndApExternalSystem(scope, dataRef.getValue().intValue(), apExternalSystem.getCode());
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

            apItemService.deletePartItems(apPart, change);
            partService.deletePart(apPart, change);

            ApPart newPart = partService.createPart(apPart, change);
            partService.createPartItems(change, newPart, apPartFormVO);

            partService.changeParentPart(apPart, newPart);

            if (apAccessPoint.getPreferredPart().getPartId().equals(apPart.getPartId())) {
                apAccessPoint.setPreferredPart(newPart);
                apAccessPointRepository.save(apAccessPoint);
            }
            updatePartValue(newPart);
//        }
    }

    private ApPart findPreferredPart(final List<ApPart> partList) {
        for (ApPart part : partList) {
            if (part.getPartType().getCode().equals("PT_NAME")) {
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

    public void updateBinding(BatchUpdateResult batchUpdateResult, Integer accessPointId, String externalSystemCode) {
        ApAccessPoint accessPoint = getAccessPoint(accessPointId);
        ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);
        ApBindingState bindingState = bindingStateRepository.findByAccessPointAndExternalSystem(accessPoint, apExternalSystem);
        ApBinding binding = bindingState.getBinding();

        if (batchUpdateResult instanceof BatchUpdateSaved) {
            BatchUpdateSaved batchUpdateSaved = (BatchUpdateSaved) batchUpdateResult;
            BatchEntityRecordRev batchEntityRecordRev = batchUpdateSaved.getRev().get(0);

            bindingState.setExtRevision(batchEntityRecordRev.getRev());
            binding.setValue(String.valueOf(batchEntityRecordRev.getEid()));

            bindingStateRepository.save(bindingState);
            bindingRepository.save(binding);
        } else {
            bindingItemRepository.deleteByBinding(binding);
            bindingStateRepository.delete(bindingState);
            bindingRepository.delete(binding);
        }
    }

    public BatchUpdate createBatchUpdate(final Integer accessPointId, final String externalSystemCode) {
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
                EntityRecordState.ERS_NEW.value(), null, userDetail.getUsername(), null);

        BatchUpdate batchUpdate = new BatchUpdate();
        batchUpdate.setInf(createBatchInfo(userDetail));
        batchUpdate.getChange().add(createCreateEntity(state, partList, itemMap, binding));
        return batchUpdate;
    }

    private BatchInfo createBatchInfo(UserDetail userDetail) {
        BatchInfo batchInfo = new BatchInfo();
        batchInfo.setUsr(userDetail.getUsername());
        batchInfo.setBid(UUID.randomUUID().toString());
        return batchInfo;
    }

    private CreateEntity createCreateEntity(ApState state, List<ApPart> partList, Map<Integer, List<ApItem>> itemMap, ApBinding binding) {
        CreateEntity createEntity = new CreateEntity();
        createEntity.setLid("LID" + state.getAccessPointId());
        createEntity.setEt(state.getApType().getCode());
        createEntity.setEuid(state.getAccessPoint().getUuid());
        createEntity.setPrts(createParts(state.getAccessPoint(), partList, itemMap, binding));
        return createEntity;
    }

    private Parts createParts(ApAccessPoint accessPoint, List<ApPart> partList, Map<Integer, List<ApItem>> itemMap, ApBinding binding) {
        List<ApBindingItem> bindingItems = new ArrayList<>();
        Parts parts = new Parts();

        ApPart preferPart = accessPoint.getPreferredPart();
        if (preferPart != null) {
            parts.getP().add(createPart(preferPart, itemMap, binding, bindingItems));
            if (CollectionUtils.isNotEmpty(partList)) {
                for (ApPart part : partList) {
                    if (!part.getPartId().equals(preferPart.getPartId())) {
                        parts.getP().add(createPart(part, itemMap, binding, bindingItems));
                    }
                }
            }
        }
        return parts;
    }

    private Part createPart(ApPart apPart, Map<Integer, List<ApItem>> itemMap, ApBinding binding, List<ApBindingItem> bindingItems) {
        String uuid = UUID.randomUUID().toString();
        ApBindingItem bindingItem = externalSystemService.createApBindingItem(binding, uuid, apPart, null);
        bindingItems.add(bindingItem);

        ApBindingItem parentBindingItem = apPart.getParentPart() != null ? findParentBindingItem(bindingItems, apPart.getParentPart().getPartId()) : null;


        Part part = new Part();
        part.setT(PartType.fromValue(apPart.getPartType().getCode()));
        part.setPid(uuid);
        part.setPrnt(parentBindingItem != null ? parentBindingItem.getValue() : null);
        part.setItms(createItems(apPart, itemMap, binding));
        return part;
    }

    private ApBindingItem findParentBindingItem(List<ApBindingItem> bindingItems, Integer partId) {
        if (CollectionUtils.isNotEmpty(bindingItems)) {
            for (ApBindingItem bindingItem : bindingItems) {
                if (bindingItem.getPart() != null && bindingItem.getPart().getPartId().equals(partId)) {
                    return bindingItem;
                }
            }
        }
        return null;
    }

    private Items createItems(ApPart apPart, Map<Integer, List<ApItem>> itemMap, ApBinding binding) {
        List<ApItem> itemList = itemMap.getOrDefault(apPart.getPartId(), new ArrayList<>());

        Items items = new Items();
        if (CollectionUtils.isNotEmpty(itemList)) {
            for (ApItem item : itemList) {
                items.getBiOrAiOrEi().add(createItem(item, binding));
            }
        }
        return items;
    }

    private Object createItem(ApItem item, ApBinding binding) {
        String itemTypeCode = item.getItemType().getCode();
        String itemSpecCode = item.getItemSpec() != null ? item.getItemSpec().getCode() : null;
        String uuid = UUID.randomUUID().toString();
        externalSystemService.createApBindingItem(binding, uuid, null, item);

        DataType dataType = DataType.fromCode(item.getItemType().getDataType().getCode());
        switch (dataType) {
            case BIT:
                ArrDataBit dataBit = (ArrDataBit) item.getData();
                ItemBoolean itemBoolean = new ItemBoolean();
                itemBoolean.setValue(dataBit.isValue());
                itemBoolean.setT(itemTypeCode);
                itemBoolean.setS(itemSpecCode);
                itemBoolean.setUuid(uuid);
                return itemBoolean;
            case URI_REF:
                ArrDataUriRef dataUriRef = (ArrDataUriRef) item.getData();
                ItemLink itemLink = new ItemLink();
                itemLink.setUrl(dataUriRef.getValue());
                itemLink.setNm(dataUriRef.getDescription());
                itemLink.setT(itemTypeCode);
                itemLink.setS(itemSpecCode);
                itemLink.setUuid(uuid);
                return itemLink;
            case TEXT:
                ArrDataText dataText = (ArrDataText) item.getData();
                ItemString itemText = new ItemString();
                itemText.setValue(dataText.getValue());
                itemText.setT(itemTypeCode);
                itemText.setS(itemSpecCode);
                itemText.setUuid(uuid);
                return itemText;
            case STRING:
                ArrDataString dataString = (ArrDataString) item.getData();
                ItemString itemString = new ItemString();
                itemString.setValue(dataString.getValue());
                itemString.setT(itemTypeCode);
                itemString.setS(itemSpecCode);
                itemString.setUuid(uuid);
                return itemString;
            case INT:
                ArrDataInteger dataInteger = (ArrDataInteger) item.getData();
                ItemInteger itemInteger = new ItemInteger();
                itemInteger.setValue(BigInteger.valueOf(dataInteger.getValueInt()));
                itemInteger.setT(itemTypeCode);
                itemInteger.setS(itemSpecCode);
                itemInteger.setUuid(uuid);
                return itemInteger;
            case UNITDATE:
                ArrDataUnitdate dataUnitdate = (ArrDataUnitdate) item.getData();
                ItemUnitDate itemUnitDate = new ItemUnitDate();
                itemUnitDate.setF(dataUnitdate.getValueFrom());
                itemUnitDate.setFe(dataUnitdate.getValueFromEstimated());
                itemUnitDate.setFmt(dataUnitdate.getFormat());
                itemUnitDate.setTo(dataUnitdate.getValueTo());
                itemUnitDate.setToe(dataUnitdate.getValueToEstimated());
                itemUnitDate.setT(itemTypeCode);
                itemUnitDate.setS(itemSpecCode);
                itemUnitDate.setUuid(uuid);
                return itemUnitDate;
            case ENUM:
                ItemEnum itemEnum = new ItemEnum();
                itemEnum.setT(itemTypeCode);
                itemEnum.setS(itemSpecCode);
                itemEnum.setUuid(uuid);
                return itemEnum;
            case RECORD_REF:
                ArrDataRecordRef dataRecordRef = (ArrDataRecordRef) item.getData();
                if (dataRecordRef.getBinding() != null) {
                    throw new SystemException("Do externího systému nelze poslat entitu, která odkazuje na entitu mimo externí systém.");
                }
                EntityRecordRef entityRecordRef = new EntityRecordRef();
                entityRecordRef.setEid(Long.parseLong(dataRecordRef.getBinding().getValue()));

                ItemEntityRef itemEntityRef = new ItemEntityRef();
                itemEntityRef.setEr(entityRecordRef);
                itemEntityRef.setT(itemTypeCode);
                itemEntityRef.setS(itemSpecCode);
                itemEntityRef.setUuid(uuid);
                return itemEntityRef;
            case COORDINATES:
                ArrDataCoordinates dataCoordinates = (ArrDataCoordinates) item.getData();
                ItemString itemCoordinates = new ItemString();
                itemCoordinates.setValue(GeometryConvertor.convert(dataCoordinates.getValue()));
                itemCoordinates.setT(itemTypeCode);
                itemCoordinates.setS(itemSpecCode);
                itemCoordinates.setUuid(uuid);
                return itemCoordinates;
        }
        return null;
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
        Assert.notNull(type.getRuleSystem(), "Typ musí mít vazbu na pravidla");

        apDataService.validateStructureType(type);

        ApChange change = apDataService.createChange(ApChange.Type.AP_CREATE);
        ApState apState = createStrucuredAccessPoint(scope, type, change);

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
     * Změna popisu přístupového bodu.
     * Podle vstupních a aktuálních dat se rozhodne, zda-li se bude popis mazat, vytvářet nebo jen upravovat - verzovaně.
     *
     * @param apState přístupový bod
     * @param description popis přístupového bodu
     * @return přístupový bod
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ApAccessPoint changeDescription(@AuthParam(type = AuthParam.Type.AP_STATE) final ApState apState,
                                           @Nullable final String description) {
        Validate.notNull(apState, "Přístupový bod musí být vyplněn");

        ApAccessPoint accessPoint = apState.getAccessPoint();
        apDataService.validationNotDeleted(apState);

        apDataService.changeDescription(apState, description, null);
        publishAccessPointUpdateEvent(accessPoint);
        return accessPoint;
    }

    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void migrateApItems(@AuthParam(type = AuthParam.Type.AP_STATE) final ApState apState,
                               final List<ApUpdateItemVO> apItems) {
        Validate.notNull(apState, "Přístupový bod musí být vyplněn");
        Validate.notNull(apItems);

        apDataService.validationMigrateAp(apState);

        ApRuleSystem ruleSystem = apState.getApType().getRuleSystem();
        ApAccessPoint accessPoint = apState.getAccessPoint();
        accessPoint.setState(ApStateEnum.INIT);
        saveWithLock(accessPoint);

        ApChange change = apDataService.createChange(ApChange.Type.AP_MIGRATE);

        List<ApItem> itemsDbAp = itemRepository.findValidItemsByAccessPoint(accessPoint);
        apItemService.changeItems(apItems, new ArrayList<>(itemsDbAp), change, (RulItemType it, RulItemSpec is, ApChange c, int objectId, int position)
                -> createApItem(accessPoint, it, is, c, objectId, position));

        apGeneratorService.generateAsyncAfterCommit(accessPoint.getAccessPointId(), change.getChangeId());
    }

    /**
     * Změna atributů jména přístupového bodu.
     *
     * @param apState přístupový bod
     * @param itemType typ atributu
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void deleteNameItemsByType(@AuthParam(type = AuthParam.Type.AP_STATE) final ApState apState,
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

        //apGeneratorService.generateAndSetResult(accessPoint, change);
        apGeneratorService.generateAsyncAfterCommit(accessPoint.getAccessPointId(), change.getChangeId());
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

        ApType apType = apState.getApType();
        if (apType.getRuleSystem() == null) {
            throw new BusinessException("Typ nemá vazbu na pravidla", BaseCode.INVALID_STATE);
        }
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
        ApAccessPoint accessPoint = apAccessPointRepository.findOne(accessPointId);
        if (accessPoint == null) {
            throw new ObjectNotFoundException("Přístupový bod neexistuje", BaseCode.ID_NOT_EXIST).setId(accessPointId);
        }
        return accessPoint;
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
        return sysLanguageRepository.findAll(new Sort(Sort.Direction.ASC, SysLanguage.FIELD_CODE));
    }

    /**
     * Získání třídy.
     *
     * @param scopeId identifikátor třídy
     * @return třída
     */
    public ApScope getScope(final Integer scopeId) {
        ApScope scope = scopeRepository.findOne(scopeId);
        if (scope == null) {
            throw new ObjectNotFoundException("Třída neexistuje", BaseCode.ID_NOT_EXIST).setId(scopeId);
        }
        return scope;
    }

    /**
     * Získání typu.
     *
     * @param typeId identifikátor typu
     * @return typ
     */
    public ApType getType(final Integer typeId) {
        ApType type = apTypeRepository.findOne(typeId);
        if (type == null) {
            throw new ObjectNotFoundException("Typ neexistuje", BaseCode.ID_NOT_EXIST).setId(typeId);
        }
        return type;
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
        List<ApAccessPoint> accessPoints = apAccessPointRepository.findAll(accessPointIds); // nahrat vsechny potrebne AP do Hibernate session
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
            apStateRepository.delete(states);
        }
        if (!changes.isEmpty()) {
            apChangeRepository.delete(changes);
        }
        apAccessPointRepository.delete(aps);
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
            apStateRepository.delete(states);
        }
        if (!changes.isEmpty()) {
            apChangeRepository.delete(changes);
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
    private ApState createAccessPoint(final ApScope scope, final ApType type, final ApChange change) {
        ApAccessPoint accessPoint = createAccessPointEntity(scope, type, change);
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
    private ApState createStrucuredAccessPoint(final ApScope scope, final ApType type, final ApChange change) {
        ApAccessPoint accessPoint = createAccessPointEntity(scope, type, change);
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
     * @param scope třída
     * @param type typ přístupového bodu
     * @param change změna
     * @return vytvořená entita AP
     */
    public static ApAccessPoint createAccessPointEntity(final ApScope scope, final ApType type, final ApChange change) {
        ApAccessPoint accessPoint = new ApAccessPoint();
        accessPoint.setUuid(UUID.randomUUID().toString());
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

    /**
     * Import přístupového bodu z externího systému.
     *
     * @param externalId identifikátor přístupového bodu v externím systému
     * @param externalIdTypeCode kód typu externího systému
     * @param externalSystem externí systém
     * @param data data pro založení/aktualizaci přístupového bodu
     * @return přístupový bod
     */
    @Transactional
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ApState importAccessPoint(final String externalId,
                                     final String externalIdTypeCode,
                                     final ApExternalSystem externalSystem,
                                     @AuthParam(type = AuthParam.Type.SCOPE) final ImportAccessPoint data) {
        Assert.notNull(externalId, "Identifikátor z externího systému musí být vyplněn");
        Assert.notNull(externalIdTypeCode, "Kód typu externího identifikátoru musí být vyplněn");
        Assert.notNull(externalSystem, "Externí systém, ze kterého importujeme přístupový bod musí být vyplněn");
        Assert.notNull(data, "Importní data musí být vyplněny");

        ApScope scope = data.getScope();
        ApType type = data.getType();
        ImportAccessPoint.Name preferredName = data.getPreferredName();
        List<ImportAccessPoint.Name> names = data.getNames();
        String description = data.getDescription();

        ApChange change = apDataService.createChange(ApChange.Type.AP_IMPORT, externalSystem);

        //TODO fantiš smazat nebo upravit
        ApExternalIdType externalIdType = staticDataService.getData().getApEidTypeByCode(externalIdTypeCode);
        ApState apStateExists = null;
//        ApState apStateExists = apStateRepository.getActiveByExternalIdAndScope(externalId, externalIdType, scope);

        ApState apState;
        ApAccessPoint accessPoint;
        if (apStateExists == null) {
            apState = createAccessPoint(scope, type, change);
            accessPoint = apState.getAccessPoint();
            if (StringUtils.isNotEmpty(description)) {
                //TODO : smazáno - vytvořit popis AP
            }
//            createExternalId(accessPoint, externalIdType, externalId, change);
            publishAccessPointCreateEvent(accessPoint);
        } else {
            apState = changeApType(apStateExists.getAccessPointId(), type.getApTypeId());
            accessPoint = apState.getAccessPoint();
            apDataService.changeDescription(apState, description, change);
            publishAccessPointUpdateEvent(accessPoint);
        }

        // kolekce pro kontrolu jmen vlastního přístupového bodu
        Set<String> uniqueNames = new HashSet<>();

        // TODO : smazáno - založení preferovaného jména

        // TODO : smazáné - chybí založení další jmen

        reindexDescItem(accessPoint);

        return apState;
    }

    /**
     * Založení externího identifikátoru přístupového bodu.
     *
     * @param accessPoint přístupový bod
     * @param externalIdType typ externího systému
     * @param externalId identifikátor v externím systému
     * @param change změna ve které se identifikátor zakládá
     */
    private void createExternalId(final ApAccessPoint accessPoint,
                                  final ApExternalIdType externalIdType,
                                  final String externalId,
                                  final ApChange change) {
        //TODO fantiš upravit
        ApBinding apBinding = new ApBinding();
//        apBinding.setValue(externalId);
//        apBinding.setAccessPoint(accessPoint);
//        apBinding.setCreateChange(change);
//        apBinding.setExternalIdType(externalIdType);
        bindingRepository.save(apBinding);
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
        if (!apPart.getPartType().getCode().equals("PT_NAME")) {
            throw new IllegalArgumentException("Preferované jméno musí být typu PT_NAME");
        }

        if (apPart.getParentPart() != null) {
            throw new IllegalArgumentException("Návazný part nelze změnit na preferovaný.");
        }

        accessPoint.setPreferredPart(apPart);
        apAccessPointRepository.save(accessPoint);
        updatePartValue(apPart);
    }

    public void checkUniqueBinding(ApScope scope, Integer archiveEntityId, String externalSystemCode) {
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
        bindingItemRepository.deleteByBinding(binding);
        bindingStateRepository.delete(bindingState);
        bindingRepository.delete(binding);
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
}
