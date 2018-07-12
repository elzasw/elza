package cz.tacr.elza.service;

import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.usage.*;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ExceptionUtils;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.packageimport.xml.SettingRecord;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventNodeIdVersionInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.vo.ApAccessPointData;
import cz.tacr.elza.service.vo.ImportAccessPoint;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Servisní třída pro registry.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
@Service
public class AccessPointService {

    @Autowired
    private ApAccessPointRepository apRepository;

    @Autowired
    private ApNameRepository apNameRepository;

    @Autowired
    private ApTypeRepository apTypeRepository;

    @Autowired
    private PartyService partyService;

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
    private AccessPointDataService accessPointDataService;

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
     * Kontrola, jestli je používáno rejstříkové heslo v navázaných tabulkách.
     *
     * @param record rejstříkové heslo
     *
     * @throws BusinessException napojení na jinou tabulku
     */
    public void checkDeletion(final ApAccessPoint record) {
        ParParty parParty = partyService.findParPartyByAccessPoint(record);
        if (parParty != null) {
            throw new BusinessException("Existuje vazba z osoby, nelze smazat.", RegistryCode.EXIST_FOREIGN_PARTY);
        }

        long countDataRecordRef = dataRecordRefRepository.countAllByRecord(record);
        if (countDataRecordRef > 0) {
            throw new BusinessException("Nalezeno použití hesla v tabulce ArrDataRecordRef.", RegistryCode.EXIST_FOREIGN_DATA).set("table", "ArrDataRecordRef");
        }

        long countNodeRegister = nodeRegisterRepository.countByRecordAndDeleteChangeIsNull(record);
        if (countNodeRegister > 0) {
            throw new BusinessException("Nalezeno použití hesla v tabulce ArrNodeRegister.", RegistryCode.EXIST_FOREIGN_DATA).set("table", "ArrNodeRegister");
        }

        // vztah osoby par_relation_entity
        List<ParRelationEntity> relationEntities = relationEntityRepository.findActiveByRecord(record);
        if (CollectionUtils.isNotEmpty(relationEntities)) {
            throw new BusinessException("Nelze smazat/zneplatnit rejstříkové heslo na kterou mají vazbu jiné aktivní osoby v relacích.", RegistryCode.EXIST_FOREIGN_DATA)
                    .set("recordId", record.getAccessPointId())
                    .set("relationEntities", relationEntities.stream().map(ParRelationEntity::getRelationEntityId).collect(Collectors.toList()))
                    .set("partyIds", relationEntities.stream().map(ParRelationEntity::getRelation).map(ParRelation::getParty).map(ParParty::getPartyId).collect(Collectors.toList()));
        }
    }

    /**
     * Smaže rej. heslo a jeho variantní hesla. Předpokládá, že již proběhlo ověření, že je možné ho smazat (vazby atd...).
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void deleteAccessPoint(@AuthParam(type = AuthParam.Type.AP) final int accessPointId, final boolean checkUsage) {
        ApChange change = createChange(ApChange.Type.AP_DELETE);

        ApAccessPoint ap = apRepository.getOne(accessPointId);
        if (checkUsage) {
            checkDeletion(ap);
        }
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

        eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.RECORD_DELETE, accessPointId));
    }


    /**
     * Validace uložení záznamu.
     *
     * @param apData    heslo
     */
    private void checkRecordSave(final ApAccessPointData apData) {
        Assert.notNull(apData.getAccessPoint(), "Rejstříkové heslo musí být vyplněno");

        Assert.notNull(apData.getPreferredName(), "Není vyplněné Record.");

        ApAccessPoint record = apData.getAccessPoint();

        ApType apType = record.getApType();
        Assert.notNull(apType, "Není vyplněné ApType.");
        Assert.notNull(apType.getApTypeId(), "ApType nemá vyplněné ID.");
        apType = apTypeRepository.findOne(apType.getApTypeId());
        Assert.notNull(apType, "ApType nebylo nalezeno podle id " + apType.getApTypeId());

        if (record.getAccessPointId() == null && apType.getPartyType() != null) {
            throw new BusinessException("Nelze vytvořit rejstříkové heslo, které je navázané na typ osoby",
                    RegistryCode.CANT_CREATE_WITH_TYPE_PARTY);
        }

        Assert.notNull(record.getScope(), "Není vyplněna třída rejstříku");
        Assert.notNull(record.getScope().getScopeId(), "Není vyplněno id třídy rejstříku");
        ApScope scope = scopeRepository.findOne(record.getScope().getScopeId());
        Assert.notNull(scope, "Nebyla nalezena třída rejstříku s id " + record.getScope().getScopeId());

        if (record.getAccessPointId() == null) {
            if (apType.isReadOnly()) {
                throw new BusinessException(
                        "Nelze přidávat heslo do typu, který nemá přidávání hesel povolené.", RegistryCode.REGISTRY_TYPE_DISABLE);
            }
        } else {
            ApAccessPoint dbRecord = apRepository.findOne(record.getAccessPointId());
            if (!record.getScope().getScopeId().equals(dbRecord.getScope().getScopeId())) {
                throw new BusinessException("Nelze změnit třídu rejstříku.", RegistryCode.SCOPE_CANT_CHANGE);
            }
        }
    }



    /**
     * Uložení či update variantního záznamu.
     *
     * @param variantName variantní záznam, bez vazeb
     * @return výslendný objekt uložený do db
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ApName saveVariantName(@AuthParam(type = AuthParam.Type.SCOPE) final ApName variantName, @NotNull ApChange createChange) {
        Assert.notNull(variantName, "Heslo musí být vyplněno");
        Assert.notNull(createChange, "Create change musí být vyplněno");

        ApAccessPoint accessPoint = variantName.getAccessPoint();
        Assert.notNull(accessPoint, "ApAccessPoint musí být vyplněno.");
        Integer recordId = accessPoint.getAccessPointId();
        Assert.notNull(recordId, "ApAccessPoint nemá vyplněno ID.");

        accessPoint = apRepository.findOne(recordId);
        Assert.notNull(accessPoint, "ApAccessPoint nebylo nalezeno podle id " + recordId);
        variantName.setAccessPoint(accessPoint);
        variantName.setCreateChange(createChange);
        ApName saved = apNameRepository.save(variantName);
        eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.RECORD_UPDATE, recordId));

        return saved;
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
			ParParty partyRel = rel.getRelation().getParty();
			PartyVO partyVO = new PartyVO();
			partyVO.setId(partyRel.getPartyId());
            ApAccessPointData apData = accessPointDataService.findAccessPointData(partyRel.getAccessPoint());
            partyVO.setName(apData.getPreferredName().getName());

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
                    ApAccessPointData apData = accessPointDataService.findAccessPointData(creator.getAccessPoint());
                    partyVO.setName(apData.getPreferredName().getName());

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


    public ApChange createChange(@Nullable final ApChange.Type type) {
        return createChange(type, null);
    }

    public ApChange createChange(@Nullable final ApChange.Type type, @Nullable ApExternalSystem externalSystem) {
        ApChange change = new ApChange();
        UserDetail userDetail = userService.getLoggedUserDetail();
        change.setChangeDate(LocalDateTime.now());

        if (userDetail != null && userDetail.getId() != null) {
            UsrUser user = em.getReference(UsrUser.class, userDetail.getId());
            change.setUser(user);
        }

        change.setType(type);
        change.setExternalSystem(externalSystem);

        return apChangeRepository.save(change);
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
                                           @Nullable final String name,
                                           @Nullable final String complement,
                                           @Nullable final SysLanguage language,
                                           @Nullable final String description) {
        Assert.notNull(scope, "Třída musí být vyplněna");
        Assert.notNull(type, "Typ musí být vyplněn");

        ApChange change = createChange(ApChange.Type.AP_CREATE);
        ApAccessPoint accessPoint = createAccessPoint(scope, type, change);

        // založení hlavního jména
        createName(accessPoint, true, name, complement, language, change);

        if (description != null) {
            createDescription(accessPoint, description, change);
        }

        return accessPoint;
    }

    /**
     * Aktualizace přístupového bodu - není verzované!
     *
     * @param accessPoint přístupový bod
     * @param type        měněný typ přístupového bodu
     * @return upravený přístupový bodu
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ApAccessPoint updateAccessPoint(@AuthParam(type = AuthParam.Type.AP) final ApAccessPoint accessPoint,
                                           final ApType type) {
        Assert.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        Assert.notNull(type, "Typ musí být vyplněn");
        validationNotDeleted(accessPoint);
        if (type.getApTypeId().equals(accessPoint.getApType().getApTypeId())) {
            return accessPoint;
        }
        accessPoint.setApType(type);
        return apRepository.save(accessPoint);
    }

    /**
     * Změna popisu přístupového bodu.
     * Podle vstupních a aktuálních dat se rozhodne, zda-li se bude popis mazat, vytvářet nebo jen upravovat - verzovaně.
     *
     * @param accessPoint přístupový bod
     * @param description popis přístupového bodu
     * @return
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ApAccessPoint changeDescription(@AuthParam(type = AuthParam.Type.AP) final ApAccessPoint accessPoint,
                                           @Nullable final String description) {
        Assert.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        validationNotDeleted(accessPoint);

        // aktuálně platný popis přístupového bodu
        ApDescription apDescription = descriptionRepository.findByAccessPoint(accessPoint);

        if (StringUtils.isBlank(description)) {
            if (apDescription != null) {
                ApChange change = createChange(ApChange.Type.DESC_DELETE);
                apDescription.setDeleteChange(change);
                descriptionRepository.save(apDescription);
            }
        } else {
            if (apDescription != null) {
                ApDescription apDescriptionNew = new ApDescription(apDescription);
                ApChange change = createChange(ApChange.Type.DESC_UPDATE);
                apDescription.setDeleteChange(change);
                descriptionRepository.save(apDescription);

                apDescriptionNew.setCreateChange(change);
                apDescriptionNew.setDescription(description);
                descriptionRepository.save(apDescriptionNew);
            } else {
                ApChange change = createChange(ApChange.Type.DESC_CREATE);
                createDescription(accessPoint, description, change);
            }
        }

        return accessPoint;
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
                                        @Nullable final String name,
                                        @Nullable final String complement,
                                        @Nullable final SysLanguage language) {
        Assert.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        validationNotDeleted(accessPoint);

        return createName(accessPoint, false, name, complement, language, null);
    }

    /**
     * Aktualizace jména přístupového bodu - verzovaně.
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
                                        @Nullable final String name,
                                        @Nullable final String complement,
                                        @Nullable final SysLanguage language) {
        Assert.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        Assert.notNull(apName, "Upravované jméno musí být vyplněno");
        validationNotDeleted(accessPoint);
        validationNotDeleted(apName);

        ApChange change = createChange(ApChange.Type.NAME_UPDATE);

        // zneplatnění původní verze jména
        ApName apNameNew = new ApName(apName);
        apName.setDeleteChange(change);
        apNameRepository.save(apName);

        // založení nové verze jména
        apNameNew.setCreateChange(change);
        apNameNew.setName(name);
        apNameNew.setComplement(complement);
        apNameNew.setLanguage(language);

        return apNameRepository.save(apNameNew);
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
        Assert.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        Assert.notNull(name, "Upravované jméno musí být vyplněno");

        validationNotDeleted(accessPoint);
        validationNotDeleted(name);

        if (name.isPreferredName()) {
            throw new BusinessException("Nelze mazat preferované jméno", RegistryCode.CANT_DELETE_PREFERRED_NAME).set("nameId", name.getNameId());
        }

        ApChange change = createChange(ApChange.Type.NAME_DELETE);
        name.setDeleteChange(change);
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
        Assert.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        Assert.notNull(name, "Upravované jméno musí být vyplněno");

        validationNotDeleted(accessPoint);
        validationNotDeleted(name);

        // pokud je jméno již jako preferované, není třeba cokoliv dělat
        if (name.isPreferredName()) {
            return;
        }

        ApChange change = createChange(ApChange.Type.NAME_UPDATE);
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
     * @param nameId identifikátor jména
     * @return jméno
     */
    public ApName getName(final Integer nameId) {
        ApName name = apNameRepository.findOne(nameId);
        if (name == null) {
            throw new ObjectNotFoundException("Jméno přístupového bodu neexistuje", BaseCode.ID_NOT_EXIST).setId(nameId);
        }
        return name;
    }

    /**
     * Vyhledání všech jazyků seřazených podle kódu.
     *
     * @return nalezené jazyky
     */
    public List<SysLanguage> findAllLanguagesOrderByCode() {
        return sysLanguageRepository.findAll(new Sort(Sort.Direction.ASC, SysLanguage.CODE));
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
        SysLanguage language = sysLanguageRepository.findByCode(languageCode);
        if (language == null) {
            throw new ObjectNotFoundException("Jazyk neexistuje", BaseCode.ID_NOT_EXIST).setId(languageCode);
        }
        return language;
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
        ApAccessPoint accessPoint = new ApAccessPoint();
        accessPoint.setUuid(UUID.randomUUID().toString());
        accessPoint.setApType(type);
        accessPoint.setScope(scope);
        accessPoint.setCreateChange(change);
        accessPoint.setDeleteChange(null);
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
                              @Nullable final String name,
                              @Nullable final String complement,
                              @Nullable final SysLanguage language,
                              @Nullable final ApChange change) {
        Assert.notNull(accessPoint, "Přístupový bod musí být vyplněn");

        ApChange createChange = change == null ? createChange(ApChange.Type.NAME_CREATE) : change;
        ApName apName = new ApName();
        apName.setName(name);
        apName.setComplement(complement);
        apName.setPreferredName(preferredName);
        apName.setLanguage(language);
        apName.setAccessPoint(accessPoint);
        apName.setCreateChange(createChange);
        return apNameRepository.save(apName);
    }

    /**
     * Založení popisu.
     *
     * @param accessPoint přístupový bod
     * @param description popis přístupového bodu
     * @param change změna
     */
    private void createDescription(final ApAccessPoint accessPoint,
                                            final String description,
                                            @Nullable final ApChange change) {
        Assert.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        Assert.notNull(description, "Popis musí být vyplněn");

        ApChange createChange = change == null ? createChange(ApChange.Type.DESC_CREATE) : change;
        ApDescription apDescription = new ApDescription();
        apDescription.setDescription(description);
        apDescription.setCreateChange(createChange);
        apDescription.setAccessPoint(accessPoint);

        descriptionRepository.save(apDescription);
    }

    /**
     * Validace přístupového bodu, že není smazaný.
     *
     * @param accessPoint přístupový bod
     */
    private void validationNotDeleted(final ApAccessPoint accessPoint) {
        if (accessPoint.getDeleteChange() != null) {
            throw new BusinessException("Nelze upravit přístupový bod", RegistryCode.CANT_CHANGE_DELETED_AP)
                    .set("accessPointId", accessPoint.getAccessPointId())
                    .set("uuid", accessPoint.getUuid());
        }
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

        ApChange change = createChange(ApChange.Type.AP_IMPORT, externalSystem);

        ApExternalIdType externalIdType = staticDataService.getData().getApEidTypeByCode(externalIdTypeCode);
        ApAccessPoint accessPointExists = apRepository.findApAccessPointByExternalIdAndExternalSystemCodeAndScope(externalId, externalIdType.getExternalIdTypeId(), scope);

        ApAccessPoint accessPoint;
        if (accessPointExists == null) {
            accessPoint = createAccessPoint(scope, type, change);
            if (StringUtils.isNotEmpty(description)) {
                createDescription(accessPoint, description, change);
            }
            createExternalId(accessPoint, externalIdType, externalId, change);
        } else {
            accessPoint = updateAccessPoint(accessPointExists, type);
            invalidateAllNames(accessPoint, change);
            changeDescription(accessPoint, description);
        }

        // založení preferovaného jména
        createName(accessPoint, true, preferredName.getName(), preferredName.getComplement(), preferredName.getLanguage(), change);

        // založení další jmen
        if (CollectionUtils.isNotEmpty(names)) {
            for (ImportAccessPoint.Name name : names) {
                createName(accessPoint, false, name.getName(), name.getComplement(), name.getLanguage(), change);
            }
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
}
