package cz.tacr.elza.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ExceptionUtils;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegCoordinates;
import cz.tacr.elza.domain.RegExternalSystem;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.packageimport.xml.SettingRecord;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.FundRegisterScopeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.RegCoordinatesRepository;
import cz.tacr.elza.repository.RegExternalSystemRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.SettingsRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
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
    private VariantRecordRepository variantRecordRepository;

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

    /**
     * Kody tříd rejstříků nastavené v konfiguraci elzy.
     */
    private List<String> scopeCodes;

    /**
     * Id tříd rejstříků nastavené v konfiguraci elzy.
     */
    private Set<Integer> defaultScopeIds = null;

    public Set<Integer> getDefaultScopeIds() {
        if (defaultScopeIds == null) {
            List<String> scopeCodes = getScopeCodes();
            if (CollectionUtils.isNotEmpty(scopeCodes)) {
                List<RegScope> foundCodes = scopeRepository.findByCodes(scopeCodes);
                defaultScopeIds = foundCodes.stream().map(RegScope::getScopeId).collect(Collectors.toSet());
            }
        }
        return defaultScopeIds;
    }

    /**
     * Nalezne takové záznamy rejstříku, které mají daný typ a jejich textová pole (record, charateristics, comment),
     * nebo pole variantního záznamu obsahují hledaný řetězec. V případě, že hledaný řetězec je null, nevyhodnocuje se.
     *
     * @param searchRecord    hledaný řetězec, může být null
     * @param registerTypeIds typ záznamu
     * @param firstResult     index prvního záznamu, začíná od 0
     * @param maxResults      počet výsledků k vrácení
     * @param parentRecordId  id rodičovského rejstříku
     * @param fund   AP, ze které se použijí třídy rejstříků
     * @return vybrané záznamy dle popisu seřazené za record, nbeo prázdná množina
     */
    public List<RegRecord> findRegRecordByTextAndType(@Nullable final String searchRecord,
                                                      @Nullable final Collection<Integer> registerTypeIds,
                                                      final Integer firstResult,
                                                      final Integer maxResults,
                                                      final Integer parentRecordId,
                                                      @Nullable final ArrFund fund) {

        Set<Integer> scopeIdsForRecord = getScopeIdsByFund(fund);

        RegRecord parentRecord = null;
        if (parentRecordId != null) {
            parentRecord = regRecordRepository.getOneCheckExist(parentRecordId);
        }

        UsrUser user = userService.getLoggedUser();
        boolean readAllScopes = userService.hasPermission(UsrPermission.Permission.REG_SCOPE_RD_ALL);
        return regRecordRepository.findRegRecordByTextAndType(searchRecord, registerTypeIds, firstResult,
                maxResults, parentRecord, scopeIdsForRecord, readAllScopes, user);
    }


    /**
     * Celkový počet záznamů v DB pro funkci {@link #findRegRecordByTextAndType(String, Collection, Integer, Integer, Integer, ArrFund)}
     *
     * @param searchRecord    hledaný řetězec, může být null
     * @param registerTypeIds typ záznamu
     * @param parentRecordId  id rodičovského rejstříku
     * @param fund   AP, ze které se použijí třídy rejstříků
     * @return celkový počet záznamů, který je v db za dané parametry
     */
    public long findRegRecordByTextAndTypeCount(@Nullable final String searchRecord,
            @Nullable final Collection<Integer> registerTypeIds, final Integer parentRecordId, @Nullable final ArrFund fund) {

        Set<Integer> scopeIdsForRecord = getScopeIdsByFund(fund);

        RegRecord parentRecord = null;
        if (parentRecordId != null) {
            parentRecord = regRecordRepository.getOneCheckExist(parentRecordId);
        }

        UsrUser user = userService.getLoggedUser();
        boolean readAllScopes = userService.hasPermission(UsrPermission.Permission.REG_SCOPE_RD_ALL);

        return regRecordRepository.findRegRecordByTextAndTypeCount(searchRecord, registerTypeIds, parentRecord, scopeIdsForRecord, readAllScopes, user);
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
            throw new BusinessException("Nalezeno použití hesla v tabulce ArrDataRecordRef.", RegistryCode.EXIST_FOREIGN_DATA).set("table", "ArrDataRecordRef");
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
        if(checkUsage){
            checkRecordUsage(record);
        }

        eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.RECORD_CREATE, record.getRecordId()));

        variantRecordRepository.delete(variantRecordRepository.findByRegRecordId(record.getRecordId()));
        regCoordinatesRepository.delete(regCoordinatesRepository.findByRegRecordId(record.getRecordId()));
        regRecordRepository.delete(record);
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
            return Collections.EMPTY_MAP;
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
     * Načte seznam id tříd pro archivní pomůcku. Pokud není AP nastavena, vrací výchozí třídy.
     *
     * @param fund AP, podle jejíž tříd se má hledat (pokud je null, hledá se podle výchozích)
     * @return množina id tříd, podle kterých se bude hledat
     */
    public Set<Integer> getScopeIdsByFund(@Nullable final ArrFund fund){
        if(fund == null){
            return getDefaultScopeIds();
        }else{
            return scopeRepository.findIdsByFund(fund);
        }
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
                    SettingRecord setting = (SettingRecord) packageService.convertSetting(uiSettings);
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
}
