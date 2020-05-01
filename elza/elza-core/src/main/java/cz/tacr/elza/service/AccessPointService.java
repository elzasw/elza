package cz.tacr.elza.service;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

import cz.tacr.elza.controller.vo.ApPartFormVO;
import org.apache.commons.collections4.CollectionUtils;
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
import cz.tacr.elza.controller.vo.usage.FundVO;
import cz.tacr.elza.controller.vo.usage.NodeVO;
import cz.tacr.elza.controller.vo.usage.OccurrenceType;
import cz.tacr.elza.controller.vo.usage.OccurrenceVO;
import cz.tacr.elza.controller.vo.usage.PartyVO;
import cz.tacr.elza.controller.vo.usage.RecordUsageVO;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApExternalIdType;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.ApNameItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApRuleSystem;
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
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.SysLanguage;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrUser;
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
import cz.tacr.elza.repository.ApDescriptionRepository;
import cz.tacr.elza.repository.ApExternalIdRepository;
import cz.tacr.elza.repository.ApNameItemRepository;
import cz.tacr.elza.repository.ApNameRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.DataPartyRefRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FundRegisterScopeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PartyCreatorRepository;
import cz.tacr.elza.repository.RelationEntityRepository;
import cz.tacr.elza.repository.ScopeRelationRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.SysLanguageRepository;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.vo.ImportAccessPoint;


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
    private ApNameRepository apNameRepository;

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
    private DataPartyRefRepository dataPartyRefRepository;

    @Autowired
    private PartyCreatorRepository partyCreatorRepository;

    @Autowired
    private RelationEntityRepository relationEntityRepository;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private ApChangeRepository apChangeRepository;

    @Autowired
    private ApDescriptionRepository descriptionRepository;

    @Autowired
    private ApExternalIdRepository externalIdRepository;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private AccessPointDataService apDataService;

    @Autowired
    private ApNameItemRepository nameItemRepository;

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
                                                        @Nullable final Collection<StateApproval> approvalStates) {

        Set<Integer> scopeIdsForSearch = getScopeIdsForSearch(fund, scopeId);

        return apAccessPointRepository.findApAccessPointByTextAndType(searchRecord, apTypeIds, firstResult, maxResults, scopeIdsForSearch, approvalStates);
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
                                                    @Nullable final Collection<StateApproval> approvalStates) {

        Set<Integer> scopeIdsForSearch = getScopeIdsForSearch(fund, scopeId);

        return apAccessPointRepository.findApAccessPointByTextAndTypeCount(searchRecord, apTypeIds, scopeIdsForSearch, approvalStates);
    }

    /**
     * Kontrola, jestli je používán přístupový bod v navázaných tabulkách.
     *
     * @param accessPoint přístupový bod
     * @throws BusinessException napojení na jinou tabulku
     */
    public void checkDeletion(final ApAccessPoint accessPoint) {
        apDataService.validationNotParty(accessPoint);

        long countDataRecordRef = dataRecordRefRepository.countAllByRecord(accessPoint);
        if (countDataRecordRef > 0) {
            throw new BusinessException("Nalezeno použití AP v tabulce ArrDataRecordRef.", RegistryCode.EXIST_FOREIGN_DATA).set("table", "ArrDataRecordRef");
        }

        // vztah osoby par_relation_entity
        List<ParRelationEntity> relationEntities = relationEntityRepository.findActiveByRecord(accessPoint);
        if (CollectionUtils.isNotEmpty(relationEntities)) {
            throw new BusinessException("Nelze smazat/zneplatnit AP na který mají vazbu jiné aktivní osoby v relacích.", RegistryCode.EXIST_FOREIGN_DATA)
                    .set("recordId", accessPoint.getAccessPointId())
                    .set("relationEntities", relationEntities.stream().map(ParRelationEntity::getRelationEntityId).collect(toList()))
                    .set("partyIds", relationEntities.stream().map(ParRelationEntity::getRelation).map(ParRelation::getParty).map(ParParty::getPartyId).collect(toList()));
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

            List<ApName> names = apNameRepository.findByAccessPoint(accessPoint);
            names.forEach(name -> name.setDeleteChange(change));
            apNameRepository.save(names);

            ApDescription desc = descriptionRepository.findByAccessPoint(accessPoint);
            // can be without description
            if (desc != null) {
                desc.setDeleteChange(change);
                descriptionRepository.save(desc);
            }

            List<ApExternalId> eids = externalIdRepository.findByAccessPoint(accessPoint);
            eids.forEach(eid -> eid.setDeleteChange(change));
            externalIdRepository.save(eids);

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
        final Long relationsCount = scopeRelationRepository.countExistsRelations(scope, connectedScope);
        if (relationsCount > 0) {
            throw new BusinessException("Nelze zrušit provázání oblastí - existuje vazba mezi osobami z těchto oblastí.", RegistryCode.CANT_DELETE_SCOPE_RELATION_EXISTS);
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
     * @param party osoba, může být null
     * @return použití rejstříku/osoby
     */
    public RecordUsageVO findRecordUsage(final ApAccessPoint record, @Nullable final ParParty party) {
        List<FundVO> usedFunds = findUsedFunds(record, party); // výskyt v archivních souborech
        List<PartyVO> usedParties = findUsedParties(record, party); // výskyt v uzlech

        return new RecordUsageVO(usedFunds, usedParties);
    }

    /**
     * Najde osoby se kterými je předaný resjtřík/osoba ve vztahu.
     *
     * @param record rejstřík
     * @param party osoba, může být null
     * @return použití rejstříku/osoby
     */
    private List<PartyVO> findUsedParties(final ApAccessPoint record, final ParParty party) {
        List<PartyVO> usedParties = new LinkedList<>();

        // hledání podle vztahů
        final Map<Integer, PartyVO> partyVOMap = relationEntityRepository.findByAccessPoint(record).stream().map(rel -> {
            ParParty relParty = rel.getRelation().getParty(); // party fetched by query
            PartyVO partyVO = new PartyVO();
            partyVO.setId(relParty.getPartyId());
            ApName prefName = apNameRepository.findPreferredNameByAccessPoint(relParty.getAccessPoint());
            partyVO.setName(prefName.getFullName());

            OccurrenceVO occurrenceVO = new OccurrenceVO(rel.getRelationEntityId(), OccurrenceType.PAR_RELATION_ENTITY);
            List<OccurrenceVO> occurrences = new LinkedList<>();
            occurrences.add(occurrenceVO);
            partyVO.setOccurrences(occurrences);

            return partyVO;
        }).collect(toMap(PartyVO::getId, Function.identity()));

        if (party != null) { // hledání podle tvůrců
            partyCreatorRepository.findByCreatorParty(party).forEach(c -> {
                PartyVO partyVO;
                ParParty creator = c.getParty();
                if (partyVOMap.containsKey(creator.getPartyId())) { // pro osobu již existuje vztah
                    partyVO = partyVOMap.get(creator.getPartyId());
                } else {
                    partyVO = new PartyVO(); // nový výskyt
                    partyVO.setId(creator.getPartyId());
                    ApName prefName = apNameRepository.findPreferredNameByAccessPoint(creator.getAccessPoint());
                    partyVO.setName(prefName.getFullName());

                    List<OccurrenceVO> occurrences = new LinkedList<>();
                    partyVO.setOccurrences(occurrences);
                    partyVOMap.put(creator.getPartyId(), partyVO);
                }

                OccurrenceVO occurrenceVO = new OccurrenceVO(c.getCreatorId(), OccurrenceType.PAR_CREATOR);
                partyVO.getOccurrences().add(occurrenceVO);
            });
        }

        usedParties.addAll(partyVOMap.values());

        return usedParties;
    }

    /**
     * Najde použité archivní soubory.
     *
     * @param record rejstřík
     * @param party osoba, může být null
     * @return použití rejstříku/osoby
     */
    private List<FundVO> findUsedFunds(final ApAccessPoint record, final ParParty party) {
        List<ArrData> dataList = new LinkedList<>(dataRecordRefRepository.findByRecord(record));
        if (party != null) {
            dataList.addAll(dataPartyRefRepository.findByParty(party));
        }

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
        return dataRecordRefRepository.findByRecord(record).isEmpty() &&
                relationEntityRepository.findByAccessPoint(record).isEmpty();
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

        publishAccessPointCreateEvent(accessPoint);

        return apState;
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
//        }
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
        createStructuredName(accessPoint, true, language, change);
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
        apDataService.validationNotParty(accessPoint);

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

        accessPoint.setRuleSystem(apType.getRuleSystem());
        ApAccessPoint result = saveWithLock(accessPoint);
        if (result.getRuleSystem() != null) {
            //apGeneratorService.generateAndSetResult(accessPoint, change);
            apGeneratorService.generateAsyncAfterCommit(accessPointId, change.getChangeId());
        }

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
        apDataService.validationNotParty(accessPoint);

        if (accessPoint.getRuleSystem() != null) {
            throw new BusinessException("Nelze upravovat charakteristiku u strukturovaného přístupového bodu",
                    BaseCode.INVALID_STATE);
        }

        apDataService.changeDescription(apState, description, null);
        publishAccessPointUpdateEvent(accessPoint);
        return accessPoint;
    }

    /**
     * Změna atributů jména.
     *
     * @param apState přístupový bod
     * @param name jméno
     * @param items položky změny
     * @return nové položky, které ze vytvořili při změně
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public List<ApItem> changeNameItems(@AuthParam(type = AuthParam.Type.AP_STATE) final ApState apState,
                                        final ApName name,
                                        final List<ApUpdateItemVO> items) {
        Validate.notNull(apState, "Přístupový bod musí být vyplněn");
        Validate.notNull(name, "Jméno musí být vyplněno");
        Validate.notEmpty(items, "Musí být alespoň jedna položka ke změně");

        ApAccessPoint accessPoint = apState.getAccessPoint();
        apDataService.validationNotDeleted(apState);
        apDataService.validationNotDeleted(name);

        List<ApNameItem> itemsDb = nameItemRepository.findValidItemsByName(name);

        ApChange change;
        if (name.getState() == ApStateEnum.TEMP) {
            change = name.getCreateChange();
        } else {
            change = apDataService.createChange(ApChange.Type.NAME_UPDATE);
        }
        List<ApItem> itemsCreated = apItemService.changeItems(items, new ArrayList<>(itemsDb), change, (RulItemType it, RulItemSpec is, ApChange c, int objectId, int position)
                -> createNameItem(name, it, is, c, objectId, position));

        //apGeneratorService.generateAndSetResult(name.getAccessPoint(), change);
        apGeneratorService.generateAsyncAfterCommit(name.getAccessPoint().getAccessPointId(), change.getChangeId());

        return itemsCreated;
    }

    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void migrateApItems(@AuthParam(type = AuthParam.Type.AP_STATE) final ApState apState,
                               final List<ApUpdateItemVO> apItems,
                               final Map<ApName, List<ApUpdateItemVO>> nameItemsMap) {
        Validate.notNull(apState, "Přístupový bod musí být vyplněn");
        Validate.notNull(apItems);
        Validate.notNull(nameItemsMap);

        apDataService.validationMigrateAp(apState);

        Set<ApName> apNames = nameItemsMap.keySet();
        for (ApName name : apNames) {
            name.setState(ApStateEnum.INIT);
        }
        apNameRepository.save(apNames);

        ApRuleSystem ruleSystem = apState.getApType().getRuleSystem();
        ApAccessPoint accessPoint = apState.getAccessPoint();
        accessPoint.setRuleSystem(ruleSystem);
        accessPoint.setState(ApStateEnum.INIT);
        saveWithLock(accessPoint);

        ApChange change = apDataService.createChange(ApChange.Type.AP_MIGRATE);

        List<ApItem> itemsDbAp = itemRepository.findValidItemsByAccessPoint(accessPoint);
        apItemService.changeItems(apItems, new ArrayList<>(itemsDbAp), change, (RulItemType it, RulItemSpec is, ApChange c, int objectId, int position)
                -> createApItem(accessPoint, it, is, c, objectId, position));

        List<ApNameItem> itemsDbNames = apNames.isEmpty() ? Collections.emptyList() : nameItemRepository.findValidItemsByNames(apNames);
        Map<Integer, List<ApNameItem>> nameApNameItemMap = itemsDbNames.stream().collect(groupingBy(ApNameItem::getNameId));

        for (Map.Entry<ApName, List<ApUpdateItemVO>> entry : nameItemsMap.entrySet()) {
            List<ApUpdateItemVO> items = entry.getValue();
            ApName name = entry.getKey();
            List<ApNameItem> itemsDb = nameApNameItemMap.computeIfAbsent(name.getNameId(), k -> new ArrayList<>());
            apItemService.changeItems(items, new ArrayList<>(itemsDb), change, (RulItemType it, RulItemSpec is, ApChange c, int objectId, int position)
                    -> createNameItem(name, it, is, c, objectId, position));
        }

        apGeneratorService.generateAsyncAfterCommit(accessPoint.getAccessPointId(), change.getChangeId());
    }

    /**
     * Změna atributů jména přístupového bodu.
     *
     * @param apState přístupový bod
     * @param name jméno ap
     * @param itemType typ atributu
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void deleteNameItemsByType(@AuthParam(type = AuthParam.Type.AP_STATE) final ApState apState,
                                      final ApName name,
                                      final RulItemType itemType) {
        Validate.notNull(apState, "Přístupový bod musí být vyplněn");
        Validate.notNull(name, "Jméno musí být vyplněno");
        Validate.notNull(itemType, "Typ musí být vyplněn");

        apDataService.validationNotDeleted(apState);
        apDataService.validationNotDeleted(name);

        ApAccessPoint accessPoint = apState.getAccessPoint();

        ApChange change;
        if (accessPoint.getState() == ApStateEnum.TEMP) {
            change = apState.getCreateChange();
        } else {
            change = apDataService.createChange(ApChange.Type.AP_UPDATE);
        }
        apItemService.deleteItemsByType(nameItemRepository, name, itemType, change);

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
     * Vytvoření nepreferovaného jména přístupového bodu.
     *
     * @param apState přístupový bod
     * @param name jméno přístupového bodu
     * @param complement doplněk přístupového bodu
     * @param language jazyk jména
     * @return založené jméno
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ApName createAccessPointName(@AuthParam(type = AuthParam.Type.AP_STATE) final ApState apState,
                                        final String name,
                                        @Nullable final String complement,
                                        @Nullable final SysLanguage language) {
        Validate.notNull(apState, "Přístupový bod musí být vyplněn");
        apDataService.validationNotDeleted(apState);

        ApName apName = createName(apState, false, name, complement, language, null, true);
        reindexDescItem(apState.getAccessPoint());
        return apName;
    }

    /**
     * Založení strukturovaného jména přístupového bodu - dočasné, nutné potvrdit {@link #confirmAccessPointName}.
     *
     * @param apState přístupový bod
     * @return založené strukturované jméno
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ApName createAccessPointStructuredName(@AuthParam(type = AuthParam.Type.AP_STATE) final ApState apState) {
        Validate.notNull(apState, "Přístupový bod musí být vyplněn");
        apDataService.validationNotDeleted(apState);

        ApAccessPoint accessPoint = apState.getAccessPoint();
        ApName apName = createStructuredName(accessPoint, false, null, null);
        reindexDescItem(accessPoint);
        return apName;
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

            ApName preferredName = apNameRepository.findPreferredNameByAccessPoint(accessPoint);
            preferredName.setState(ApStateEnum.INIT);
            apNameRepository.save(preferredName);

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

        if (accessPoint.getRuleSystem() != null) {
            throw new BusinessException("Nelze AP přepnout do řízení pravidly, protože již pravidla má", BaseCode.INVALID_STATE);
        }

        ApType apType = apState.getApType();
        if (apType.getRuleSystem() == null) {
            throw new BusinessException("Typ nemá vazbu na pravidla", BaseCode.INVALID_STATE);
        }

        accessPoint.setRuleSystem(apType.getRuleSystem());
        saveWithLock(accessPoint);

        ApChange change = apDataService.createChange(ApChange.Type.AP_UPDATE);
        //apGeneratorService.generateAndSetResult(accessPoint, change);
        apGeneratorService.generateAsyncAfterCommit(accessPoint.getAccessPointId(), change.getChangeId());
    }

    /**
     * Potvrzení dočasného jména a převalidování celého AP.
     *
     * @param apState přístupový bod
     * @param name jméno
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void confirmAccessPointName(@AuthParam(type = AuthParam.Type.AP_STATE) final ApState apState,
                                       final ApName name) {
        Validate.notNull(apState, "Přístupový bod musí být vyplněn");
        Validate.notNull(name, "Jméno musí být vyplněno");
        apDataService.validationNotDeleted(apState);
        apDataService.validationNotDeleted(name);

        if (name.getState() == ApStateEnum.TEMP) {
            name.setState(ApStateEnum.INIT);
            apNameRepository.save(name);
            //apGeneratorService.generateAndSetResult(accessPoint, name.getCreateChange());
            apGeneratorService.generateAsyncAfterCommit(apState.getAccessPointId(), name.getCreateChangeId());
        } else {
            throw new BusinessException("Nelze potvrdit jméno, které není dočasné", BaseCode.INVALID_STATE);
        }
    }

    /**
     * Aktualizace jména přístupového bodu - verzovaně.
     * Použití v případě, že se nejedná o strukturovaný popis.
     *
     * @param apState přístupový bod
     * @param apName upravované jméno přístupového bodu
     * @param name jméno přístupového bodu
     * @param complement doplněk přístupového bodu
     * @param language jazyk jména
     * @return upravený jméno
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ApName updateAccessPointName(@AuthParam(type = AuthParam.Type.AP_STATE) final ApState apState,
                                        final ApName apName,
                                        final String name,
                                        @Nullable final String complement,
                                        @Nullable final SysLanguage language) {
        Validate.notNull(apState, "Přístupový bod musí být vyplněn");
        Validate.notNull(apName, "Upravované jméno musí být vyplněno");
        Validate.notNull(name, "Nové jméno musí být vyplněno");

        ApAccessPoint accessPoint = apState.getAccessPoint();
        apDataService.validationNotDeleted(apState);
        apDataService.validationNotDeleted(apName);
        apDataService.validationNotParty(accessPoint);

        if (accessPoint.getRuleSystem() != null) {
            throw new BusinessException("Nelze upravovat jméno u strukturovaného přístupového bodu",
                    BaseCode.INVALID_STATE);
        }

        ApChange change = apDataService.createChange(ApChange.Type.NAME_UPDATE);
        String fullName = AccessPointDataService.generateFullName(name, complement);
        ApName apNameNew = apDataService.updateAccessPointName(apState, apName, name, complement, fullName, language, change, true);
        publishAccessPointUpdateEvent(accessPoint);
        reindexDescItem(accessPoint);
        return apNameNew;
    }

    /**
     * Aktualizace jména přístupového bodu - verzovaně.
     * Použití v případě, že jde o strukturovaný popis.
     *
     * @param apState přístupový bod
     * @param apName upravované jméno přístupového bodu
     * @param language jazyk jména
     * @return upravený jméno
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ApName updateAccessPointName(@AuthParam(type = AuthParam.Type.AP_STATE) final ApState apState,
                                        final ApName apName,
                                        @Nullable final SysLanguage language) {
        Validate.notNull(apState, "Přístupový bod musí být vyplněn");
        Validate.notNull(apName, "Upravované jméno musí být vyplněno");
        apDataService.validationNotDeleted(apState);
        apDataService.validationNotDeleted(apName);

        ApType apType = apState.getApType();
        if (apType.getRuleSystem() == null) {
            throw new BusinessException("Nelze upravovat jméno u nestrukturovaného přístupového bodu",
                    BaseCode.INVALID_STATE);
        }

        ApChange change = apDataService.createChange(ApChange.Type.NAME_UPDATE);
        ApName newName = apDataService.updateAccessPointName(apState, apName, apName.getName(), apName.getComplement(), apName.getFullName(), language, change, true);
        apItemService.copyItems(apName, newName, change);
        //apGeneratorService.generateAndSetResult(accessPoint, change);
        apGeneratorService.generateAsyncAfterCommit(apState.getAccessPointId(), change.getChangeId());
        reindexDescItem(apState.getAccessPoint());
        return newName;
    }

    /**
     * Smazání nepreferovaného přístupového bodu.
     *
     * @param apState přístupový bod
     * @param name mazané jméno
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void deleteAccessPointName(@AuthParam(type = AuthParam.Type.AP_STATE) final ApState apState,
                                      final ApName name) {
        Validate.notNull(apState, "Přístupový bod musí být vyplněn");
        Validate.notNull(name, "Upravované jméno musí být vyplněno");

        apDataService.validationNotDeleted(apState);
        apDataService.validationNotDeleted(name);

        ApAccessPoint accessPoint = apState.getAccessPoint();
        ApChange change = apDataService.createChange(ApChange.Type.NAME_DELETE);
        deleteName(apState, name, change);

        if (accessPoint.getRuleSystem() != null) {
            //apGeneratorService.generateAndSetResult(accessPoint, change);
            apGeneratorService.generateAsyncAfterCommit(accessPoint.getAccessPointId(), change.getChangeId());
        }
        publishAccessPointUpdateEvent(accessPoint);
        reindexDescItem(accessPoint);
    }

    /**
     * Smazání nepreferovaného přístupového bodu.
     *
     * @param apState přístupový bod
     * @param name mazané jméno
     * @param deleteChange změna pro smazání
     */
    private void deleteName(@AuthParam(type = AuthParam.Type.AP_STATE) final ApState apState,
                            final ApName name,
                            final ApChange deleteChange) {
        Validate.notNull(apState, "Přístupový bod musí být vyplněn");
        Validate.notNull(name, "Upravované jméno musí být vyplněno");
        Validate.notNull(deleteChange, "Změna pro mazání musí být vyplněna");

        apDataService.validationNotDeleted(apState);
        apDataService.validationNotDeleted(name);

        if (name.isPreferredName()) {
            throw new BusinessException("Nelze mazat preferované jméno", RegistryCode.CANT_DELETE_PREFERRED_NAME).set("nameId", name.getNameId());
        }

        name.setDeleteChange(deleteChange);
        apNameRepository.save(name);
    }

    /**
     * Vrati preferovane jmeno pristupoveho bodu
     * @param accessPoint
     * @return
     */
    public ApName getPreferredAccessPointName(final ApAccessPoint accessPoint) {
    	ApName prefName = apNameRepository.findPreferredNameByAccessPoint(accessPoint);
        if (prefName == null) {
        	// ?? Asi by bylo vhodnejsi vyhodit vyjimku
        	logger.error("AccessPoint without preferred name, apId={}", accessPoint.getAccessPointId());
            return null;
        }
        return prefName;
    }

    /**
     * Nastavení nepreferované jméno jako preferované přístupovému bodu.
     *
     * @param apState přístupový bod
     * @param name nastavované jméno
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void setPreferredAccessPointName(@AuthParam(type = AuthParam.Type.AP_STATE) final ApState apState,
                                            final ApName name) {
        Validate.notNull(apState, "Přístupový bod musí být vyplněn");
        Validate.notNull(name, "Upravované jméno musí být vyplněno");

        apDataService.validationNotDeleted(apState);
        apDataService.validationNotDeleted(name);

        // pokud je jméno již jako preferované, není třeba cokoliv dělat
        if (name.isPreferredName()) {
            return;
        }

        ApAccessPoint accessPoint = apState.getAccessPoint();

        ApChange change = apDataService.createChange(ApChange.Type.NAME_SET_PREFERRED);
        ApName preferredNameOld = apNameRepository.findPreferredNameByAccessPoint(accessPoint);

        // založení nové verze původního preferovaného jména (nyní nepreferované)
        ApName nameNew = new ApName(preferredNameOld);
        nameNew.setCreateChange(change);
        nameNew.setPreferredName(false);
        apNameRepository.save(nameNew);

        // zneplatnění původního preferovaného jména
        preferredNameOld.setDeleteChange(change);
        apNameRepository.save(preferredNameOld);

        // založení nové verze původního preferovaného jména (nyní preferovaného)
        ApName preferredNameNew = new ApName(name);
        preferredNameNew.setCreateChange(change);
        preferredNameNew.setPreferredName(true);
        apNameRepository.save(preferredNameNew);

        // zneplatnění původního nepreferovaného jména
        name.setDeleteChange(change);
        apNameRepository.save(name);

        ApType apType = apState.getApType();
        if (apType.getRuleSystem() != null) {
            apItemService.copyItems(preferredNameOld, nameNew, change);
            apItemService.copyItems(name, preferredNameNew, change);
        }

        if (accessPoint.getRuleSystem() != null) {
            //apGeneratorService.generateAndSetResult(accessPoint, change);
            apGeneratorService.generateAsyncAfterCommit(accessPoint.getAccessPointId(), change.getChangeId());
        }

        reindexDescItem(accessPoint);
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
     * Získání jména.
     *
     * @param objectId identifikátor objektu jména
     * @return jméno
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_RD_ALL, UsrPermission.Permission.AP_SCOPE_RD})
    public ApName getName(@AuthParam(type = AuthParam.Type.AP) final ApAccessPoint accessPoint, final Integer objectId) {
        Validate.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        return getName(objectId);
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
     * Získání jména.
     *
     * @param objectId identifikátor objektu jména
     * @return jméno
     */
    public ApName getName(final Integer objectId) {
        ApName name = apNameRepository.findByObjectId(objectId);
        if (name == null) {
            throw new ObjectNotFoundException("Jméno přístupového bodu neexistuje", BaseCode.ID_NOT_EXIST).setId(objectId);
        }
        return name;
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
        descriptionRepository.removeTemp();
        apNameRepository.removeTemp();
        List<ApState> states = apStateRepository.findTempStates();
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
        apAccessPointRepository.removeTemp();
    }

    /**
     * Odstranění dočasných AP včetně návazných objektů u AP.
     */
    private void removeTempAccessPoint(final ApAccessPoint ap) {
        apItemService.removeTempItems(ap);
        descriptionRepository.removeTemp(ap);
        apNameRepository.removeTemp(ap);
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
        accessPoint.setRuleSystem(type.getRuleSystem());
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
     * Založení jména.
     *
     * @param apState přístupový bod
     * @param preferredName zda-li se jedná o preferované jméno
     * @param name jméno přístupového bodu
     * @param complement doplněk přístupového bodu
     * @param language jazyk jména
     * @param change změna
     * @return jméno
     */
    private ApName createName(final ApState apState,
                              final boolean preferredName,
                              final String name,
                              @Nullable final String complement,
                              @Nullable final SysLanguage language,
                              @Nullable final ApChange change,
                              final boolean validate) {
        Validate.notNull(apState, "Přístupový bod musí být vyplněn");
        Validate.notNull(name, "Jméno musí být vyplněno");

        ApChange createChange = change == null ? apDataService.createChange(ApChange.Type.NAME_CREATE) : change;
        ApName apName = createNameEntity(apState.getAccessPoint(), preferredName, name, complement, language, createChange);
        apNameRepository.save(apName);
        if (validate) {
            apDataService.validationNameUnique(apState.getScope(), apName.getFullName());
        }
        return apName;
    }

    /**
     * Vytvoření strukturovaného jména.
     *
     * @param accessPoint přístupový bod
     * @param preferredName zda-li se jedná o preferované jméno
     * @param language jazyk jména
     * @param change změna
     * @return založené a uložené jméno
     */
    private ApName createStructuredName(final ApAccessPoint accessPoint,
                                        final boolean preferredName,
                                        @Nullable final SysLanguage language,
                                        @Nullable final ApChange change) {
        Validate.notNull(accessPoint, "Přístupový bod musí být vyplněn");

        ApChange createChange = change == null ? apDataService.createChange(ApChange.Type.NAME_CREATE) : change;
        ApName apName = createNameEntity(accessPoint, preferredName, null, null, language, createChange);
        apName.setState(ApStateEnum.TEMP);

        return apNameRepository.save(apName);
    }

    /**
     * Vytvoření entity pro jméno přístupového bodu.
     *
     * @param accessPoint přístupový bod
     * @param preferredName zda-li se jedná o preferované jméno
     * @param name jméno přístupového bodu
     * @param complement doplněk přístupového bodu
     * @param language jazyk jména
     * @param createChange zakládací změna
     * @return vytvořená entita
     */
    public ApName createNameEntity(final ApAccessPoint accessPoint,
                                   final boolean preferredName,
                                   final @Nullable String name,
                                   final @Nullable String complement,
                                   final @Nullable SysLanguage language,
                                   final ApChange createChange) {
        ApName apName = new ApName();
        apName.setName(name);
        apName.setComplement(complement);
        apName.setFullName(AccessPointDataService.generateFullName(name, complement));
        apName.setPreferredName(preferredName);
        apName.setLanguage(language);
        apName.setAccessPoint(accessPoint);
        apName.setCreateChange(createChange);
        apName.setObjectId(nextNameObjectId());
        return apName;
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
     * Vytvoření entity hodnoty atributu jména.
     *
     * @param name jméno pro který atribut tvoříme
     * @param it typ atributu
     * @param is specifikace atribututu
     * @param c změna
     * @param objectId jednoznačný identifikátor položky (nemění se při odverzování)
     * @param position pozice
     * @return vytvořená položka
     */
    private ApItem createNameItem(final ApName name, final RulItemType it, final RulItemSpec is, final ApChange c, final int objectId, final int position) {
        ApNameItem item = new ApNameItem();
        item.setName(name);
        item.setItemType(it);
        item.setItemSpec(is);
        item.setCreateChange(c);
        item.setObjectId(objectId);
        item.setPosition(position);
        return item;
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
     * Validace jména, že není smazaný.
     *
     * @param name jméno
     */
    private void validationNotDeleted(final ApName name) {
        if (name.getDeleteChange() != null) {
            throw new BusinessException("Nelze upravit jméno přístupového bodu", RegistryCode.CANT_CHANGE_DELETED_NAME)
                    .set("nameId", name.getNameId());
        }
    }

    /**
     * Validace unikátnosti jména v daném scope.
     *
     * @param scope třída
     * @param fullName validované jméno
     */
    private void validationNameUnique(final ApScope scope, final String fullName) {
        Assert.notNull(scope, "Přístupový bod musí být vyplněn");
        Assert.notNull(fullName, "Plné jméno musí být vyplněno");

        int count = apNameRepository.countUniqueName(fullName, scope);
        if (count > 1) {
            throw new BusinessException("Celé jméno není unikátní v rámci třídy", RegistryCode.NOT_UNIQUE_FULL_NAME)
                    .set("fullName", fullName)
                    .set("scopeId", scope.getScopeId());
        }
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

        ApExternalIdType externalIdType = staticDataService.getData().getApEidTypeByCode(externalIdTypeCode);
        ApState apStateExists = apStateRepository.getActiveByExternalIdAndScope(externalId, externalIdType, scope);

        ApState apState;
        ApAccessPoint accessPoint;
        if (apStateExists == null) {
            apState = createAccessPoint(scope, type, change);
            accessPoint = apState.getAccessPoint();
            if (StringUtils.isNotEmpty(description)) {
                apDataService.createDescription(accessPoint, description, change);
            }
            createExternalId(accessPoint, externalIdType, externalId, change);
            publishAccessPointCreateEvent(accessPoint);
        } else {
            apState = changeApType(apStateExists.getAccessPointId(), type.getApTypeId());
            accessPoint = apState.getAccessPoint();
            invalidateAllNames(accessPoint, change);
            apDataService.changeDescription(apState, description, change);
            publishAccessPointUpdateEvent(accessPoint);
        }

        // kolekce pro kontrolu jmen vlastního přístupového bodu
        Set<String> uniqueNames = new HashSet<>();

        // založení preferovaného jména
        ApName nameCreated = createName(apState, true, preferredName.getName(), preferredName.getComplement(), preferredName.getLanguage(), change, false);
        uniqueNames.add(nameCreated.getFullName().toLowerCase());

        List<ApName> newNames = new ArrayList<>();
        newNames.add(nameCreated);

        // založení další jmen
        if (CollectionUtils.isNotEmpty(names)) {
            for (ImportAccessPoint.Name name : names) {
                nameCreated = createName(apState, false, name.getName(), name.getComplement(), name.getLanguage(), change, false);
                newNames.add(nameCreated);
                String compareName = nameCreated.getFullName().toLowerCase();
                if (uniqueNames.contains(compareName)) {
                    throw new BusinessException("Celé jméno není unikátní v rámci jmen přístupového bodu", RegistryCode.NOT_UNIQUE_FULL_NAME)
                            .set("fullName", nameCreated.getFullName())
                            .set("scopeId", apState.getScopeId());
                }
                uniqueNames.add(compareName);
            }
        }

        for (ApName name : newNames) {
            apDataService.validationNameUnique(apState.getScope(), name.getFullName());
        }

        reindexDescItem(accessPoint);

        return apState;
    }

    /**
     * Invalidace všech jmen na přístupovém bodu.
     *
     * @param accessPoint přístupový bod
     * @param change změna, která se nastaví na smazání jmen
     */
    private void invalidateAllNames(final ApAccessPoint accessPoint, final ApChange change) {
        apNameRepository.flush();
        apNameRepository.invalidateByAccessPointIdIn(Collections.singleton(accessPoint.getAccessPointId()), change);
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
        ApExternalId apExternalId = new ApExternalId();
        apExternalId.setValue(externalId);
        apExternalId.setAccessPoint(accessPoint);
        apExternalId.setCreateChange(change);
        apExternalId.setExternalIdType(externalIdType);
        externalIdRepository.save(apExternalId);
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
     * Synchronizace přístupového bodu.
     *
     * @param apStateCmp porovnávaný přístupový bod (v případě, že není uložen v DB, zakládáme nový)
     * @param names jména přístupového bodu (musí existovat alespoň jedna hodnota - preferované jméno,
     * které je vždy první)
     * @param description popis přístupového bodu
     * @return synchronizovaný přístupový bod
     */
    public ApState syncAccessPoint(final ApState apStateCmp,
                                   final List<ApName> names,
                                   @Nullable final ApDescription description) {
        Assert.notNull(apStateCmp, "Přístupový bod musí být vyplněn");
        Assert.notEmpty(names, "Musí být vyplněno alespoň jedno jméno");

        Iterator<ApName> namesIterator = names.iterator();
        ApName preferredName = namesIterator.next();

        ApChangeNeed change = new ApChangeNeed(ApChange.Type.AP_SYNCH);

        ApState apState;
        ApAccessPoint accessPoint;

        // pokud není uložen v DB, zakládáme nový
        if (apStateCmp.getStateId() == null) {

            apState = createAccessPoint(apStateCmp.getScope(), apStateCmp.getApType(), change.get());
            accessPoint = apState.getAccessPoint();

            createName(apState, true, preferredName.getName(), preferredName.getComplement(), preferredName.getLanguage(), change.get(), true);
            while (namesIterator.hasNext()) {
                ApName name = namesIterator.next();
                createName(apState, false, name.getName(), name.getComplement(), name.getLanguage(), change.get(), true);
            }

            publishAccessPointCreateEvent(accessPoint);

        } else {

            apState = apStateCmp;
            accessPoint = apState.getAccessPoint();

            List<ApName> existsNames = apNameRepository.findByAccessPoint(accessPoint);

            Iterator<ApName> existsNamesIterator = existsNames.iterator();
            ApName existsPreferredName = existsNamesIterator.next();

            List<ApName> newNames = new ArrayList<>();
            newNames.add(updateAccessPointNameWhenChanged(change, apState, existsPreferredName, preferredName));

            int count = Math.max(existsNames.size(), names.size()) - 1;
            for (int i = 0; i < count; i++) {
                boolean eNHas = existsNamesIterator.hasNext();
                boolean nHas = namesIterator.hasNext();

                if (nHas && eNHas) { // pokud oba existují, aktualizujeme
                    ApName existsName = existsNamesIterator.next();
                    ApName apName = namesIterator.next();
                    newNames.add(updateAccessPointNameWhenChanged(change, apState, existsName, apName));
                } else if (nHas) { // pokud existuje pouze nový, zakládáme
                    ApName name = namesIterator.next();
                    newNames.add(createName(apState, false, name.getName(), name.getComplement(), name.getLanguage(), change.get(), false));
                } else if (eNHas) { // pokud existuje pouze v db, mažeme
                    ApName existsName = existsNamesIterator.next();
                    deleteName(apState, existsName, change.get());
                } else {
                    throw new IllegalStateException("Nesedí počty iterací!");
                }
            }

            for (ApName name : newNames) {
                apDataService.validationNameUnique(apState.getScope(), name.getFullName());
            }

            publishAccessPointUpdateEvent(accessPoint);
        }

        // sychronizace popisu
        if (description != null) {
            apDataService.changeDescription(apState, description.getDescription(), change.get());
        }

        reindexDescItem(accessPoint);

        return apState;
    }

    /**
     * Aktualizace jména přístupového bodu pokud bylo změněno - verzovaně.
     *
     * @param change změna
     * @param apState přístupový bod
     * @param existsName první jméno
     */
    private ApName updateAccessPointNameWhenChanged(final ApChangeNeed change, final ApState apState, final ApName existsName, final ApName newName) {
        if (apDataService.equalsNames(existsName, newName)) {
            return existsName;
        }
        String name = newName.getName();
        String complement = newName.getComplement();
        String fullName = AccessPointDataService.generateFullName(name, complement);
        return apDataService.updateAccessPointName(apState, existsName, name, complement, fullName, newName.getLanguage(), change.get(), false);
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
        itemIds.addAll(apAccessPointRepository.findItemIdByAccessPointIdOverDataPartyRef(accessPoint.getAccessPointId()));
        itemIds.addAll(apAccessPointRepository.findItemIdByAccessPointIdOverDataRecordRef(accessPoint.getAccessPointId()));
        descriptionItemService.reindexDescItem(itemIds);
    }

    /**
     * Vrátí preferovaná jména pro dané přístupové body
     *
     * @return seznam jmen přístupových bodů
     */
    public List<ApName> findPreferredNamesByAccessPointIds(Collection<Integer> accessPointIds) {
        if (accessPointIds.isEmpty()) {
            return Collections.emptyList();
        }
        return apNameRepository.findPreferredNamesByAccessPointIds(accessPointIds);
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

        boolean validateParty = apDataService.hasParty(accessPoint);
        if (validateParty && newApType != null) {
            apDataService.validationPartyType(accessPoint, newApType);
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
            accessPoint.setRuleSystem(newApType.getRuleSystem());
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
