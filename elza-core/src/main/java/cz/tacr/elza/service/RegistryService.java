package cz.tacr.elza.service;

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

import javax.annotation.Nullable;

import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.SystemException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.controller.vo.usage.FundVO;
import cz.tacr.elza.controller.vo.usage.NodeVO;
import cz.tacr.elza.controller.vo.usage.OccurrenceType;
import cz.tacr.elza.controller.vo.usage.OccurrenceVO;
import cz.tacr.elza.controller.vo.usage.PartyVO;
import cz.tacr.elza.controller.vo.usage.RecordUsageVO;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ExceptionUtils;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.packageimport.xml.SettingRecord;
import cz.tacr.elza.repository.DataPartyRefRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FundRegisterScopeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PartyCreatorRepository;
import cz.tacr.elza.repository.RegCoordinatesRepository;
import cz.tacr.elza.repository.RegExternalSystemRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegVariantRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RelationEntityRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.SettingsRepository;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventNodeIdVersionInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;


/**
 * Servisní třída pro registry.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
@Service
public class RegistryService {

    @Autowired
    private RegRecordRepository regRecordRepository;

    @Autowired
    private RegVariantRecordRepository variantRecordRepository;

    @Autowired
    private RegisterTypeRepository registerTypeRepository;

    @Autowired
    private RegExternalSystemRepository regExternalSystemRepository;

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
    private RegCoordinatesRepository regCoordinatesRepository;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private UserService userService;

    @Autowired
    private BeanFactory beanFactory;

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private PackageService packageService;

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

    /**
     * Kody tříd rejstříků nastavené v konfiguraci elzy.
     */
    private List<String> scopeCodes;

    /**
     * Nalezne takové záznamy rejstříku, které mají daný typ a jejich textová pole (record, charateristics, comment),
     * nebo pole variantního záznamu obsahují hledaný řetězec. V případě, že hledaný řetězec je null, nevyhodnocuje se.
     *
     * @param searchRecord    hledaný řetězec, může být null
     * @param registerTypeIds typ záznamu
     * @param firstResult     index prvního záznamu, začíná od 0
     * @param maxResults      počet výsledků k vrácení
     * @param parentRecordId  id rodičovského rejstříku
     * @param fund            AP, ze které se použijí třídy rejstříků
     * @param scopeId         id scope, pokud je vyplněno hledají se rejstříky pouze s tímto scope
     * @return vybrané záznamy dle popisu seřazené za record, nbeo prázdná množina
     */
    public List<RegRecord> findRegRecordByTextAndType(@Nullable final String searchRecord,
                                                      @Nullable final Collection<Integer> registerTypeIds,
                                                      final Integer firstResult,
                                                      final Integer maxResults,
                                                      final Integer parentRecordId,
                                                      @Nullable final ArrFund fund,
                                                      @Nullable final Integer scopeId) {

        Set<Integer> scopeIdsForSearch = getScopeIdsForSearch(fund, scopeId);

        RegRecord parentRecord = null;
        if (parentRecordId != null) {
            parentRecord = regRecordRepository.getOneCheckExist(parentRecordId);
        }

        return regRecordRepository.findRegRecordByTextAndType(searchRecord, registerTypeIds, firstResult,
                maxResults, parentRecord, scopeIdsForSearch);
    }


    /**
     * Celkový počet záznamů v DB pro funkci {@link #findRegRecordByTextAndType(String, Collection, Integer, Integer, Integer, ArrFund, Integer)}
     *
     * @param searchRecord    hledaný řetězec, může být null
     * @param registerTypeIds typ záznamu
     * @param parentRecordId  id rodičovského rejstříku
     * @param fund   AP, ze které se použijí třídy rejstříků
     * @param scopeId scope, pokud je vyplněno hledají se rejstříky pouze s tímto scope
     * @return celkový počet záznamů, který je v db za dané parametry
     */
    public long findRegRecordByTextAndTypeCount(@Nullable final String searchRecord,
                                                @Nullable final Collection<Integer> registerTypeIds,
                                                final Integer parentRecordId, @Nullable final ArrFund fund,
                                                final Integer scopeId) {

        Set<Integer> scopeIdsForSearch = getScopeIdsForSearch(fund, scopeId);

        RegRecord parentRecord = null;
        if (parentRecordId != null) {
            parentRecord = regRecordRepository.getOneCheckExist(parentRecordId);
        }

        return regRecordRepository.findRegRecordByTextAndTypeCount(searchRecord, registerTypeIds, parentRecord,
                scopeIdsForSearch);
    }

    /**
     * Kontrola, jestli je používáno rejstříkové heslo v navázaných tabulkách.
     *
     * @param record rejstříkové heslo
     * @throws IllegalStateException napojení na jinou tabulku
     */
    private void checkRecordUsage(final RegRecord record) {
        ParParty parParty = partyService.findParPartyByRecord(record);
        if (parParty != null) {
            throw new BusinessException("Existuje vazba z osoby, nelze smazat.", RegistryCode.EXIST_FOREIGN_PARTY);
        }

        List<ArrDataRecordRef> dataRecordRefList = dataRecordRefRepository.findByRecord(record);
        if (CollectionUtils.isNotEmpty(dataRecordRefList)) {
            throw new BusinessException("Nalezeno použití hesla v tabulce ArrDataRecordRef.", RegistryCode.EXIST_FOREIGN_DATA).set("table", "ArrDataRecordRef");
        }

        List<ArrNodeRegister> nodeRegisterList = nodeRegisterRepository.findByRecordId(record);
        if (CollectionUtils.isNotEmpty(nodeRegisterList)) {
            throw new BusinessException("Nalezeno použití hesla v tabulce ArrNodeRegister.", RegistryCode.EXIST_FOREIGN_DATA).set("table", "ArrNodeRegister");
        }

        List<RegRecord> childs = regRecordRepository.findByParentRecord(record);
        if (!childs.isEmpty()) {
            throw new BusinessException("Nelze smazat rejstříkové heslo, které má potomky.", RegistryCode.EXISTS_CHILD);
        }

    }

    /**
     * Uložení či update záznamu.
     *
     * @param record    naplněný objekt, bez vazeb
     * @param partySave true - jedná se o ukládání přes ukládání osoby, false -> editace z klienta
     * @return výsledný objekt
     */
    @AuthMethod(permission = {UsrPermission.Permission.REG_SCOPE_WR_ALL, UsrPermission.Permission.REG_SCOPE_WR})
    public RegRecord saveRecord(@AuthParam(type = AuthParam.Type.SCOPE) final RegRecord record,
                                final boolean partySave) {
        Assert.notNull(record, "Rejstříkové heslo musí být vyplněno");

        checkRecordSave(record, partySave);

        RegRegisterType registerType = registerTypeRepository.findOne(record.getRegisterType().getRegisterTypeId());
        record.setRegisterType(registerType);

        RegScope scope = scopeRepository.findOne(record.getScope().getScopeId());
        record.setScope(scope);

        RegExternalSystem externalSystem = record.getExternalSystem();
        if (externalSystem != null) {
            Integer externalSystemId = externalSystem.getExternalSystemId();
            Assert.notNull(externalSystemId, "RegExternalSystem nemá vyplněné ID.");
            externalSystem = regExternalSystemRepository.findOne(externalSystemId);
            Assert.notNull(externalSystem, "RegExternalSystem nebylo nalezeno podle id " + externalSystemId);
            record.setExternalSystem(externalSystem);
        }

        RegRecord parentRecord = null;
        if (record.getParentRecord() != null && record.getParentRecord().getRecordId() != null){
            parentRecord = regRecordRepository.findOne(record.getParentRecord().getRecordId());
            checkRecordCycle(record, parentRecord);
            record.setParentRecord(parentRecord);
        }


        if (record.getRecordId() != null){
            //při editaci typu kořenového hesla promítnout změnu na všechny potomky
            RegRecord dbRecord = regRecordRepository.findOne(record.getRecordId());
            if (dbRecord.getParentRecord() == null) {
                if (!ObjectUtils.equals(record.getRegisterType(), dbRecord.getRegisterType())) {
                    List<RegRecord> childs = regRecordRepository.findByParentRecord(dbRecord);
                    childs.forEach(child -> hierarchicalUpdateRegisterType(child, registerType));
                }
            }
        } else if (record.getUuid() == null) {
            record.setUuid(UUID.randomUUID().toString());
        }

        record.setLastUpdate(LocalDateTime.now());

        RegRecord result = regRecordRepository.save(record);
        EventType type = record.getRecordId() == null ? EventType.RECORD_CREATE : EventType.RECORD_UPDATE;
        eventNotificationService.publishEvent(EventFactory.createIdEvent(type, result.getRecordId()));

        return result;
    }

    private void hierarchicalUpdateRegisterType(final RegRecord record, final RegRegisterType type) {
        Assert.notNull(record, "Rejstříkové heslo musí být vyplněno");
        Assert.notNull(type, "Typ musí být vyplněn");

        record.setRegisterType(type);

        List<RegRecord> childs = regRecordRepository.findByParentRecord(record);
        childs.forEach(child -> hierarchicalUpdateRegisterType(child, type));
    }

    /**
     * Test, že nevkládáme rejstříkové heslo pod svého potomka.
     *
     * @param record    heslo
     * @param newParent nový rodič
     */
    private void checkRecordCycle(final RegRecord record, final RegRecord newParent) {
        RegRecord parent = newParent;
        while (parent != null) {
            if (parent.equals(record)) {
                throw new BusinessException("Nelze vložit pod potomka.", BaseCode.CYCLE_DETECT);
            }
            parent = parent.getParentRecord();
        }
    }

    /**
     * Smaže rej. heslo a jeho variantní hesla. Předpokládá, že již proběhlo ověření, že je možné ho smazat (vazby atd...).
     * @param record heslo
     */
    @AuthMethod(permission = {UsrPermission.Permission.REG_SCOPE_WR_ALL, UsrPermission.Permission.REG_SCOPE_WR})
    public void deleteRecord(@AuthParam(type = AuthParam.Type.SCOPE) final RegRecord record, final boolean checkUsage) {
        if (checkUsage) {
            checkRecordUsage(record);
        }

        if (canBeDeleted(record)) {
            eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.RECORD_DELETE, record.getRecordId()));

            variantRecordRepository.delete(variantRecordRepository.findByRegRecordId(record.getRecordId()));
            regCoordinatesRepository.delete(regCoordinatesRepository.findByRegRecordId(record.getRecordId()));
            regRecordRepository.delete(record);
        } else {
            record.setInvalid(true);
            saveRecord(record, false);
        }
    }


    /**
     * Validace uložení záznamu.
     *
     * @param record    heslo
     * @param partySave true - jedná se o ukládání přes ukládání osoby, false -> editace z klienta
     */
    private void checkRecordSave(final RegRecord record, final boolean partySave) {
        Assert.notNull(record, "Rejstříkové heslo musí být vyplněno");

        Assert.notNull(record.getRecord(), "Není vyplněné Record.");

        RegRegisterType regRegisterType = record.getRegisterType();
        Assert.notNull(regRegisterType, "Není vyplněné RegisterType.");
        Assert.notNull(regRegisterType.getRegisterTypeId(), "RegisterType nemá vyplněné ID.");
        regRegisterType = registerTypeRepository.findOne(regRegisterType.getRegisterTypeId());
        Assert.notNull(regRegisterType, "RegisterType nebylo nalezeno podle id " + regRegisterType.getRegisterTypeId());

        if (partySave) {
            if (regRegisterType.getPartyType() == null) {
                throw new BusinessException("Typ hesla musí mít vazbu na typ osoby", RegistryCode.REGISTRY_HAS_NOT_TYPE_PARTY);
            }
        } else {
            if (record.getRecordId() == null && regRegisterType.getPartyType() != null) {
                throw new BusinessException("Nelze vytvořit rejstříkové heslo, které je navázané na typ osoby",
                        RegistryCode.CANT_CREATE_WITH_TYPE_PARTY);
            }
        }

        Assert.notNull(record.getScope(), "Není vyplněna třída rejstříku");
        Assert.notNull(record.getScope().getScopeId(), "Není vyplněno id třídy rejstříku");
        RegScope scope = scopeRepository.findOne(record.getScope().getScopeId());
        Assert.notNull(scope, "Nebyla nalezena třída rejstříku s id " + record.getScope().getScopeId());

        RegRecord parentRecord = null;
        if (record.getParentRecord() != null && record.getParentRecord().getRecordId() != null) {
            parentRecord = regRecordRepository.findOne(record.getParentRecord().getRecordId());
            Assert.notNull(parentRecord,
                    "Nebylo nalezeno rejstříkové heslo s id " + record.getParentRecord().getRecordId());
        }

        if (parentRecord != null) {
            if (ObjectUtils.equals(parentRecord.getRecordId(), record.getRecordId())) {
                throw new BusinessException("Nelze nastavit rodiče rejstříkovému heslu sebe samotného.", RegistryCode.CANT_BE_SELF_PARENT);
            }

            ExceptionUtils.equalsElseBusiness(record.getRegisterType(), parentRecord.getRegisterType(),
                    "Potomek rejstříkového hesla musí mít stejný typ jako jeho rodič.", RegistryCode.CHILD_AND_PARENT_DIFFERENT_TYPE);
        }

        if (record.getRecordId() == null) {
            if (!regRegisterType.getAddRecord()) {
                throw new BusinessException(
                        "Nelze přidávat heslo do typu, který nemá přidávání hesel povolené.", RegistryCode.REGISTRY_TYPE_DISABLE);
            }

            if (parentRecord != null && !parentRecord.getRegisterType().getHierarchical()) {
                throw new BusinessException("Nelze přidávat heslo k rodiči, který není hierarchický.", RegistryCode.PARENT_IS_NOT_HIERARCHICAL);
            }
        } else {
            RegRecord dbRecord = regRecordRepository.findOne(record.getRecordId());
            if (!record.getScope().getScopeId().equals(dbRecord.getScope().getScopeId())) {
                throw new BusinessException("Nelze změnit třídu rejstříku.", RegistryCode.SCOPE_CANT_CHANGE);
            }

            List<RegRecord> childs = regRecordRepository.findByParentRecord(dbRecord);
            if (dbRecord.getRegisterType().getHierarchical() && !regRegisterType.getHierarchical() && !childs
                    .isEmpty()) {
                throw new BusinessException(
                        "Nelze změnit typ rejstříkového hesla na nehierarchický, pokud má heslo potomky."
                        , RegistryCode.HIERARCHICAL_RECORD_HAS_CHILDREN);
            }

            ParParty party = partyService.findParPartyByRecord(dbRecord);
            if (party == null) {
                ExceptionUtils.nullElseBusiness(regRegisterType.getPartyType(),
                        "Nelze nastavit typ hesla, které je navázané na typ osoby.", RegistryCode.CANT_CHANGE_WITH_TYPE_PARTY);
            } else {
                ExceptionUtils.equalsElseBusiness(regRegisterType.getPartyType(), party.getPartyType(),
                        "Nelze změnit typ rejstříkového hesla osoby, který odkazuje na jiný typ osoby.",
                        RegistryCode.CANT_CREATE_WITH_OTHER_TYPE_PARTY);

                //pokud editujeme heslo přes insert/update, a ne přes ukládání osoby
                if (!partySave) {
                    ExceptionUtils.equalsElseBusiness(record.getRecord(), dbRecord.getRecord(),
                            "Nelze editovat hodnotu rejstříkového hesla napojeného na osobu.",
                            RegistryCode.CANT_CHANGE_VALUE_WITH_PARTY);
                    ExceptionUtils.equalsElseBusiness(record.getCharacteristics(), dbRecord.getCharacteristics(),
                            "Nelze editovat charakteristiku rejstříkového hesla napojeného na osobu.",
                            RegistryCode.CANT_CHANGE_CHAR_WITH_PARTY);
                    ExceptionUtils.equalsElseBusiness(record.getExternalId(), dbRecord.getExternalId(),
                            "Nelze editovat externí id rejstříkového hesla napojeného na osobu.",
                            RegistryCode.CANT_CHANGE_EXID_WITH_PARTY);
                    ExceptionUtils.equalsElseBusiness(record.getExternalSystem(), dbRecord.getExternalSystem(),
                            "Nelze editovat externí systém rejstříkového hesla, které je napojené na osobu.",
                            RegistryCode.CANT_CHANGE_EXSYS_WITH_PARTY);
                }

            }
        }
    }



    /**
     * Uložení či update variantního záznamu.
     *
     * @param variantRecord variantní záznam, bez vazeb
     * @return výslendný objekt uložený do db
     */
    @AuthMethod(permission = {UsrPermission.Permission.REG_SCOPE_WR_ALL, UsrPermission.Permission.REG_SCOPE_WR})
    public RegVariantRecord saveVariantRecord(@AuthParam(type = AuthParam.Type.SCOPE) final RegVariantRecord variantRecord) {
        Assert.notNull(variantRecord, "Heslo musí být vyplněno");

        RegRecord regRecord = variantRecord.getRegRecord();
        Assert.notNull(regRecord, "RegRecord musí být vyplněno.");
        Integer recordId = regRecord.getRecordId();
        Assert.notNull(recordId, "RegRecord nemá vyplněno ID.");

        regRecord = regRecordRepository.findOne(recordId);
        Assert.notNull(regRecord, "RegRecord nebylo nalezeno podle id " + recordId);
        variantRecord.setRegRecord(regRecord);

        RegVariantRecord saved = variantRecordRepository.save(variantRecord);
        eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.RECORD_UPDATE, recordId));

        return saved;
    }

    public Map<RegRecord, List<RegRecord>> findChildren(final List<RegRecord> records) {
        Assert.notNull(records, "Musí být vyplněny hesla");

        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyMap();
        }

        Map<RegRecord, List<RegRecord>> result = new HashMap<>();
        regRecordRepository.findByParentRecords(records).forEach(record -> {
            RegRecord parent = record.getParentRecord();
            List<RegRecord> children = result.get(parent);
            if (children == null) {
                children = new LinkedList<>();
                result.put(parent, children);
            }
            children.add(record);
        });

        return result;
    }

    /**
     * Uložení třídy rejstříku.
     *
     * @param scope třída k uložení
     * @return uložená třída
     */
    @AuthMethod(permission = {UsrPermission.Permission.REG_SCOPE_WR_ALL})
    public RegScope saveScope(final RegScope scope) {
        Assert.notNull(scope, "Scope musí být vyplněn");
        checkScopeSave(scope);

        if (scope.getScopeId() == null) {
            return scopeRepository.save(scope);
        } else {
            RegScope targetScope = scopeRepository.findOne(scope.getScopeId());
            targetScope.setName(scope.getName());
            return scopeRepository.save(targetScope);
        }
    }

    /**
     * Smazání třídy rejstříku.
     *
     * @param scope třída rejstříku
     */
    @AuthMethod(permission = {UsrPermission.Permission.REG_SCOPE_WR_ALL})
    public void deleteScope(final RegScope scope) {
        Assert.notNull(scope, "Scope musí být vyplněn");
        Assert.notNull(scope.getScopeId(), "Identifikátor scope musí být vyplněn");

        List<RegRecord> scopeRecords = regRecordRepository.findByScope(scope);
        ExceptionUtils.isEmptyElseBusiness(scopeRecords, "Nelze smazat třídu rejstříku, která je nastavena na rejstříku.", RegistryCode.USING_SCOPE_CANT_DELETE);

        fundRegisterScopeRepository.delete(fundRegisterScopeRepository.findByScope(scope));
        scopeRepository.delete(scope);
    }

    /**
     * Kontrola uložení třídy rejstříku.
     *
     * @param scope ukládaná třída
     */
    private void checkScopeSave(final RegScope scope) {
        Assert.notNull(scope, "Scope musí být vyplněn");
        Assert.notNull(scope.getCode(), "Třída musí mít vyplněný kod");
        Assert.notNull(scope.getName(), "Třída musí mít vyplněný název");

        List<RegScope> scopes = scopeRepository.findByCodes(Arrays.asList(scope.getCode()));
        RegScope codeScope = scopes.isEmpty() ? null : scopes.get(0);
        if (scope.getScopeId() == null) {
            ExceptionUtils.isEmptyElseBusiness(scopes, "Kod třídy rejstříku se již nachází v databázi.", RegistryCode.SCOPE_EXISTS);
        } else {
            if (codeScope == null) {
                throw new ObjectNotFoundException("Záznam pro editaci nebyl nalezen.", BaseCode.ID_NOT_EXIST);
            }

            ExceptionUtils.equalsElseBusiness(codeScope.getScopeId(), scope.getScopeId(), "Kod třídy rejstříku se již nachází v databázi.", RegistryCode.SCOPE_EXISTS);

            RegScope dbScope = scopeRepository.getOneCheckExist(scope.getScopeId());
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
    	boolean readAllScopes = userService.hasPermission(UsrPermission.Permission.REG_SCOPE_RD_ALL);
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
                    SettingRecord setting = (SettingRecord) packageService.convertSetting(uiSettings, null);
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

    public List<RegScope> findDefaultScopes() {
        List<String> scopeCodes = getScopeCodes();
        List<RegScope> defaultScopes;
        if (CollectionUtils.isEmpty(scopeCodes)) {
            defaultScopes = Collections.emptyList();
        } else {
            defaultScopes = scopeRepository.findByCodes(scopeCodes);
        }

        return defaultScopes;
    }

    /**
     * Uložení či update souřadnic rejsříkového hesla.
     *
     * @param coordinates souřadnice
     * @return výslendný objekt uložený do db
     */
    @AuthMethod(permission = {UsrPermission.Permission.REG_SCOPE_WR_ALL, UsrPermission.Permission.REG_SCOPE_WR})
    public RegCoordinates saveRegCoordinates(@AuthParam(type = AuthParam.Type.SCOPE) final RegCoordinates coordinates) {
        Assert.notNull(coordinates, "Musí být vyplněné koordináty");

        RegRecord regRecord = coordinates.getRegRecord();
        Assert.notNull(regRecord, "RegRecord musí být vyplněno.");
        Integer recordId = regRecord.getRecordId();
        Assert.notNull(recordId, "RegRecord nemá vyplněno ID.");

        regRecord = regRecordRepository.findOne(recordId);
        Assert.notNull(regRecord, "RegRecord nebylo nalezeno podle id " + recordId);
        coordinates.setRegRecord(regRecord);

        Assert.notNull(coordinates.getValue(), "Hodnota value musí být vyplněna");
        RegCoordinates savedCords = regCoordinatesRepository.save(coordinates);
        eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.RECORD_UPDATE, recordId));
        return savedCords;
    }

    /**
     * Uložení či update List souřadnic rejsříkového hesla. - využito pro import kml
     *
     * @param coordinatesList souřadnice
     * @return výslendný objekt uložený do db
     */
    @AuthMethod(permission = {UsrPermission.Permission.REG_SCOPE_WR_ALL})
    public List<RegCoordinates> saveRegCoordinates(final List<RegCoordinates> coordinatesList) {
        Assert.notEmpty(coordinatesList, "Musí být vyplněn alespoň jeden koordinát");
        List<Integer> notifiedIds = new ArrayList<>();
        for (RegCoordinates cord : coordinatesList) {
            Assert.notNull(cord, "Koodrinát musí být nenulový");
            Assert.notNull(cord.getRegRecord(), "RegRecord musí být vyplněno.");
            Integer recordId = cord.getRegRecord().getRecordId();
            Assert.notNull(recordId, "RegRecord nemá vyplněno ID.");
            Assert.notNull(cord.getValue(), "Hodnota value musí být vyplněna");
            if (!notifiedIds.contains(recordId)) {
                eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.RECORD_UPDATE, recordId));
                notifiedIds.add(recordId);
            }
        }

        return regCoordinatesRepository.save(coordinatesList);
    }

    @AuthMethod(permission = {UsrPermission.Permission.REG_SCOPE_RD_ALL, UsrPermission.Permission.REG_SCOPE_RD})
    public RegRecord getRecord(@AuthParam(type = AuthParam.Type.REGISTRY) final Integer recordId) {
        Assert.notNull(recordId, "Identifikátor rejstříkového hesla musí být vyplněn");
        return regRecordRepository.findOne(recordId);
    }

    /**
     * Vyhledání rejstříkových hesel k požadovaným jednotkám popisu.
     *
     * @param nodeIds identifikátory jednotky popisu
     * @return mapa - klíč identifikátor jed. popisu, hodnota - seznam rejstříkových hesel
     */
    public Map<Integer, List<RegRecord>> findByNodes(final Collection<Integer> nodeIds) {
        return nodeRegisterRepository.findByNodes(nodeIds);
    }

    /**
     * Získání coordinate
     * včetně oprávnění
     *
     * @param coordinatesId coordinate Id
     * @return coordinate
     */
    public RegCoordinates getRegCoordinate(final Integer coordinatesId) {
        RegCoordinates coordinates = regCoordinatesRepository.getOneCheckExist(coordinatesId);
        beanFactory.getBean(RegistryService.class).getRecord(coordinates.getRegRecord().getRecordId());
        return coordinates;
    }

    /**
     * Smazání coordinate
     *
     * @param coordinate coordinate ke smazání
     * @param record record z důvodu oprávnění
     */
    @AuthMethod(permission = {UsrPermission.Permission.REG_SCOPE_WR_ALL, UsrPermission.Permission.REG_SCOPE_WR})
    public void deleteRegCoordinate(final RegCoordinates coordinate, @AuthParam(type = AuthParam.Type.REGISTRY) final RegRecord record) {
        regCoordinatesRepository.delete(coordinate);
    }

    /**
     * Získání variant record
     * včetně oprávnění
     *
     * @param variantRecordId variant record id
     * @return variant record
     */
    public RegVariantRecord getVariantRecord(final Integer variantRecordId) {
        RegVariantRecord variantRecord = variantRecordRepository.getOneCheckExist(variantRecordId);
        beanFactory.getBean(RegistryService.class).getRecord(variantRecord.getRegRecord().getRecordId());
        return variantRecord;
    }


    /**
     * Smazání variant record
     *
     * @param variantRecord variant record ke smazání
     * @param record record z důvodu oprávnění
     */
    @AuthMethod(permission = {UsrPermission.Permission.REG_SCOPE_WR_ALL, UsrPermission.Permission.REG_SCOPE_WR})
    public void deleteVariantRecord(final RegVariantRecord variantRecord, @AuthParam(type = AuthParam.Type.REGISTRY) final RegRecord record) {
        variantRecordRepository.delete(variantRecord);
    }

    /**
     * Najde použití rejstříku/osoby.
     *
     * @param record rejstřík
     * @param party osoba, může být null
     *
     * @return použití rejstříku/osoby
     */
	public RecordUsageVO findRecordUsage(final RegRecord record, @Nullable final ParParty party) {
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
	private List<PartyVO> findUsedParties(final RegRecord record, final ParParty party) {
		List<PartyVO> usedParties = new LinkedList<>();

		// hledání podle vztahů
		final Map<Integer, PartyVO> partyVOMap = relationEntityRepository.findByRecord(record).stream().map(rel -> {
			ParParty partyRel = rel.getRelation().getParty();
			PartyVO partyVO = new PartyVO();
			partyVO.setId(partyRel.getPartyId());
			partyVO.setName(partyRel.getRecord().getRecord());

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
					partyVO.setName(creator.getRecord().getRecord());

					List<OccurrenceVO> occurrences = new LinkedList<>();
					partyVO.setOccurrences(occurrences);
				}

				OccurrenceVO occurrenceVO = new OccurrenceVO(c.getCreatorId(), OccurrenceType.PAR_CREATOR);
				partyVO.getOccurrences().add(occurrenceVO);
			});

			usedParties.addAll(partyVOMap.values());
		}

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
	private List<FundVO> findUsedFunds(final RegRecord record, final ParParty party) {
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
					dataIds = nodeIdToDataIdsMap.get(fundId);
				} else {
					dataIds = new HashSet<>();
					nodeIdToDataIdsMap.put(nodeId, dataIds);
				}
				dataIds.add(dataId);
			});
		}

		List<ArrFundVersion> fundVersions = fundVersionRepository.findByFundIdsAndLockChangeIsNull(fundIdToNodeIdsMap.keySet());
		return fundVersions.stream().map(v -> {
			return createFundVO(nodeIdToNodeRegisters, fundIdToNodeIdsMap, dataIdToArrDataMap, nodeIdToDataIdsMap, v);

		}).collect(Collectors.toList());
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

		Collection<TreeNodeClient> treeNodes = levelTreeCacheService.getFaTreeNodes(fundVersion.getFundVersionId(), nodeIdsSubList);

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
			final TreeNodeClient node) {
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
    public void replace(final RegRecord replaced, final RegRecord replacement) {

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


        final Map<Integer, ArrFundVersion> fundVersions = arrangementService.getOpenVersionsByFundIds(fundsAll).stream().collect(Collectors.toMap(ArrFundVersion::getFundId, Function.identity()));

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
                if (!fundScopes.contains(replacement.getRegScope().getScopeId())) {
                    throw new BusinessException("Nelze nahradit rejsříkové heslo v AS jelikož AS nemá scope rejstříku pomocí kterého nahrazujeme.", BaseCode.INVALID_STATE)
                            .set("fundId", fundId)
                            .set("scopeId", replacement.getRegScope().getScopeId());
                }
            }
            descriptionItemService.updateDescriptionItem(im, fundVersions.get(fundId), change, true);
        });


        final RegistryService self = applicationContext.getBean(RegistryService.class);
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
                if (!fundScopes.contains(replacement.getRegScope().getScopeId())) {
                    throw new BusinessException("Nelze nahradit rejsříkové heslo v AS jelikož AS nemá scope rejstříku pomocí kterého nahrazujeme.", BaseCode.INVALID_STATE)
                            .set("fundId", fundId)
                            .set("scopeId", replacement.getRegScope().getScopeId());
                }
            }
            self.updateRegisterLink(fundVersions.get(fundId).getFundVersionId(), i.getNodeId(), arrNodeRegister);
        });

        // relace
        final List<ParRelationEntity> byRecord = relationEntityRepository.findByRecord(replaced);

        final Map<ParRelation, Map<Integer, ParRelationEntity>> relationWithEntities = new HashMap<>();
        byRecord.forEach(i -> {
            ParRelationEntity relationEntity = relationWithEntities.computeIfAbsent(i.getRelation(), n -> relationEntityRepository.findByRelation(n).stream()
                    .collect(Collectors.toMap(ParRelationEntity::getRelationEntityId, Function.identity()))
            ).get(i.getRelationEntityId());

            relationEntity.setRecord(replacement);
        });

        // oprávnění relací
        Set<ParParty> hasPermForParty = new HashSet<>();
        boolean isScopeAdmin = userService.hasPermission(UsrPermission.Permission.REG_SCOPE_WR_ALL);
        relationWithEntities.forEach((rel, relEnt) -> {
            if (!hasPermForParty.contains(rel.getParty())) {
                Integer scopeId = rel.getParty().getRegScope().getScopeId();
                if (!isScopeAdmin &&
                        !userService.hasPermission(UsrPermission.Permission.REG_SCOPE_WR, scopeId)) {
                    throw new SystemException("Uživatel nemá oprávnění na scope.", BaseCode.INSUFFICIENT_PERMISSIONS).set("scopeId", scopeId);

                }
                hasPermForParty.add(rel.getParty());
            }
            partyService.saveRelation(rel, relEnt.values());
        });
    }

    public boolean canBeDeleted(RegRecord record) {
        return CollectionUtils.isEmpty(dataRecordRefRepository.findByRecord(record)) &&
                CollectionUtils.isEmpty(nodeRegisterRepository.findByRecordId(record)) &&
                CollectionUtils.isEmpty(relationEntityRepository.findByRecord(record));
    }
}
