package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

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
import cz.tacr.elza.domain.ApBodyItem;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApExternalIdType;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.ApNameItem;
import cz.tacr.elza.domain.ApRuleSystem;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.SysLanguage;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ExceptionUtils;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.packageimport.xml.SettingRecord;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBodyItemRepository;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.repository.ApDescriptionRepository;
import cz.tacr.elza.repository.ApExternalIdRepository;
import cz.tacr.elza.repository.ApNameItemRepository;
import cz.tacr.elza.repository.ApNameRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.DataPartyRefRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FundRegisterScopeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PartyCreatorRepository;
import cz.tacr.elza.repository.RelationEntityRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.SettingsRepository;
import cz.tacr.elza.repository.SysLanguageRepository;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventNodeIdVersionInVersion;
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
    private ApAccessPointRepository apRepository;

    @Autowired
    private ApNameRepository apNameRepository;

    @Autowired
    private ApTypeRepository apTypeRepository;

    @Autowired
    private DataRecordRefRepository dataRecordRefRepository;

    @Autowired
    private NodeRegisterRepository nodeRegisterRepository;

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
    private SettingsRepository settingsRepository;

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
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private EntityManager em;

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
    private ApBodyItemRepository bodyItemRepository;

    @Autowired
    private AccessPointItemService apItemService;

    @Autowired
    private AccessPointGeneratorService apGeneratorService;

    @Autowired
    private SequenceService sequenceService;

    /**
     * Kody tříd rejstříků nastavené v konfiguraci elzy.
     */
    private List<String> scopeCodes;

    /**
     * Nalezne takové záznamy rejstříku, které mají daný typ a jejich textová pole (record, charateristics, comment),
     * nebo pole variantního záznamu obsahují hledaný řetězec. V případě, že hledaný řetězec je null, nevyhodnocuje se.
     *
     * @param searchRecord    hledaný řetězec, může být null
     * @param apTypeIds typ záznamu
     * @param firstResult     index prvního záznamu, začíná od 0
     * @param maxResults      počet výsledků k vrácení
     * @param fund            AP, ze které se použijí třídy rejstříků
     * @param scopeId         id scope, pokud je vyplněno hledají se rejstříky pouze s tímto scope
     * @return vybrané záznamy dle popisu seřazené za record, nbeo prázdná množina
     */
    public List<ApAccessPoint> findApAccessPointByTextAndType(@Nullable final String searchRecord,
                                                              @Nullable final Collection<Integer> apTypeIds,
                                                              final Integer firstResult,
                                                              final Integer maxResults,
                                                              @Nullable final ArrFund fund,
                                                              @Nullable final Integer scopeId) {

        Set<Integer> scopeIdsForSearch = getScopeIdsForSearch(fund, scopeId);

        return apRepository.findApAccessPointByTextAndType(searchRecord, apTypeIds, firstResult,
                maxResults, scopeIdsForSearch);
    }


    /**
     * Celkový počet záznamů v DB pro funkci {@link #findApAccessPointByTextAndType}
     *
     * @param searchRecord    hledaný řetězec, může být null
     * @param apTypeIds typ záznamu
     * @param fund   AP, ze které se použijí třídy rejstříků
     * @param scopeId scope, pokud je vyplněno hledají se rejstříky pouze s tímto scope
     * @return celkový počet záznamů, který je v db za dané parametry
     */
    public long findApAccessPointByTextAndTypeCount(@Nullable final String searchRecord,
                                                    @Nullable final Collection<Integer> apTypeIds,
                                                    @Nullable final ArrFund fund,
                                                    final Integer scopeId) {

        Set<Integer> scopeIdsForSearch = getScopeIdsForSearch(fund, scopeId);

        return apRepository.findApAccessPointByTextAndTypeCount(searchRecord, apTypeIds,
                scopeIdsForSearch);
    }

    /**
     * Kontrola, jestli je používán přístupový bod v navázaných tabulkách.
     *
     * @param accessPoint přístupový bod
     *
     * @throws BusinessException napojení na jinou tabulku
     */
    public void checkDeletion(final ApAccessPoint accessPoint) {
        apDataService.validationNotParty(accessPoint);

        long countDataRecordRef = dataRecordRefRepository.countAllByRecord(accessPoint);
        if (countDataRecordRef > 0) {
            throw new BusinessException("Nalezeno použití AP v tabulce ArrDataRecordRef.", RegistryCode.EXIST_FOREIGN_DATA).set("table", "ArrDataRecordRef");
        }

        long countNodeRegister = nodeRegisterRepository.countByRecordAndDeleteChangeIsNull(accessPoint);
        if (countNodeRegister > 0) {
            throw new BusinessException("Nalezeno použití AP v tabulce ArrNodeRegister.", RegistryCode.EXIST_FOREIGN_DATA).set("table", "ArrNodeRegister");
        }

        // vztah osoby par_relation_entity
        List<ParRelationEntity> relationEntities = relationEntityRepository.findActiveByRecord(accessPoint);
        if (CollectionUtils.isNotEmpty(relationEntities)) {
            throw new BusinessException("Nelze smazat/zneplatnit AP na který mají vazbu jiné aktivní osoby v relacích.", RegistryCode.EXIST_FOREIGN_DATA)
                    .set("recordId", accessPoint.getAccessPointId())
                    .set("relationEntities", relationEntities.stream().map(ParRelationEntity::getRelationEntityId).collect(Collectors.toList()))
                    .set("partyIds", relationEntities.stream().map(ParRelationEntity::getRelation).map(ParRelation::getParty).map(ParParty::getPartyId).collect(Collectors.toList()));
        }
    }

    /**
     * Smaže rej. heslo a jeho variantní hesla. Předpokládá, že již proběhlo ověření, že je možné ho smazat (vazby atd...).
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void deleteAccessPoint(@AuthParam(type = AuthParam.Type.AP) final int accessPointId, final boolean checkUsage) {

        ApAccessPoint ap = apRepository.findOne(accessPointId);
        apDataService.validationNotDeleted(ap);
        if (ap.getState() == ApState.TEMP) {
            removeTempAccessPoint(ap);
        } else {
            if (checkUsage) {
                checkDeletion(ap);
            }
            ApChange change = apDataService.createChange(ApChange.Type.AP_DELETE);
            ap.setDeleteChange(change);
            apRepository.save(ap);

            List<ApName> names = apNameRepository.findByAccessPoint(ap);
            names.forEach(name -> name.setDeleteChange(change));
            apNameRepository.save(names);

            ApDescription desc = descriptionRepository.findByAccessPoint(ap);
            // can be without description
            if (desc != null) {
                desc.setDeleteChange(change);
                descriptionRepository.save(desc);
            }

            List<ApExternalId> eids = externalIdRepository.findByAccessPoint(ap);
            eids.forEach(eid -> eid.setDeleteChange(change));
            externalIdRepository.save(eids);

            publishAccessPointDeleteEvent(ap);
        }
    }

    /**
     * Uložení třídy rejstříku.
     *
     * @param scope třída k uložení
     * @return uložená třída
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL})
    public ApScope saveScope(final ApScope scope) {
        Assert.notNull(scope, "Scope musí být vyplněn");
        checkScopeSave(scope);

        if (scope.getScopeId() == null) {
            return scopeRepository.save(scope);
        } else {
            ApScope targetScope = scopeRepository.findOne(scope.getScopeId());
            targetScope.setName(scope.getName());
            return scopeRepository.save(targetScope);
        }
    }

    /**
     * Smazání třídy rejstříku.
     *
     * @param scope třída rejstříku
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL})
    public void deleteScope(final ApScope scope) {
        Assert.notNull(scope, "Scope musí být vyplněn");
        Assert.notNull(scope.getScopeId(), "Identifikátor scope musí být vyplněn");

        List<ApAccessPoint> scopeRecords = apRepository.findByScope(scope);
        ExceptionUtils.isEmptyElseBusiness(scopeRecords, "Nelze smazat třídu rejstříku, která je nastavena na rejstříku.", RegistryCode.USING_SCOPE_CANT_DELETE);

        fundRegisterScopeRepository.delete(fundRegisterScopeRepository.findByScope(scope));
        scopeRepository.delete(scope);
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

        List<ApScope> scopes = scopeRepository.findByCodes(Arrays.asList(scope.getCode()));
        ApScope codeScope = scopes.isEmpty() ? null : scopes.get(0);
        if (scope.getScopeId() == null) {
            ExceptionUtils.isEmptyElseBusiness(scopes, "Kod třídy rejstříku se již nachází v databázi.", RegistryCode.SCOPE_EXISTS);
        } else {
            if (codeScope == null) {
                throw new ObjectNotFoundException("Záznam pro editaci nebyl nalezen.", BaseCode.ID_NOT_EXIST);
            }

            ExceptionUtils.equalsElseBusiness(codeScope.getScopeId(), scope.getScopeId(), "Kod třídy rejstříku se již nachází v databázi.", RegistryCode.SCOPE_EXISTS);

            ApScope dbScope = scopeRepository.getOneCheckExist(scope.getScopeId());
            ExceptionUtils.equalsElseBusiness(dbScope.getCode(), scope.getCode(), "Třídě rejstříku nelze změnít kód, pouze název.", RegistryCode.SCOPE_CODE_CANT_CHANGE);
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
    			scopeIdsToSearch.removeIf( id -> !id.equals(scopeId));
			}
		}

        return scopeIdsToSearch;
    }

    /**
     * Vrátí vazby mezi uzlem a rejstříkovými hesly za danou verzi.
     *
     * @param fundVersionId   identifikátor verze AP
     * @param nodeId                identifikátor JP
     * @return  seznam vazeb, může být prázdný
     */
    public List<ArrNodeRegister> findRegisterLinks(final Integer fundVersionId,
                                                   final Integer nodeId) {
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");
        Assert.notNull(nodeId, "Identifikátor JP musí být vyplněn");

        ArrNode node = nodeRepository.findOne(nodeId);

        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);

        Assert.notNull(version, "Verze AP neexistuje");

        boolean open = version.getLockChange() == null;

        if (open) {
            return nodeRegisterRepository.findByNodeAndDeleteChangeIsNull(node);
        } else {
            return nodeRegisterRepository.findClosedVersion(node, version.getLockChange().getChangeId());
        }
    }

    /**
     * Uložení uzlu - optimistické zámky
     *
     * @param node uzel
     * @param change
     * @return uložený uzel
     */
    private ArrNode saveNode(final ArrNode node, final ArrChange change) {
        node.setLastUpdate(change.getChangeDate());
        nodeRepository.save(node);
        nodeRepository.flush();
        return node;
    }

    /**
     * Vytvoření vazby rejstřík-jednotka popisu.
     *
     * @param versionId     identifikátor verze AP
     * @param nodeId        identifikátor JP
     * @param nodeRegister  vazba
     * @return  vazba
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrNodeRegister createRegisterLink(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer versionId,
                                              final Integer nodeId,
                                              final ArrNodeRegister nodeRegister) {
        Assert.notNull(nodeRegister, "Rejstříkové heslo musí být vyplněno");
        Assert.isNull(nodeRegister.getNodeRegisterId(), "Identifikátor hesla musí být vyplěn");

        ArrNode node = nodeRepository.findOne(nodeId);

        ArrChange change = arrangementService.createChange(ArrChange.Type.ADD_RECORD_NODE, node);

        node.setVersion(nodeRegister.getNode().getVersion());
        saveNode(node, change);

        validateNodeRegisterLink(nodeRegister);

        nodeRegister.setNode(node);
        nodeRegister.setCreateChange(change);
        eventNotificationService.publishEvent(new EventNodeIdVersionInVersion(EventType.FUND_RECORD_CHANGE, versionId, nodeRegister.getNode().getNodeId(), nodeRegister.getNode().getVersion()));

        nodeRegisterRepository.saveAndFlush(nodeRegister);
        arrangementCacheService.createNodeRegister(nodeId, nodeRegister);

        return nodeRegister;
    }

    /**
     * Upravení vazby rejstřík-jednotka popisu.
     *
     * @param versionId     identifikátor verze AP
     * @param nodeId        identifikátor JP
     * @param nodeRegister  vazba
     * @return  vazba
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrNodeRegister updateRegisterLink(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer versionId,
                                              final Integer nodeId,
                                              final ArrNodeRegister nodeRegister) {
        Assert.notNull(nodeRegister, "Rejstříkové heslo musí být vyplněno");
        Assert.notNull(nodeRegister.getNodeRegisterId(), "Identifikátor musí být vyplněn");

        ArrNodeRegister nodeRegisterDB = nodeRegisterRepository.findOne(nodeRegister.getNodeRegisterId());

        ArrNode node = nodeRepository.findOne(nodeId);

        ArrChange change = arrangementService.createChange(ArrChange.Type.UPDATE_RECORD_NODE, node);

        node.setVersion(nodeRegister.getNode().getVersion());
        saveNode(node, change);

        validateNodeRegisterLink(nodeRegister);
        validateNodeRegisterLink(nodeRegisterDB);

        nodeRegisterDB.setDeleteChange(change);
        nodeRegisterRepository.save(nodeRegisterDB);


        nodeRegister.setNodeRegisterId(null);
        nodeRegister.setNode(node);
        nodeRegister.setRecord(nodeRegister.getRecord());
        nodeRegister.setCreateChange(change);
        eventNotificationService.publishEvent(new EventNodeIdVersionInVersion(EventType.FUND_RECORD_CHANGE, versionId, nodeRegister.getNode().getNodeId(), nodeRegister.getNode().getVersion()));

        nodeRegisterRepository.save(nodeRegister);
        arrangementCacheService.changeNodeRegister(nodeId, nodeRegisterDB, nodeRegister);
        return nodeRegister;
    }

    /**
     * Smazání vazby rejstřík-jednotka popisu.
     *
     * @param versionId     identifikátor verze AP
     * @param nodeId        identifikátor JP
     * @param nodeRegister  vazba
     * @return  vazba
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrNodeRegister deleteRegisterLink(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer versionId,
                                              final Integer nodeId,
                                              final ArrNodeRegister nodeRegister) {
        Assert.notNull(nodeRegister, "Rejstříkové heslo musí být vyplněno");
        Assert.notNull(nodeRegister.getNodeRegisterId(), "Identifikátor musí být vyplněn");

        ArrNodeRegister nodeRegisterDB = nodeRegisterRepository.findOne(nodeRegister.getNodeRegisterId());

        ArrNode node = nodeRepository.findOne(nodeId);

        ArrChange change = arrangementService.createChange(ArrChange.Type.DELETE_RECORD_NODE, node);

        node.setVersion(nodeRegister.getNode().getVersion());
        saveNode(node, change);

        validateNodeRegisterLink(nodeRegisterDB);

        nodeRegisterDB.setDeleteChange(change);

        eventNotificationService.publishEvent(new EventNodeIdVersionInVersion(EventType.FUND_RECORD_CHANGE, versionId, node.getNodeId(), node.getVersion()));

        nodeRegisterRepository.save(nodeRegisterDB);
        arrangementCacheService.deleteNodeRegister(nodeId, nodeRegisterDB.getNodeRegisterId());
        return nodeRegisterDB;
    }

    /**
     * Validuje entitu před uložením.
     *
     * @param nodeRegister  entita
     */
    private void validateNodeRegisterLink(final ArrNodeRegister nodeRegister) {
        if (nodeRegister.getDeleteChange() != null) {
            throw new IllegalStateException("Nelze vytvářet či modifikovat změnu," +
                    " která již byla smazána (má delete change).");
        }

        if (nodeRegister.getNode() == null) {
            throw new IllegalArgumentException("Není vyplněn uzel.");
        }
        if (nodeRegister.getRecord() == null) {
            throw new IllegalArgumentException("Není vyplněno rejstříkové heslo.");
        }
    }

    public List<String> getScopeCodes() {
        if (scopeCodes == null) {
            List<UISettings> uiSettingsList = settingsRepository.findByUserAndSettingsTypeAndEntityType(null, UISettings.SettingsType.RECORD, null);
            if (uiSettingsList.size() > 0) {
                uiSettingsList.forEach(uiSettings -> {
                    SettingRecord setting = (SettingRecord) PackageService.convertSetting(uiSettings, itemTypeRepository);
                    List<SettingRecord.ScopeCode> scopeCodes = setting.getScopeCodes();
                    if (CollectionUtils.isEmpty(scopeCodes)) {
                        this.scopeCodes = new ArrayList<>();
                    } else {
                        this.scopeCodes = scopeCodes.stream().map(SettingRecord.ScopeCode::getValue).collect(Collectors.toList());
                    }
                });
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
     *
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
     *
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
		}).collect(Collectors.toMap(PartyVO::getId, Function.identity()));

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
     *
     * @return použití rejstříku/osoby
	 */
	private List<FundVO> findUsedFunds(final ApAccessPoint record, final ParParty party) {
		List<ArrData> dataList = new LinkedList<>(dataRecordRefRepository.findByRecord(record));
		if (party != null) {
			dataList.addAll(dataPartyRefRepository.findByParty(party));
		}

		List<ArrNodeRegister> nodeRegisters = nodeRegisterRepository.findByRecordAndDeleteChangeIsNull(record);
		return createFundVOList(dataList, nodeRegisters);
	}

	/**
	 * Z předaných výskytů v archivních souborech vytvoří seznam {@link FundVO}.
	 *
	 * @param arrDataList hodnoty uzlů
	 * @param nodeRegisters vazba na uzly
	 */
	private List<FundVO> createFundVOList(final List<? extends ArrData> arrDataList, final List<ArrNodeRegister> nodeRegisters) {
		if (arrDataList.isEmpty() && nodeRegisters.isEmpty()) {
			return Collections.emptyList();
		}

		Map<Integer, List<ArrNodeRegister>> nodeIdToNodeRegisters = nodeRegisters.stream().collect(Collectors.groupingBy(ArrNodeRegister::getNodeId));
		Map<Integer, Set<Integer>> fundIdToNodeIdsMap = nodeRegisters.stream()
				.map(ArrNodeRegister::getNode)
				.collect(Collectors.groupingBy(ArrNode::getFundId, Collectors.mapping(ArrNode::getNodeId, Collectors.toSet())));

		Map<Integer, ? extends ArrData> dataIdToArrDataMap = arrDataList.stream().collect(Collectors.toMap(ArrData::getDataId, Function.identity()));

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
                .map(v -> createFundVO(nodeIdToNodeRegisters, fundIdToNodeIdsMap, dataIdToArrDataMap, nodeIdToDataIdsMap, v))
                .collect(Collectors.toList());
	}

	/**
	 * Vytvoří {@link FundVO}.
	 *
	 * @param nodeIdToNodeRegisters mapa id node na seznam vazeb {@link ArrNodeRegister}
	 * @param fundIdToNodeIdsMap id archivního souboru na množinu použitých uzlů
	 * @param dataIdToArrDataMap mapa id {@link ArrData} na konkrétní instanci
	 * @param nodeIdToDataIdsMap mapa id node na množinu {@link ArrData}
	 * @param fundVersion verze archivního souboru
	 *
	 * @return {@link FundVO}
	 */
	private FundVO createFundVO(final Map<Integer, List<ArrNodeRegister>> nodeIdToNodeRegisters,
			final Map<Integer, Set<Integer>> fundIdToNodeIdsMap, final Map<Integer, ? extends ArrData> dataIdToArrDataMap,
			final Map<Integer, Set<Integer>> nodeIdToDataIdsMap, final ArrFundVersion fundVersion) {
		Set<Integer> nodeIds = fundIdToNodeIdsMap.get(fundVersion.getFundId());
		List<Integer> nodeIdsSubList = getNodeIdsSublist(nodeIds);

		Collection<TreeNodeVO> treeNodes = levelTreeCacheService.getFaTreeNodes(fundVersion.getFundVersionId(), nodeIdsSubList);

		List<NodeVO> nodes = treeNodes.stream()
			.map(n ->  createNodeVO(nodeIdToNodeRegisters, dataIdToArrDataMap, nodeIdToDataIdsMap, n))
			.collect(Collectors.toList());

		ArrFund fund = fundVersion.getFund();
		return new FundVO(fund.getFundId(), fund.getName(), nodeIds.size(), nodes);
	}

	/**
	 * Vytvoří {@link NodeVO}.
	 *
	 * @param nodeIdToNodeRegisters mapa id node na seznam vazeb {@link ArrNodeRegister}
	 * @param dataIdToArrDataMap mapa id {@link ArrData} na konkrétní instanci
	 * @param nodeIdToDataIdsMap mapa id node na množinu {@link ArrData}
	 * @param node node
	 *
	 * @return {@link NodeVO}
	 */
	private NodeVO createNodeVO(final Map<Integer, List<ArrNodeRegister>> nodeIdToNodeRegisters,
			final Map<Integer, ? extends ArrData> dataIdToArrDataMap, final Map<Integer, Set<Integer>> nodeIdToDataIdsMap,
			final TreeNodeVO node) {
		List<OccurrenceVO> occurrences = new LinkedList<>();
		if (nodeIdToNodeRegisters.containsKey(node.getId())) {
			occurrences.addAll(createOccurrenceVOFromNodeRegisters(nodeIdToNodeRegisters.get(node.getId())));
		}

		if (nodeIdToDataIdsMap.containsKey(node.getId())) {
			occurrences.addAll(createOccurrenceVOFromData(dataIdToArrDataMap, nodeIdToDataIdsMap.get(node.getId())));
		}

		return new NodeVO(node.getId(), node.getName(), occurrences);
	}

	/**
	 * Získá podmnožinu id uzlů pokud jich je celkem více než počet který chceme vracet.
	 *
	 * @param nodeIds množina identifikátorů uzlů
	 *
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
	 *
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
		}).collect(Collectors.toList());
	}


	/**
	 * Vytvoří {@link OccurrenceVO} z předaných instancí {@link ArrNodeRegister}.
	 *
	 * @param nodeRegisters seznam vazeb
	 *
	 * @return seznam výskytů {@link OccurrenceVO}
	 */
	private List<OccurrenceVO> createOccurrenceVOFromNodeRegisters(final List<ArrNodeRegister> nodeRegisters) {
		return nodeRegisters.stream()
				.map(nr -> new OccurrenceVO(nr.getNodeRegisterId(), OccurrenceType.ARR_NODE_REGISTER))
				.collect(Collectors.toList());
	}

    /**
     * Replace record replaced by record replacement in all usages in JP, NodeRegisters
     * @param replaced
     * @param replacement
     */
    public void replace(final ApAccessPoint replaced, final ApAccessPoint replacement) {

        final List<ArrDescItem> arrItems = descItemRepository.findArrItemByRecord(replaced);
        List<ArrNodeRegister> nodeRegisters = nodeRegisterRepository.findByRecordAndDeleteChangeIsNull(replaced);

        final Set<ArrFund> arrFunds = nodeRegisters.stream().map(arrNodeRegister -> arrNodeRegister.getNode().getFund()).collect(Collectors.toSet());
        final Set<Integer> funds = arrFunds.stream().map(ArrFund::getFundId).collect(Collectors.toSet());

        // ArrItems + nodeRegisters
        final Collection<Integer> fundsAll = arrItems.stream().map(ArrDescItem::getFundId).collect(Collectors.toSet());
        fundsAll.addAll(funds);

        // fund to scopes
        Map<Integer, Set<Integer>> fundIdsToScopes = fundsAll.stream().collect(Collectors.toMap(Function.identity(), scopeRepository::findIdsByFundId));

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
                    .collect(Collectors.toMap(ArrFundVersion::getFundId, Function.identity()));
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
                if (!fundScopes.contains(replacement.getScopeId())) {
                    throw new BusinessException("Nelze nahradit rejsříkové heslo v AS jelikož AS nemá scope rejstříku pomocí kterého nahrazujeme.", BaseCode.INVALID_STATE)
                            .set("fundId", fundId)
                            .set("scopeId", replacement.getScopeId());
                }
            }
            descriptionItemService.updateDescriptionItem(im, fundVersions.get(fundId), change, true);
        });


        final AccessPointService self = applicationContext.getBean(AccessPointService.class);
        nodeRegisters.forEach(i -> {
            final ArrNodeRegister arrNodeRegister = new ArrNodeRegister();
            arrNodeRegister.setRecord(replacement);
            arrNodeRegister.setNode(i.getNode());
            arrNodeRegister.setCreateChange(i.getCreateChange());
            arrNodeRegister.setDeleteChange(i.getDeleteChange());
            arrNodeRegister.setNodeRegisterId(i.getNodeRegisterId());

            Integer fundId = i.getNode().getFundId();
            Set<Integer> fundScopes = fundIdsToScopes.get(fundId);
            if (fundScopes == null) {
                throw new SystemException("Pro AS neexistují žádné scope.", BaseCode.INVALID_STATE)
                        .set("fundId", fundId);
            } else {
                if (!fundScopes.contains(replacement.getScopeId())) {
                    throw new BusinessException("Nelze nahradit rejsříkové heslo v AS jelikož AS nemá scope rejstříku pomocí kterého nahrazujeme.", BaseCode.INVALID_STATE)
                            .set("fundId", fundId)
                            .set("scopeId", replacement.getScopeId());
                }
            }
            self.updateRegisterLink(fundVersions.get(fundId).getFundVersionId(), i.getNodeId(), arrNodeRegister);
        });
    }

    public boolean canBeDeleted(ApAccessPoint record) {
        return dataRecordRefRepository.findByRecord(record).isEmpty() &&
               nodeRegisterRepository.findByRecordId(record).isEmpty() &&
               relationEntityRepository.findByAccessPoint(record).isEmpty();
    }

    /**
     * Založení nového přístupového bodu.
     *
     * @param scope       třída přístupového bodu
     * @param type        typ přístupového bodu
     * @param name        jméno přístupového bodu
     * @param complement  doplněk přístupového bodu
     * @param language    jazyk jména
     * @param description popis přístupového bodu
     * @return založený přístupový bod
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ApAccessPoint createAccessPoint(@AuthParam(type = AuthParam.Type.SCOPE) final ApScope scope,
                                           final ApType type,
                                           final String name,
                                           @Nullable final String complement,
                                           @Nullable final SysLanguage language,
                                           @Nullable final String description) {
        Assert.notNull(scope, "Třída musí být vyplněna");
        Assert.notNull(type, "Typ musí být vyplněn");

        apDataService.validateNotStructureType(type);

        ApChange change = apDataService.createChange(ApChange.Type.AP_CREATE);
        ApAccessPoint accessPoint = createAccessPoint(scope, type, change);

        // založení hlavního jména
        createName(accessPoint, true, name, complement, language, change, true);

        if (description != null) {
            apDataService.createDescription(accessPoint, description, change);
        }

        publishAccessPointCreateEvent(accessPoint);
        return accessPoint;
    }

    /**
     * Založení strukturovaného přístupového bodu.
     *
     * @param scope    třída
     * @param type     typ přístupového bodu
     * @param language jazyk hlavního jména
     * @return založený strukturovaný přístupový bod
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ApAccessPoint createStructuredAccessPoint(final ApScope scope, final ApType type, final SysLanguage language) {
        Assert.notNull(scope, "Třída musí být vyplněna");
        Assert.notNull(type, "Typ musí být vyplněn");
        Assert.notNull(type.getRuleSystem(), "Typ musí mít vazbu na pravidla");

        apDataService.validateStructureType(type);

        ApChange change = apDataService.createChange(ApChange.Type.AP_CREATE);
        ApAccessPoint accessPoint = createStrucuredAccessPoint(scope, type, change);

        // založení strukturovaného hlavního jména
        createStructuredName(accessPoint, true, language, change);

        return accessPoint;
    }

    /**
     * Aktualizace přístupového bodu - není verzované!
     *
     * @param accessPointId
     *            ID přístupového bodu
     * @param type
     *            měněný typ přístupového bodu
     * @return upravený přístupový bodu
     */
    @Transactional
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ApAccessPoint changeApType(@AuthParam(type = AuthParam.Type.AP) final Integer accessPointId,
                                      final Integer apTypeId) {
        Validate.notNull(accessPointId);
        Validate.notNull(apTypeId);

        // get ap type
        StaticDataProvider sdp = this.staticDataService.createProvider();
        ApType apType = sdp.getApTypeById(apTypeId);
        Validate.notNull(apType, "AP Type not found, id={}", apTypeId);

        // get ap
        ApAccessPoint accessPoint = getAccessPoint(accessPointId);
        Validate.notNull(accessPoint, "AP not found, id={}", accessPointId);

        apDataService.validationNotDeleted(accessPoint);
        apDataService.validationNotParty(accessPoint);

        // check if modified
        if (apTypeId.equals(accessPoint.getApTypeId())) {
            return accessPoint;
        }
        accessPoint.setApType(apType);
        accessPoint.setRuleSystem(apType.getRuleSystem());
        ApAccessPoint result = apRepository.save(accessPoint);
        if (result.getRuleSystem() != null) {
            ApChange change = apDataService.createChange(ApChange.Type.AP_UPDATE);
            //apGeneratorService.generateAndSetResult(accessPoint, change);
            apGeneratorService.generateAsyncAfterCommit(accessPointId, change.getChangeId());
        }

        publishAccessPointUpdateEvent(result);

        return result;
    }

    /**
     * Změna popisu přístupového bodu.
     * Podle vstupních a aktuálních dat se rozhodne, zda-li se bude popis mazat, vytvářet nebo jen upravovat - verzovaně.
     *
     * @param accessPoint přístupový bod
     * @param description popis přístupového bodu
     * @return přístupový bod
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ApAccessPoint changeDescription(@AuthParam(type = AuthParam.Type.AP) final ApAccessPoint accessPoint,
                                           @Nullable final String description) {
        Validate.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        apDataService.validationNotDeleted(accessPoint);
        apDataService.validationNotParty(accessPoint);

        if (accessPoint.getRuleSystem() != null) {
            throw new BusinessException("Nelze upravovat charakteristiku u strukturovaného přístupového bodu",
                    BaseCode.INVALID_STATE);
        }

        apDataService.changeDescription(accessPoint, description, null);
        publishAccessPointUpdateEvent(accessPoint);
        return accessPoint;
    }

    /**
     * Změna atributů jména.
     *
     * @param accessPoint přístupový bod
     * @param name        jméno
     * @param items       položky změny
     * @return nové položky, které ze vytvořili při změně
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public List<ApItem> changeNameItems(@AuthParam(type = AuthParam.Type.AP) final ApAccessPoint accessPoint,
                                        final ApName name,
                                        final List<ApUpdateItemVO> items) {
        Validate.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        Validate.notNull(name, "Jméno musí být vyplněno");
        Validate.notEmpty(items, "Musí být alespoň jedna položka ke změně");
        apDataService.validationNotDeleted(accessPoint);
        apDataService.validationNotDeleted(name);

        List<ApNameItem> itemsDb = nameItemRepository.findValidItemsByName(name);

        ApChange change;
        if (name.getState() == ApState.TEMP) {
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
    public void migrateApItems(@AuthParam(type = AuthParam.Type.AP) final ApAccessPoint accessPoint,
                               final List<ApUpdateItemVO> apItems,
                               final Map<ApName, List<ApUpdateItemVO>> nameItemsMap) {
        Validate.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        apDataService.validationMigrateAp(accessPoint);
        Validate.notNull(apItems);
        Validate.notNull(nameItemsMap);

        Set<ApName> apNames = nameItemsMap.keySet();
        for (ApName name : apNames) {
            name.setState(ApState.INIT);
        }
        apNameRepository.save(apNames);

        ApRuleSystem ruleSystem = accessPoint.getApType().getRuleSystem();
        accessPoint.setRuleSystem(ruleSystem);
        accessPoint.setState(ApState.INIT);
        apRepository.save(accessPoint);

        ApChange change = apDataService.createChange(ApChange.Type.AP_MIGRATE);

        List<ApBodyItem> itemsDbAp = bodyItemRepository.findValidItemsByAccessPoint(accessPoint);
        apItemService.changeItems(apItems, new ArrayList<>(itemsDbAp), change, (RulItemType it, RulItemSpec is, ApChange c, int objectId, int position)
                -> createBodyItem(accessPoint, it, is, c, objectId, position));

        List<ApNameItem> itemsDbNames = apNames.isEmpty() ? Collections.emptyList() : nameItemRepository.findValidItemsByNames(apNames);
        Map<Integer, List<ApNameItem>> nameApNameItemMap = itemsDbNames.stream().collect(Collectors.groupingBy(ApNameItem::getNameId));

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
     * @param accessPoint přístupový bod
     * @param name        jméno ap
     * @param itemType    typ atributu
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void deleteNameItemsByType(@AuthParam(type = AuthParam.Type.AP) final ApAccessPoint accessPoint,
                                      final ApName name,
                                      final RulItemType itemType) {
        Validate.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        Validate.notNull(name, "Jméno musí být vyplněno");
        Validate.notNull(itemType, "Typ musí být vyplněn");
        apDataService.validationNotDeleted(accessPoint);
        apDataService.validationNotDeleted(name);

        ApChange change;
        if (accessPoint.getState() == ApState.TEMP) {
            change = accessPoint.getCreateChange();
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
     * @param accessPoint přístupový bod
     * @param items       položky změny
     * @return nové položky, které ze vytvořili při změně
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public List<ApItem> changeApItems(@AuthParam(type = AuthParam.Type.AP) final ApAccessPoint accessPoint,
                                      final List<ApUpdateItemVO> items) {
        Validate.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        Validate.notEmpty(items, "Musí být alespoň jedna položka ke změně");
        apDataService.validationNotDeleted(accessPoint);

        List<ApBodyItem> itemsDb = bodyItemRepository.findValidItemsByAccessPoint(accessPoint);

        ApChange change;
        if (accessPoint.getState() == ApState.TEMP) {
            change = accessPoint.getCreateChange();
        } else {
            change = apDataService.createChange(ApChange.Type.AP_UPDATE);
        }
        List<ApItem> itemsCreated = apItemService.changeItems(items, new ArrayList<>(itemsDb), change, (RulItemType it, RulItemSpec is, ApChange c, int objectId, int position)
                -> createBodyItem(accessPoint, it, is, c, objectId, position));

        //apGeneratorService.generateAndSetResult(accessPoint, change);
        apGeneratorService.generateAsyncAfterCommit(accessPoint.getAccessPointId(), change.getChangeId());

        return itemsCreated;
    }

    /**
     * Smazání hodnot ap podle typu.
     *
     * @param accessPoint přístupový bod
     * @param itemType    typ atributu
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void deleteApItemsByType(@AuthParam(type = AuthParam.Type.AP) final ApAccessPoint accessPoint,
                                    final RulItemType itemType) {
        Validate.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        Validate.notNull(itemType, "Typ musí být vyplněn");
        apDataService.validationNotDeleted(accessPoint);

        ApChange change;
        if (accessPoint.getState() == ApState.TEMP) {
            change = accessPoint.getCreateChange();
        } else {
            change = apDataService.createChange(ApChange.Type.AP_UPDATE);
        }
        apItemService.deleteItemsByType(bodyItemRepository, accessPoint, itemType, change);

        //apGeneratorService.generateAndSetResult(accessPoint, change);
        apGeneratorService.generateAsyncAfterCommit(accessPoint.getAccessPointId(), change.getChangeId());
    }

    /**
     * Vytvoření nepreferovaného jména přístupového bodu.
     *
     * @param accessPoint přístupový bod
     * @param name        jméno přístupového bodu
     * @param complement  doplněk přístupového bodu
     * @param language    jazyk jména
     * @return založené jméno
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ApName createAccessPointName(@AuthParam(type = AuthParam.Type.AP) final ApAccessPoint accessPoint,
                                        final String name,
                                        @Nullable final String complement,
                                        @Nullable final SysLanguage language) {
        Validate.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        apDataService.validationNotDeleted(accessPoint);

        return createName(accessPoint, false, name, complement, language, null, true);
    }

    /**
     * Založení strukturovaného jména přístupového bodu - dočasné, nutné potvrdit {@link #confirmAccessPointName}.
     *
     * @param accessPoint přístupový bod
     * @return založené strukturované jméno
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ApName createAccessPointStructuredName(@AuthParam(type = AuthParam.Type.AP) final ApAccessPoint accessPoint) {
        Validate.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        apDataService.validationNotDeleted(accessPoint);

        return createStructuredName(accessPoint, false, null, null);
    }

    /**
     * Potvrzení dočasného přístupového bodu a jeho převalidování.
     *
     * @param accessPoint přístupový bod
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void confirmAccessPoint(@AuthParam(type = AuthParam.Type.AP) final ApAccessPoint accessPoint) {
        Validate.notNull(accessPoint);
        apDataService.validationNotDeleted(accessPoint);

        if (accessPoint.getState() == ApState.TEMP) {
            accessPoint.setState(ApState.INIT);
            apRepository.save(accessPoint);

            ApName preferredName = apNameRepository.findPreferredNameByAccessPoint(accessPoint);
            preferredName.setState(ApState.INIT);
            apNameRepository.save(preferredName);

            //apGeneratorService.generateAndSetResult(accessPoint, accessPoint.getCreateChange());
            apGeneratorService.generateAsyncAfterCommit(accessPoint.getAccessPointId(), accessPoint.getCreateChangeId());
        } else {
            throw new BusinessException("Nelze potvrdit přístupový bod, který není dočasný", BaseCode.INVALID_STATE);
        }
    }

    /**
     * Nastaví pravidla přístupovému bodu podle typu.
     *
     * @param accessPoint přístupový bod
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void setRuleAccessPoint(@AuthParam(type = AuthParam.Type.AP) final ApAccessPoint accessPoint) {
        Validate.notNull(accessPoint);
        apDataService.validationNotDeleted(accessPoint);

        if (accessPoint.getRuleSystem() != null) {
            throw new BusinessException("Nelze AP přepnout do řízení pravidly, protože již pravidla má", BaseCode.INVALID_STATE);
        }

        ApType apType = accessPoint.getApType();
        if (apType.getRuleSystem() == null) {
            throw new BusinessException("Typ nemá vazbu na pravidla", BaseCode.INVALID_STATE);
        }

        accessPoint.setRuleSystem(apType.getRuleSystem());
        apRepository.save(accessPoint);

        ApChange change = apDataService.createChange(ApChange.Type.AP_UPDATE);
        //apGeneratorService.generateAndSetResult(accessPoint, change);
        apGeneratorService.generateAsyncAfterCommit(accessPoint.getAccessPointId(), change.getChangeId());
    }

    /**
     * Potvrzení dočasného jména a převalidování celého AP.
     *
     * @param accessPoint přístupový bod
     * @param name        jméno
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void confirmAccessPointName(@AuthParam(type = AuthParam.Type.AP) final ApAccessPoint accessPoint,
                                       final ApName name) {
        Validate.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        Validate.notNull(name, "Jméno musí být vyplněno");
        apDataService.validationNotDeleted(accessPoint);
        apDataService.validationNotDeleted(name);

        if (name.getState() == ApState.TEMP) {
            name.setState(ApState.INIT);
            apNameRepository.save(name);
            //apGeneratorService.generateAndSetResult(accessPoint, name.getCreateChange());
            apGeneratorService.generateAsyncAfterCommit(accessPoint.getAccessPointId(), name.getCreateChangeId());
        } else {
            throw new BusinessException("Nelze potvrdit jméno, které není dočasné", BaseCode.INVALID_STATE);
        }
    }

    /**
     * Aktualizace jména přístupového bodu - verzovaně.
     * Použití v případě, že se nejedná o strukturovaný popis.
     *
     * @param accessPoint přístupový bod
     * @param apName      upravované jméno přístupového bodu
     * @param name        jméno přístupového bodu
     * @param complement  doplněk přístupového bodu
     * @param language    jazyk jména
     * @return upravený jméno
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ApName updateAccessPointName(@AuthParam(type = AuthParam.Type.AP) final ApAccessPoint accessPoint,
                                        final ApName apName,
                                        final String name,
                                        @Nullable final String complement,
                                        @Nullable final SysLanguage language) {
        Validate.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        Validate.notNull(apName, "Upravované jméno musí být vyplněno");
        Validate.notNull(name, "Nové jméno musí být vyplněno");
        apDataService.validationNotDeleted(accessPoint);
        apDataService.validationNotDeleted(apName);
        apDataService.validationNotParty(accessPoint);

        if (accessPoint.getRuleSystem() != null) {
            throw new BusinessException("Nelze upravovat jméno u strukturovaného přístupového bodu",
                    BaseCode.INVALID_STATE);
        }

        ApChange change = apDataService.createChange(ApChange.Type.NAME_UPDATE);
        String fullName = AccessPointDataService.generateFullName(name, complement);
        ApName apNameNew = apDataService.updateAccessPointName(accessPoint, apName, name, complement, fullName, language, change, true);
        publishAccessPointUpdateEvent(accessPoint);
        return apNameNew;
    }

    /**
     * Aktualizace jména přístupového bodu - verzovaně.
     * Použití v případě, že jde o strukturovaný popis.
     *
     * @param accessPoint přístupový bod
     * @param apName      upravované jméno přístupového bodu
     * @param language    jazyk jména
     * @return upravený jméno
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ApName updateAccessPointName(@AuthParam(type = AuthParam.Type.AP) final ApAccessPoint accessPoint,
                                        final ApName apName,
                                        @Nullable final SysLanguage language) {
        Validate.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        Validate.notNull(apName, "Upravované jméno musí být vyplněno");
        apDataService.validationNotDeleted(accessPoint);
        apDataService.validationNotDeleted(apName);

        ApType apType = accessPoint.getApType();
        if (apType.getRuleSystem() == null) {
            throw new BusinessException("Nelze upravovat jméno u nestrukturovaného přístupového bodu",
                    BaseCode.INVALID_STATE);
        }

        ApChange change = apDataService.createChange(ApChange.Type.NAME_UPDATE);
        ApName newName = apDataService.updateAccessPointName(accessPoint, apName, apName.getName(), apName.getComplement(), apName.getFullName(), language, change, true);
        apItemService.copyItems(apName, newName, change);
        //apGeneratorService.generateAndSetResult(accessPoint, change);
        apGeneratorService.generateAsyncAfterCommit(accessPoint.getAccessPointId(), change.getChangeId());
        return newName;
    }

    /**
     * Smazání nepreferovaného přístupového bodu.
     *
     * @param accessPoint přístupový bod
     * @param name        mazané jméno
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void deleteAccessPointName(@AuthParam(type = AuthParam.Type.AP) final ApAccessPoint accessPoint,
                                      final ApName name) {
        Validate.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        Validate.notNull(name, "Upravované jméno musí být vyplněno");

        apDataService.validationNotDeleted(accessPoint);
        apDataService.validationNotDeleted(name);

        ApChange change = apDataService.createChange(ApChange.Type.NAME_DELETE);
        deleteName(accessPoint, name, change);

        if (accessPoint.getRuleSystem() != null) {
            //apGeneratorService.generateAndSetResult(accessPoint, change);
            apGeneratorService.generateAsyncAfterCommit(accessPoint.getAccessPointId(), change.getChangeId());
        }
        publishAccessPointUpdateEvent(accessPoint);
    }

    /**
     * Smazání nepreferovaného přístupového bodu.
     *
     * @param accessPoint  přístupový bod
     * @param name         mazané jméno
     * @param deleteChange změna pro smazání
     */
    private void deleteName(final @AuthParam(type = AuthParam.Type.AP) ApAccessPoint accessPoint,
                            final ApName name,
                            final ApChange deleteChange) {
        Validate.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        Validate.notNull(name, "Upravované jméno musí být vyplněno");
        Validate.notNull(deleteChange, "Změna pro mazání musí být vyplněna");

        apDataService.validationNotDeleted(accessPoint);
        apDataService.validationNotDeleted(name);

        if (name.isPreferredName()) {
            throw new BusinessException("Nelze mazat preferované jméno", RegistryCode.CANT_DELETE_PREFERRED_NAME).set("nameId", name.getNameId());
        }

        name.setDeleteChange(deleteChange);
        apNameRepository.save(name);
    }

    /**
     * Nastavení nepreferované jméno jako preferované přístupovému bodu.
     *
     * @param accessPoint přístupový bod
     * @param name        nastavované jméno
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void setPreferredAccessPointName(@AuthParam(type = AuthParam.Type.AP) final ApAccessPoint accessPoint,
                                            final ApName name) {
        Validate.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        Validate.notNull(name, "Upravované jméno musí být vyplněno");

        apDataService.validationNotDeleted(accessPoint);
        apDataService.validationNotDeleted(name);

        // pokud je jméno již jako preferované, není třeba cokoliv dělat
        if (name.isPreferredName()) {
            return;
        }

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

        ApType apType = accessPoint.getApType();
        if (apType.getRuleSystem() != null) {
            apItemService.copyItems(preferredNameOld, nameNew, change);
            apItemService.copyItems(name, preferredNameNew, change);
        }

        if (accessPoint.getRuleSystem() != null) {
            //apGeneratorService.generateAndSetResult(accessPoint, change);
            apGeneratorService.generateAsyncAfterCommit(accessPoint.getAccessPointId(), change.getChangeId());
        }
    }

    /**
     * Získání přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu
     * @return přístupový bod
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_RD_ALL, UsrPermission.Permission.AP_SCOPE_RD})
    public ApAccessPoint getAccessPoint(@AuthParam(type = AuthParam.Type.AP) final Integer accessPointId) {
        ApAccessPoint accessPoint = apRepository.findOne(accessPointId);
        if (accessPoint == null) {
            throw new ObjectNotFoundException("Přístupový bod neexistuje", BaseCode.ID_NOT_EXIST).setId(accessPointId);
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
    public ApAccessPoint getAccessPointInternalWithLock(@AuthParam(type = AuthParam.Type.AP) final Integer accessPointId) {
        ApAccessPoint accessPoint = apRepository.findOneWithLock(accessPointId);
        if (accessPoint == null) {
            throw new ObjectNotFoundException("Přístupový bod neexistuje", BaseCode.ID_NOT_EXIST).setId(accessPointId);
        }
        return accessPoint;
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
     * Odstranění dočasných AP včetně návazných objektů.
     */
    public void removeTempAccessPoints() {
        apItemService.removeTempItems();
        descriptionRepository.removeTemp();
        apNameRepository.removeTemp();
        List<ApChange> tempChanges = apChangeRepository.findTempChange();
        apRepository.removeTemp();
        if (tempChanges.size() > 0) {
            apChangeRepository.delete(tempChanges);
        }
    }

    /**
     * Odstranění dočasných AP včetně návazných objektů u AP.
     */
    private void removeTempAccessPoint(final ApAccessPoint ap) {
        apItemService.removeTempItems(ap);
        descriptionRepository.removeTemp(ap);
        apNameRepository.removeTemp(ap);
        apRepository.delete(ap);
        apChangeRepository.delete(ap.getCreateChange());
    }

    /**
     * Založení přístupového bodu.
     *
     * @param scope  třída
     * @param type   typ
     * @param change změna
     * @return přístupový bod
     */
    private ApAccessPoint createAccessPoint(final ApScope scope, final ApType type, final ApChange change) {
        ApAccessPoint accessPoint = createAccessPointEntity(scope, type, change);
        accessPoint.setState(ApState.OK);
        return apRepository.save(accessPoint);
    }

    /**
     * Založení dočasného strukturovaného přístupového bodu.
     *
     * @param scope  třída
     * @param type   typ přístupového bodu
     * @param change změna
     * @return založený a uložený AP
     */
    private ApAccessPoint createStrucuredAccessPoint(final ApScope scope, final ApType type, final ApChange change) {
        ApAccessPoint accessPoint = createAccessPointEntity(scope, type, change);
        accessPoint.setRuleSystem(type.getRuleSystem());
        accessPoint.setState(ApState.TEMP);
        return apRepository.save(accessPoint);
    }

    /**
     * Založení jména.
     *
     * @param accessPoint   přístupový bod
     * @param preferredName zda-li se jedná o preferované jméno
     * @param name          jméno přístupového bodu
     * @param complement    doplněk přístupového bodu
     * @param language      jazyk jména
     * @param change        změna
     * @return jméno
     */
    private ApName createName(final ApAccessPoint accessPoint,
                              final boolean preferredName,
                              final String name,
                              @Nullable final String complement,
                              @Nullable final SysLanguage language,
                              @Nullable final ApChange change,
                              final boolean validate) {
        Validate.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        Validate.notNull(name, "Jméno musí být vyplněno");

        ApChange createChange = change == null ? apDataService.createChange(ApChange.Type.NAME_CREATE) : change;
        ApName apName = createNameEntity(accessPoint, preferredName, name, complement, language, createChange);
        apNameRepository.save(apName);
        if (validate) {
            apDataService.validationNameUnique(accessPoint.getScope(), apName.getFullName());
        }
        return apName;
    }

    /**
     * Vytvoření strukturovaného jména.
     *
     * @param accessPoint   přístupový bod
     * @param preferredName zda-li se jedná o preferované jméno
     * @param language      jazyk jména
     * @param change        změna
     * @return založené a uložené jméno
     */
    private ApName createStructuredName(final ApAccessPoint accessPoint,
                                        final boolean preferredName,
                                        @Nullable final SysLanguage language,
                                        @Nullable final ApChange change) {
        Validate.notNull(accessPoint, "Přístupový bod musí být vyplněn");

        ApChange createChange = change == null ? apDataService.createChange(ApChange.Type.NAME_CREATE) : change;
        ApName apName = createNameEntity(accessPoint, preferredName, null, null, language, createChange);
        apName.setState(ApState.TEMP);

        return apNameRepository.save(apName);
    }

    /**
     * Vytvoření entity pro jméno přístupového bodu.
     *
     * @param accessPoint   přístupový bod
     * @param preferredName zda-li se jedná o preferované jméno
     * @param name          jméno přístupového bodu
     * @param complement    doplněk přístupového bodu
     * @param language      jazyk jména
     * @param createChange  zakládací změna
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
     * @param scope  třída
     * @param type   typ přístupového bodu
     * @param change změna
     * @return vytvořená entita AP
     */
    public static ApAccessPoint createAccessPointEntity(final ApScope scope, final ApType type, final ApChange change) {
        ApAccessPoint accessPoint = new ApAccessPoint();
        accessPoint.setUuid(UUID.randomUUID().toString());
        accessPoint.setApType(type);
        accessPoint.setScope(scope);
        accessPoint.setCreateChange(change);
        accessPoint.setDeleteChange(null);
        return accessPoint;
    }

    /**
     * Vytvoření entity hodnoty atributu jména.
     *
     * @param name     jméno pro který atribut tvoříme
     * @param it       typ atributu
     * @param is       specifikace atribututu
     * @param c        změna
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
     * @param it          typ atributu
     * @param is          specifikace atribututu
     * @param c           změna
     * @param objectId    jednoznačný identifikátor položky (nemění se při odverzování)
     * @param position    pozice
     * @return vytvořená položka
     */
    private ApItem createBodyItem(final ApAccessPoint accessPoint, final RulItemType it, final RulItemSpec is, final ApChange c, final int objectId, final int position) {
        ApBodyItem item = new ApBodyItem();
        item.setAccessPoint(accessPoint);
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
     * @param scope    třída
     * @param fullName validované jméno
     */
    private void validationNameUnique(final ApScope scope, final String fullName) {
        Assert.notNull(scope, "Přístupový bod musí být vyplněn");
        Assert.notNull(fullName, "Plné jméno musí být vyplněno");

        long count = apNameRepository.countUniqueName(fullName, scope);
        if (count > 1) {
            throw new BusinessException("Celé jméno není unikátní v rámci třídy", RegistryCode.NOT_UNIQUE_FULL_NAME)
                    .set("fullName", fullName)
                    .set("scopeId", scope.getScopeId());
        }
    }

    /**
     * Import přístupového bodu z externího systému.
     *
     * @param externalId         identifikátor přístupového bodu v externím systému
     * @param externalIdTypeCode kód typu externího systému
     * @param externalSystem     externí systém
     * @param data               data pro založení/aktualizaci přístupového bodu
     * @return přístupový bod
     */
    @Transactional
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ApAccessPoint importAccessPoint(final String externalId,
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
        ApAccessPoint accessPointExists = apRepository.findApAccessPointByExternalIdAndExternalSystemCodeAndScope(externalId, externalIdType.getExternalIdTypeId(), scope);

        ApAccessPoint accessPoint;
        if (accessPointExists == null) {
            accessPoint = createAccessPoint(scope, type, change);
            if (StringUtils.isNotEmpty(description)) {
                apDataService.createDescription(accessPoint, description, change);
            }
            createExternalId(accessPoint, externalIdType, externalId, change);
            publishAccessPointCreateEvent(accessPoint);
        } else {
            accessPoint = changeApType(accessPointExists.getAccessPointId(), type.getApTypeId());
            invalidateAllNames(accessPoint, change);
            apDataService.changeDescription(accessPoint, description, change);
            publishAccessPointUpdateEvent(accessPoint);
        }

        // kolekce pro kontrolu jmen vlastního přístupového bodu
        Set<String> uniqueNames = new HashSet<>();

        // založení preferovaného jména
        ApName nameCreated = createName(accessPoint, true, preferredName.getName(), preferredName.getComplement(), preferredName.getLanguage(), change, false);
        uniqueNames.add(nameCreated.getFullName().toLowerCase());

        List<ApName> newNames = new ArrayList<>();
        newNames.add(nameCreated);

        // založení další jmen
        if (CollectionUtils.isNotEmpty(names)) {
            for (ImportAccessPoint.Name name : names) {
                nameCreated = createName(accessPoint, false, name.getName(), name.getComplement(), name.getLanguage(), change, false);
                newNames.add(nameCreated);
                String compareName = nameCreated.getFullName().toLowerCase();
                if (uniqueNames.contains(compareName)) {
                    throw new BusinessException("Celé jméno není unikátní v rámci jmen přístupového bodu", RegistryCode.NOT_UNIQUE_FULL_NAME)
                            .set("fullName", nameCreated.getFullName())
                            .set("scopeId", accessPoint.getScopeId());
                }
                uniqueNames.add(compareName);
            }
        }

        for (ApName name : newNames) {
            apDataService.validationNameUnique(accessPoint.getScope(), name.getFullName());
        }

        return accessPoint;
    }

    /**
     * Invalidace všech jmen na přístupovém bodu.
     *
     * @param accessPoint přístupový bod
     * @param change      změna, která se nastaví na smazání jmen
     */
    private void invalidateAllNames(final ApAccessPoint accessPoint, final ApChange change) {
        apNameRepository.flush();
        apNameRepository.invalidateByAccessPointIdIn(Collections.singleton(accessPoint.getAccessPointId()), change);
    }

    /**
     * Založení externího identifikátoru přístupového bodu.
     *
     * @param accessPoint    přístupový bod
     * @param externalIdType typ externího systému
     * @param externalId     identifikátor v externím systému
     * @param change         změna ve které se identifikátor zakládá
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
     * @param accessPointCmp porovnávaný přístupový bod (v případě, že není uložen v DB, zakládáme nový)
     * @param names          jména přístupového bodu (musí existovat alespoň jedna hodnota - preferované jméno,
     *                       které je vždy první)
     * @param description    popis přístupového bodu
     * @return synchronizovaný přístupový bod
     */
    public ApAccessPoint syncAccessPoint(final ApAccessPoint accessPointCmp,
                                         final List<ApName> names,
                                         @Nullable final ApDescription description) {
        Assert.notNull(accessPointCmp, "Přístupový bod musí být vyplněn");
        Assert.notEmpty(names, "Musí být vyplněno alespoň jedno jméno");

        Iterator<ApName> namesIterator = names.iterator();
        ApName preferredName = namesIterator.next();

        ApChangeNeed change = new ApChangeNeed(ApChange.Type.AP_SYNCH);
        ApAccessPoint accessPoint;

        // pokud není uložen v DB, zakládáme nový
        if (accessPointCmp.getAccessPointId() == null) {
            accessPoint = createAccessPoint(accessPointCmp.getScope(), accessPointCmp.getApType(), change.get());
            createName(accessPoint, true, preferredName.getName(), preferredName.getComplement(), preferredName.getLanguage(), change.get(), true);
            while (namesIterator.hasNext()) {
                ApName name = namesIterator.next();
                createName(accessPoint, false, name.getName(), name.getComplement(), name.getLanguage(), change.get(), true);
            }

            publishAccessPointCreateEvent(accessPoint);
        } else {
            accessPoint = accessPointCmp;

            List<ApName> existsNames = apNameRepository.findByAccessPoint(accessPoint);

            Iterator<ApName> existsNamesIterator = existsNames.iterator();
            ApName existsPreferredName = existsNamesIterator.next();

            List<ApName> newNames = new ArrayList<>();
            newNames.add(updateAccessPointNameWhenChanged(change, accessPoint, existsPreferredName, preferredName));

            int count = Math.max(existsNames.size(), names.size()) - 1;
            for (int i = 0; i < count; i++) {
                boolean eNHas = existsNamesIterator.hasNext();
                boolean nHas = namesIterator.hasNext();

                if (nHas && eNHas) { // pokud oba existují, aktualizujeme
                    ApName existsName = existsNamesIterator.next();
                    ApName apName = namesIterator.next();
                    newNames.add(updateAccessPointNameWhenChanged(change, accessPoint, existsName, apName));
                } else if (nHas) { // pokud existuje pouze nový, zakládáme
                    ApName name = namesIterator.next();
                    newNames.add(createName(accessPoint, false, name.getName(), name.getComplement(), name.getLanguage(), change.get(), false));
                } else if (eNHas) { // pokud existuje pouze v db, mažeme
                    ApName existsName = existsNamesIterator.next();
                    deleteName(accessPoint, existsName, change.get());
                } else {
                    throw new IllegalStateException("Nesedí počty iterací!");
                }
            }

            for (ApName name : newNames) {
                apDataService.validationNameUnique(accessPoint.getScope(), name.getFullName());
            }

            publishAccessPointUpdateEvent(accessPoint);
        }

        // sychronizace popisu
        if (description != null) {
            apDataService.changeDescription(accessPoint, description.getDescription(), change.get());
        }

        return accessPoint;
    }

    /**
     * Aktualizace jména přístupového bodu pokud bylo změněno - verzovaně.
     *
     * @param change      změna
     * @param accessPoint přístupový bod
     * @param existsName       první jméno
     * @param newName
     */
    private ApName updateAccessPointNameWhenChanged(final ApChangeNeed change, final ApAccessPoint accessPoint, final ApName existsName, final ApName newName) {
        if (!apDataService.equalsNames(existsName, newName)) {
            String name = newName.getName();
            String complement = newName.getComplement();
            String fullName = AccessPointDataService.generateFullName(name, complement);
            return apDataService.updateAccessPointName(accessPoint, existsName, name, complement, fullName, newName.getLanguage(), change.get(), false);
        }
        return existsName;
    }

    /**
     * @return identifikátor pro nové jméno AP
     */
    public int nextNameObjectId() {
        return sequenceService.getNext(OBJECT_ID_SEQUENCE_NAME);
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
