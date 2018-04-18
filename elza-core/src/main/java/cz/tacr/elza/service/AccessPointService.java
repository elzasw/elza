package cz.tacr.elza.service;

import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.usage.FundVO;
import cz.tacr.elza.controller.vo.usage.NodeVO;
import cz.tacr.elza.controller.vo.usage.OccurrenceType;
import cz.tacr.elza.controller.vo.usage.OccurrenceVO;
import cz.tacr.elza.controller.vo.usage.PartyVO;
import cz.tacr.elza.controller.vo.usage.RecordUsageVO;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.ApScope;
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
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.repository.ApDescriptionRepository;
import cz.tacr.elza.repository.ApExternalIdRepository;
import cz.tacr.elza.repository.ApExternalSystemRepository;
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
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventNodeIdVersionInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.vo.ApAccessPointData;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;
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
import java.util.Set;
import java.util.UUID;
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
    private ApAccessPointRepository apAccessPointRepository;

    @Autowired
    private ApNameRepository apNameRepository;

    @Autowired
    private ApTypeRepository apTypeRepository;

    @Autowired
    private ApExternalSystemRepository apExternalSystemRepository;

    @Autowired
    private PartyService partyService;

    @Autowired
    private DataRecordRefRepository dataRecordRefRepository;

    @Autowired
    private NodeRegisterRepository nodeRegisterRepository;

    @Autowired
    private IEventNotificationService eventNotificationService;

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
    private BeanFactory beanFactory;

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
    private ApDescriptionRepository apDescriptionRepository;

    @Autowired
    private ApExternalIdRepository apExternalIdRepository;

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
                                                              @Nullable final Integer scopeId,
                                                              @Nullable final Boolean excludeInvalid) {

        Set<Integer> scopeIdsForSearch = getScopeIdsForSearch(fund, scopeId);

        return apAccessPointRepository.findApAccessPointByTextAndType(searchRecord, apTypeIds, firstResult,
                maxResults, scopeIdsForSearch, excludeInvalid);
    }


    /**
     * Celkový počet záznamů v DB pro funkci {@link #findApAccessPointByTextAndType(String, Collection, Integer, Integer, ArrFund, Integer, Boolean)}
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
                                                    final Integer scopeId,
                                                    final Boolean excludeInvalid) {

        Set<Integer> scopeIdsForSearch = getScopeIdsForSearch(fund, scopeId);

        return apAccessPointRepository.findApAccessPointByTextAndTypeCount(searchRecord, apTypeIds,
                scopeIdsForSearch, excludeInvalid);
    }

    /**
     * Kontrola, jestli je používáno rejstříkové heslo v navázaných tabulkách.
     *
     * @param record rejstříkové heslo
     * @param checkUsage kontrolovat použití
     *
     * @throws BusinessException napojení na jinou tabulku
     */
    public boolean canBeDeleted(final ApAccessPoint record, final boolean checkUsage) {
        long countDataRecordRef = dataRecordRefRepository.countAllByRecord(record);
        if (checkUsage) {
            ParParty parParty = partyService.findParPartyByAccessPoint(record);
            if (parParty != null) {
                throw new BusinessException("Existuje vazba z osoby, nelze smazat.", RegistryCode.EXIST_FOREIGN_PARTY);
            }

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

        return countDataRecordRef == 0 && nodeRegisterRepository.countByRecordId(record) == 0 && relationEntityRepository.countAllByAccessPoint(record) == 0;

    }

    /**
     * Uložení či update záznamu.
     *
     * @param accessPointData    naplněný objekt, bez vazeb
     * @param partySave true - jedná se o ukládání přes ukládání osoby, false -> editace z klienta
     * @return výsledný objekt
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ApAccessPoint saveAccessPoint(@AuthParam(type = AuthParam.Type.SCOPE) final ApAccessPointData accessPointData,
                                         final boolean partySave) {
        ApAccessPoint record = accessPointData.getAccessPoint();
        Assert.notNull(record, "Rejstříkové heslo musí být vyplněno");

        checkRecordSave(accessPointData, partySave);

        ApType apType = apTypeRepository.findOne(record.getApType().getApTypeId());
        record.setApType(apType);

        ApScope scope = scopeRepository.findOne(record.getScope().getScopeId());
        record.setScope(scope);


        ApExternalSystem externalSystem = accessPointData.getExternalSystem();
        if (externalSystem != null) {
            Integer externalSystemId = externalSystem.getExternalSystemId();
            Assert.notNull(externalSystemId, "ApExternalSystem nemá vyplněné ID.");
            externalSystem = apExternalSystemRepository.findOne(externalSystemId);
            Assert.notNull(externalSystem, "ApExternalSystem nebylo nalezeno podle id " + externalSystemId);
            accessPointData.setExternalSystem(externalSystem);
        }

        if (record.getUuid() == null) {
            record.setUuid(UUID.randomUUID().toString());
        }

        ApAccessPoint result = apAccessPointRepository.save(record);
        saveAccessPointDataVersion(accessPointData, result);

        EventType type = record.getAccessPointId() == null ? EventType.RECORD_CREATE : EventType.RECORD_UPDATE;
        eventNotificationService.publishEvent(EventFactory.createIdEvent(type, result.getAccessPointId()));

        return result;
    }

    /**
     * Uloží nové verze všech navázaných entit pro daný přístupový bod.
     *
     * @param accessPointData wrapper který obsahuje návazné entity
     * @param result pristupovy bod, na ktery se navazou navazne entity
     * @return nová verze wrapperu, která obsahuje všechny nové uložené verze navázaných entit
     */
    private ApAccessPointData saveAccessPointDataVersion(ApAccessPointData accessPointData, ApAccessPoint result) {
        // ap_description
        // ap_name
        // ap_external_id
        ApChange apChange;
        if (accessPointData.getAccessPointId() == null) {
            apChange = createChange(ApChange.Type.ACCESS_POINT_CREATE);
        } else {
            apChange = createChange(ApChange.Type.ACCESS_POINT_UPDATE);
        }
        // version description
        ApDescription description = apDescriptionRepository.findApDescriptionByAccessPoint(result);
        if (description != null) {
            description.setDeleteChange(apChange);
            apDescriptionRepository.save(description);
        }
        ApDescription descriptionCopy = new ApDescription(accessPointData.getDescription());
        descriptionCopy.setDescriptionId(null);
        descriptionCopy.setDeleteChange(null);
        descriptionCopy.setCreateChange(apChange);
        descriptionCopy.setAccessPoint(result);
        ApDescription savedDescription = apDescriptionRepository.save(descriptionCopy);

        // version name
        ApName name = apNameRepository.findPreferredNameByAccessPoint(result);
        if (name != null) {
            name.setDeleteChange(apChange);
            apNameRepository.save(name);
        }
        ApName nameCopy = new ApName(accessPointData.getPreferredName());
        nameCopy.setNameId(null);
        nameCopy.setDeleteChange(null);
        nameCopy.setCreateChange(apChange);
        nameCopy.setAccessPoint(result);
        ApName savedName = apNameRepository.save(nameCopy);

        ApExternalId externalId = apExternalIdRepository.findApExternalIdByAccessPoint(result);
        if (externalId != null) {
            externalId.setDeleteChange(apChange);
            externalId.setValue(null); // the external ID is not carried over between versions
            externalId.setAccessPoint(result);
            apExternalIdRepository.save(externalId);
        }

        accessPointData.setCharacteristics(savedDescription);
        accessPointData.setPreferredName(savedName);

        return accessPointData;
    }

    /**
     * Smaže rej. heslo a jeho variantní hesla. Předpokládá, že již proběhlo ověření, že je možné ho smazat (vazby atd...).
     * @param accessPoint heslo
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void deleteAccessPoint(@AuthParam(type = AuthParam.Type.SCOPE) final ApAccessPoint accessPoint, final boolean checkUsage) {
        if (canBeDeleted(accessPoint, checkUsage)) {
            eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.RECORD_DELETE, accessPoint.getAccessPointId()));

            ApChange change = createChange(ApChange.Type.ACCESS_POINT_DELETE);
            List<ApName> names = apNameRepository.findVariantNamesByAccessPointId(accessPoint);
            names.forEach(name -> name.setDeleteChange(change));
            apNameRepository.save(names);

            ApDescription apDescription = apDescriptionRepository.findApDescriptionByAccessPoint(accessPoint);
            apDescription.setDeleteChange(change);
            apDescriptionRepository.save(apDescription);

            ApExternalId apExternalId = apExternalIdRepository.findApExternalIdByAccessPoint(accessPoint);
            apExternalId.setDeleteChange(change);
            apExternalIdRepository.save(apExternalId);

            //TODO [fric] je tohle spravny postup? Nemuzu primo smazat zaznam v AP

            ApAccessPoint apAccessPoint = apAccessPointRepository.getOne(accessPoint.getAccessPointId());
            apAccessPoint.setInvalid(true);
            apAccessPointRepository.save(accessPoint);
        } else {
            ApAccessPointData accessPointData = accessPointDataService.findAccessPointData(accessPoint);
            accessPoint.setInvalid(true);
            saveAccessPoint(accessPointData, false);
        }
    }


    /**
     * Validace uložení záznamu.
     *
     * @param apData    heslo
     * @param partySave true - jedná se o ukládání přes ukládání osoby, false -> editace z klienta
     */
    private void checkRecordSave(final ApAccessPointData apData, final boolean partySave) {
        Assert.notNull(apData.getAccessPoint(), "Rejstříkové heslo musí být vyplněno");

        Assert.notNull(apData.getPreferredName(), "Není vyplněné Record.");

        ApAccessPoint record = apData.getAccessPoint();

        ApType apType = record.getApType();
        Assert.notNull(apType, "Není vyplněné ApType.");
        Assert.notNull(apType.getApTypeId(), "ApType nemá vyplněné ID.");
        apType = apTypeRepository.findOne(apType.getApTypeId());
        Assert.notNull(apType, "ApType nebylo nalezeno podle id " + apType.getApTypeId());

        if (partySave) {
            if (apType.getPartyType() == null) {
                throw new BusinessException("Typ hesla musí mít vazbu na typ osoby", RegistryCode.REGISTRY_HAS_NOT_TYPE_PARTY);
            }
        } else {
            if (record.getAccessPointId() == null && apType.getPartyType() != null) {
                throw new BusinessException("Nelze vytvořit rejstříkové heslo, které je navázané na typ osoby",
                        RegistryCode.CANT_CREATE_WITH_TYPE_PARTY);
            }
        }

        Assert.notNull(record.getScope(), "Není vyplněna třída rejstříku");
        Assert.notNull(record.getScope().getScopeId(), "Není vyplněno id třídy rejstříku");
        ApScope scope = scopeRepository.findOne(record.getScope().getScopeId());
        Assert.notNull(scope, "Nebyla nalezena třída rejstříku s id " + record.getScope().getScopeId());

        if (record.getAccessPointId() == null) {
            if (!apType.getAddRecord()) {
                throw new BusinessException(
                        "Nelze přidávat heslo do typu, který nemá přidávání hesel povolené.", RegistryCode.REGISTRY_TYPE_DISABLE);
            }
        } else {
            ApAccessPoint dbRecord = apAccessPointRepository.findOne(record.getAccessPointId());
            ApAccessPointData dbApData = accessPointDataService.findAccessPointData(dbRecord);
            if (!record.getScope().getScopeId().equals(dbRecord.getScope().getScopeId())) {
                throw new BusinessException("Nelze změnit třídu rejstříku.", RegistryCode.SCOPE_CANT_CHANGE);
            }

            ParParty party = partyService.findParPartyByAccessPoint(dbRecord);
            if (party == null) {
                ExceptionUtils.nullElseBusiness(apType.getPartyType(),
                        "Nelze nastavit typ hesla, které je navázané na typ osoby.", RegistryCode.CANT_CHANGE_WITH_TYPE_PARTY);
            } else {
                ExceptionUtils.equalsElseBusiness(apType.getPartyType(), party.getPartyType(),
                        "Nelze změnit typ rejstříkového hesla osoby, který odkazuje na jiný typ osoby.",
                        RegistryCode.CANT_CREATE_WITH_OTHER_TYPE_PARTY);

                //pokud editujeme heslo přes insert/update, a ne přes ukládání osoby
                if (!partySave) {
                    ExceptionUtils.equalsElseBusiness(apData.getPreferredName(), dbApData.getPreferredName(),
                            "Nelze editovat hodnotu rejstříkového hesla napojeného na osobu.",
                            RegistryCode.CANT_CHANGE_VALUE_WITH_PARTY);
                    ExceptionUtils.equalsElseBusiness(apData.getDescription(), dbApData.getDescription(),
                            "Nelze editovat charakteristiku rejstříkového hesla napojeného na osobu.",
                            RegistryCode.CANT_CHANGE_CHAR_WITH_PARTY);
                    ExceptionUtils.equalsElseBusiness(apData.getExternalId(), dbApData.getExternalId(),
                            "Nelze editovat externí id rejstříkového hesla napojeného na osobu.",
                            RegistryCode.CANT_CHANGE_EXID_WITH_PARTY);
                    ExceptionUtils.equalsElseBusiness(apData.getExternalSystem(), dbApData.getExternalSystem(),
                            "Nelze editovat externí systém rejstříkového hesla, které je napojené na osobu.",
                            RegistryCode.CANT_CHANGE_EXSYS_WITH_PARTY);
                }

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

        accessPoint = apAccessPointRepository.findOne(recordId);
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

        List<ApAccessPoint> scopeRecords = apAccessPointRepository.findByScope(scope);
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

    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_RD_ALL, UsrPermission.Permission.AP_SCOPE_RD})
    public ApAccessPoint getAccessPoint(@AuthParam(type = AuthParam.Type.REGISTRY) final Integer recordId) {
        Assert.notNull(recordId, "Identifikátor rejstříkového hesla musí být vyplněn");
        return apAccessPointRepository.findOne(recordId);
    }

    /**
     * Získání variant record
     * včetně oprávnění
     *
     * @param variantNameId variant record id
     * @return variant record
     */
    public ApName getVariantName(final Integer variantNameId) {
        ApName variantName = apNameRepository.getOneCheckExist(variantNameId);
        beanFactory.getBean(AccessPointService.class).getAccessPoint(variantName.getAccessPoint().getAccessPointId());
        return variantName;
    }


    /**
     * Smazání variant record
     *
     * @param variantName variant record ke smazání
     * @param record record z důvodu oprávnění
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void deleteVariantName(final ApName variantName, @AuthParam(type = AuthParam.Type.REGISTRY) final ApAccessPoint record) {
        ApChange change = createChange(ApChange.Type.VARIANT_NAME_DELETE);
        ApName apName = apNameRepository.getOneCheckExist(variantName.getNameId());
        apName.setDeleteChange(change);
        apNameRepository.save(apName);
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
            ApAccessPointData apData = accessPointDataService.findAccessPointData(partyRel.getRecord());
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
                    ApAccessPointData apData = accessPointDataService.findAccessPointData(creator.getRecord());
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
                if (!fundScopes.contains(replacement.getApScope().getScopeId())) {
                    throw new BusinessException("Nelze nahradit rejsříkové heslo v AS jelikož AS nemá scope rejstříku pomocí kterého nahrazujeme.", BaseCode.INVALID_STATE)
                            .set("fundId", fundId)
                            .set("scopeId", replacement.getApScope().getScopeId());
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
                if (!fundScopes.contains(replacement.getApScope().getScopeId())) {
                    throw new BusinessException("Nelze nahradit rejsříkové heslo v AS jelikož AS nemá scope rejstříku pomocí kterého nahrazujeme.", BaseCode.INVALID_STATE)
                            .set("fundId", fundId)
                            .set("scopeId", replacement.getApScope().getScopeId());
                }
            }
            self.updateRegisterLink(fundVersions.get(fundId).getFundVersionId(), i.getNodeId(), arrNodeRegister);
        });
    }

    public boolean canBeDeleted(ApAccessPoint record) {
        return CollectionUtils.isEmpty(dataRecordRefRepository.findByRecord(record)) &&
                CollectionUtils.isEmpty(nodeRegisterRepository.findByRecordId(record)) &&
                CollectionUtils.isEmpty(relationEntityRepository.findByAccessPoint(record));
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
}
