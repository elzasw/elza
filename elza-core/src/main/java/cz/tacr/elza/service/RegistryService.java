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
import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaTools;
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
import cz.tacr.elza.domain.RegExternalSource;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.ExternalSourceRepository;
import cz.tacr.elza.repository.FundRegisterScopeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.RegCoordinatesRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.ScopeRepository;
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
@ConfigurationProperties(prefix = "elza.record")
@Service
public class RegistryService {

    @Autowired
    private RegRecordRepository regRecordRepository;

    @Autowired
    private VariantRecordRepository variantRecordRepository;

    @Autowired
    private RegisterTypeRepository registerTypeRepository;

    @Autowired
    private ExternalSourceRepository externalSourceRepository;

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

    /**
     * Kody tříd rejstříků nastavené v konfiguraci elzy.
     */
    private List<String> scopeCodes;

    /**
     * Id tříd rejstříků nastavené v konfiguraci elzy.
     */
    private Set<Integer> defaultScopeIds;


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
            parentRecord = regRecordRepository.getOne(parentRecordId);
            if (parentRecord == null) {
                throw new IllegalArgumentException("Rejstřík s identifikátorem " + parentRecordId + " neexistuje.");
            }
        }

        UsrUser user = userService.getLoggedUser();
        boolean readAllScopes = userService.hasPermission(cz.tacr.elza.api.UsrPermission.Permission.REG_SCOPE_RD_ALL);
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
            parentRecord = regRecordRepository.getOne(parentRecordId);
            if (parentRecord == null) {
                throw new IllegalArgumentException("Rejstřík s identifikátorem " + parentRecordId + " neexistuje.");
            }
        }

        UsrUser user = userService.getLoggedUser();
        boolean readAllScopes = userService.hasPermission(cz.tacr.elza.api.UsrPermission.Permission.REG_SCOPE_RD_ALL);

        return regRecordRepository
                .findRegRecordByTextAndTypeCount(searchRecord, registerTypeIds, parentRecord, scopeIdsForRecord,
                        readAllScopes, user);
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
            throw new IllegalStateException("Existuje vazba z osoby, nelze smazat.");
        }

        List<ArrDataRecordRef> dataRecordRefList = dataRecordRefRepository.findByRecord(record);
        if (CollectionUtils.isNotEmpty(dataRecordRefList)) {
            throw new IllegalStateException("Nalezeno použití hesla v tabulce ArrDataRecordRef.");
        }

        List<ArrNodeRegister> nodeRegisterList = nodeRegisterRepository.findByRecordId(record);
        if (CollectionUtils.isNotEmpty(nodeRegisterList)) {
            throw new IllegalStateException("Nalezeno použití hesla v tabulce ArrDataRecordRef.");
        }

        List<RegRecord> childs = regRecordRepository.findByParentRecord(record);
        if (!childs.isEmpty()) {
            throw new IllegalStateException("Nelze smazat rejstříkové heslo, které má potomky.");
        }


    }


    /**
     * Uložení či update záznamu.
     *
     * @param record    naplněný objekt, bez vazeb
     * @param partySave true - jedná se o ukládání přes ukládání osoby, false -> editace z klienta
     * @return výslendný objekt
     */
    @AuthMethod(permission = {UsrPermission.Permission.REG_SCOPE_WR_ALL, UsrPermission.Permission.REG_SCOPE_WR})
    public RegRecord saveRecord(@AuthParam(type = AuthParam.Type.SCOPE) final RegRecord record,
                                final boolean partySave) {
        Assert.notNull(record);

        checkRecordSave(record, partySave);

        RegRegisterType registerType = registerTypeRepository.findOne(record.getRegisterType().getRegisterTypeId());
        record.setRegisterType(registerType);

        RegScope scope = scopeRepository.findOne(record.getScope().getScopeId());
        record.setScope(scope);

        RegExternalSource externalSource = record.getExternalSource();
        if (externalSource != null) {
            Integer externalSourceId = externalSource.getExternalSourceId();
            Assert.notNull(externalSourceId, "ExternalSource nemá vyplněné ID.");
            externalSource = externalSourceRepository.findOne(externalSourceId);
            Assert.notNull(externalSource, "ExternalSource nebylo nalezeno podle id " + externalSourceId);
            record.setExternalSource(externalSource);
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
        Assert.notNull(record);
        Assert.notNull(type);

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
                throw new IllegalArgumentException("Nelze vložit pod potomka.");
            }

            parent = parent.getParentRecord();
        }
    }

    /**
     * Smaže rej. heslo a jeho variantní hesla. Předpokládá, že již proběhlo ověření, že je možné ho smazat (vazby atd...).
     * @param record heslo
     */
    @AuthMethod(permission = {UsrPermission.Permission.REG_SCOPE_WR_ALL, UsrPermission.Permission.REG_SCOPE_WR})
    public void deleteRecord(@AuthParam(type = AuthParam.Type.SCOPE) final RegRecord record,
                             final boolean checkUsage) {
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
        Assert.notNull(record);

        Assert.notNull(record.getRecord(), "Není vyplněné Record.");

        RegRegisterType regRegisterType = record.getRegisterType();
        Assert.notNull(regRegisterType, "Není vyplněné RegisterType.");
        Assert.notNull(regRegisterType.getRegisterTypeId(), "RegisterType nemá vyplněné ID.");
        regRegisterType = registerTypeRepository.findOne(regRegisterType.getRegisterTypeId());
        Assert.notNull(regRegisterType, "RegisterType nebylo nalezeno podle id " + regRegisterType.getRegisterTypeId());

        if (partySave) {
            if (regRegisterType.getPartyType() == null) {
                throw new IllegalArgumentException("Typ hesla musí mít vazbu na typ osoby.");
            }
        } else {
            if (record.getRecordId() == null && regRegisterType.getPartyType() != null) {
                throw new IllegalArgumentException("Nelze vytvořit rejstříkové heslo, které je navázané na typ osoby");
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
                throw new IllegalArgumentException("Nelze nastavit rodiče rejstříkovému heslu sebe samotného.");
            }

            ElzaTools.checkEquals(record.getRegisterType(), parentRecord.getRegisterType(),
                    "Potomek rejstříkového hesla musí mít stejný typ jako jeho rodič. ");
        }


        if (record.getRecordId() == null) {
            if (!regRegisterType.getAddRecord()) {
                throw new IllegalArgumentException(
                        "Nelze přidávat heslo do typu, který nemá přidávání hesel povolené.");
            }

            if (parentRecord != null && !parentRecord.getRegisterType().getHierarchical()) {
                throw new IllegalArgumentException("Nelze přidávat heslo k rodiči, který není hierarchický.");
            }
        } else {
            RegRecord dbRecord = regRecordRepository.findOne(record.getRecordId());
            if (!record.getScope().getScopeId().equals(dbRecord.getScope().getScopeId())) {
                throw new IllegalArgumentException("Nelze změnit třídu rejstříku.");
            }

            List<RegRecord> childs = regRecordRepository.findByParentRecord(dbRecord);
            if (dbRecord.getRegisterType().getHierarchical() && !regRegisterType.getHierarchical() && !childs
                    .isEmpty()) {
                throw new IllegalArgumentException(
                        "Nelze změnit typ rejstříkového hesla na nehierarchický, pokud má heslo potomky.");
            }

            ParParty party = partyService.findParPartyByRecord(dbRecord);
            if (party == null) {
                if (regRegisterType.getPartyType() != null) {
                    throw new IllegalArgumentException("Nelze nastavit typ hesla, které je navázané na typ osoby.");
                }

            } else {
                ElzaTools.checkEquals(regRegisterType.getPartyType(), party.getPartyType(),
                        "Nelze změnit typ rejstříkového hesla osoby, který odkazuje na jiný typ osoby.");

                //pokud editujeme heslo přes insert/update, a ne přes ukládání osoby
                if (!partySave) {
                    ElzaTools.checkEquals(record.getRecord(), dbRecord.getRecord(),
                            "Nelze editovat hodnotu rejstříkového hesla napojeného na osobu.");
                    ElzaTools.checkEquals(record.getCharacteristics(), dbRecord.getCharacteristics(),
                            "Nelze editovat charakteristiku rejstříkového hesla napojeného na osobu.");
                    ElzaTools.checkEquals(record.getExternalId(), dbRecord.getExternalId(),
                            "Nelze editovat externí id rejstříkového hesla napojeného na osobu.");
                    ElzaTools.checkEquals(record.getRecord(), dbRecord.getRecord(),
                            "Nelze editovat hodnotu rejstříkového hesla napojeného na osobu.");
                    ElzaTools.checkEquals(record.getExternalSource(), record.getExternalSource(),
                            "Nelze editovat externí zdroj rejstříkového hesla, které je napojené na osobu.");
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
        Assert.notNull(variantRecord);

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
        Assert.notNull(records);

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
        Assert.notNull(scope);
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
        Assert.notNull(scope);
        Assert.notNull(scope.getScopeId());

        List<RegRecord> scopeRecords = regRecordRepository.findByScope(scope);
        if (!scopeRecords.isEmpty()) {
            throw new IllegalStateException("Nelze smazat třídu rejstříku, která je nastavena na rejstříku.");
        }

        fundRegisterScopeRepository.delete(fundRegisterScopeRepository.findByScope(scope));
        scopeRepository.delete(scope);
    }

    /**
     * Kontrola uložení třídy rejstříku.
     *
     * @param scope ukládaná třída
     */
    private void checkScopeSave(final RegScope scope) {
        Assert.notNull(scope);
        Assert.notNull(scope.getCode(), "Třída musí mít vyplněný kod");
        Assert.notNull(scope.getName(), "Třída musí mít vyplněný název");

        List<RegScope> scopes = scopeRepository.findByCodes(Arrays.asList(scope.getCode()));
        RegScope codeScope = scopes.isEmpty() ? null : scopes.get(0);
        if (scope.getScopeId() == null) {
            if (!scopes.isEmpty()) {
                throw new IllegalArgumentException("Kod třídy rejstříku se již nachází v databázi.");
            }
        } else {
            if (codeScope == null) {
                throw new IllegalArgumentException("Záznam pro editaci nebyl nalezen.");
            }

            if (!codeScope.getScopeId().equals(scope.getScopeId())) {
                throw new IllegalArgumentException("Kod třídy rejstříku se již nachází v databázi.");
            }

            RegScope dbScope = scopeRepository.getOne(scope.getScopeId());
            if (!dbScope.getCode().equals(scope.getCode())) {
                throw new IllegalArgumentException("Třídě rejstříku nelze změnít kód, pouze název.");
            }
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
            return defaultScopeIds;
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
        Assert.notNull(fundVersionId);
        Assert.notNull(nodeId);

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
        Assert.notNull(nodeRegister);
        Assert.isNull(nodeRegister.getNodeRegisterId());

        ArrNode node = nodeRepository.findOne(nodeId);

        ArrChange change = arrangementService.createChange(ArrChange.Type.ADD_RECORD_NODE, node);

        node.setVersion(nodeRegister.getNode().getVersion());
        saveNode(node, change);

        validateNodeRegisterLink(nodeRegister);

        nodeRegister.setNode(node);
        nodeRegister.setCreateChange(change);
        eventNotificationService.publishEvent(new EventNodeIdVersionInVersion(EventType.FUND_RECORD_CHANGE, versionId, nodeRegister.getNode().getNodeId(), nodeRegister.getNode().getVersion()));
        return nodeRegisterRepository.save(nodeRegister);
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
        Assert.notNull(nodeRegister);
        Assert.notNull(nodeRegister.getNodeRegisterId());

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
        return nodeRegisterRepository.save(nodeRegister);
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
        Assert.notNull(nodeRegister);
        Assert.notNull(nodeRegister.getNodeRegisterId());

        ArrNodeRegister nodeRegisterDB = nodeRegisterRepository.findOne(nodeRegister.getNodeRegisterId());

        ArrNode node = nodeRepository.findOne(nodeId);

        ArrChange change = arrangementService.createChange(ArrChange.Type.DELETE_RECORD_NODE, node);

        node.setVersion(nodeRegister.getNode().getVersion());
        saveNode(node, change);

        validateNodeRegisterLink(nodeRegisterDB);

        nodeRegisterDB.setDeleteChange(change);

        eventNotificationService.publishEvent(new EventNodeIdVersionInVersion(EventType.FUND_RECORD_CHANGE, versionId, node.getNodeId(), node.getVersion()));
        return nodeRegisterRepository.save(nodeRegisterDB);
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
        return scopeCodes;
    }

    public void setScopeCodes(final List<String> scopeCodes) {
        this.scopeCodes = scopeCodes;
    }

    @PostConstruct
    public void initScopeIds() throws Exception {
        if (CollectionUtils.isNotEmpty(scopeCodes)) {
            List<RegScope> foundCodes = scopeRepository.findByCodes(scopeCodes);
            defaultScopeIds = foundCodes.stream().map(s -> s.getScopeId()).collect(Collectors.toSet());
        }
    }


    public List<RegScope> findDefaultScopes() {
        List<RegScope> defaultScopes;
        if (CollectionUtils.isEmpty(scopeCodes)) {
            defaultScopes = Collections.EMPTY_LIST;
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
        Assert.notNull(coordinates);

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
        Assert.notEmpty(coordinatesList);
        List<Integer> notifiedIds = new ArrayList<>();
        for (RegCoordinates cord : coordinatesList) {
            Assert.notNull(cord);
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
        Assert.notNull(recordId);
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
}
