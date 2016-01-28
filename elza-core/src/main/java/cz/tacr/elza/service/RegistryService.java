package cz.tacr.elza.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegExternalSource;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.ExternalSourceRepository;
import cz.tacr.elza.repository.FaRegisterScopeRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import cz.tacr.elza.service.eventnotification.EventFactory;
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
    private FaRegisterScopeRepository faRegisterScopeRepository;

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
     * @param findingAid   AP, ze které se použijí třídy rejstříků
     * @return vybrané záznamy dle popisu seřazené za record, nbeo prázdná množina
     */
    public List<RegRecord> findRegRecordByTextAndType(@Nullable final String searchRecord,
                                                      @Nullable final Collection<Integer> registerTypeIds,
                                                      final Integer firstResult,
                                                      final Integer maxResults,
                                                      final Integer parentRecordId,
                                                      @Nullable final ArrFindingAid findingAid) {

        Set<Integer> scopeIdsForRecord = getScopeIdsByFindingAid(findingAid);

        if (StringUtils.isBlank(searchRecord) && parentRecordId == null) {
            return regRecordRepository.findRootRecords(registerTypeIds, firstResult, maxResults, scopeIdsForRecord);
        }

        RegRecord parentRecord = null;
        if (parentRecordId != null) {
            parentRecord = regRecordRepository.getOne(parentRecordId);
            if (parentRecord == null) {
                throw new IllegalArgumentException("Rejstřík s identifikátorem " + parentRecordId + " neexistuje.");
            }
        }

        return regRecordRepository.findRegRecordByTextAndType(searchRecord, registerTypeIds, firstResult,
                maxResults, parentRecord, scopeIdsForRecord);
    }


    /**
     * Celkový počet záznamů v DB pro funkci {@link #findRegRecordByTextAndType(String, Collection, Integer, Integer, Integer, ArrFindingAid)}
     *
     * @param searchRecord    hledaný řetězec, může být null
     * @param registerTypeIds typ záznamu
     * @param parentRecordId  id rodičovského rejstříku
     * @param findingAid   AP, ze které se použijí třídy rejstříků
     * @return celkový počet záznamů, který je v db za dané parametry
     */
    public long findRegRecordByTextAndTypeCount(@Nullable final String searchRecord,
            @Nullable final Collection<Integer> registerTypeIds, final Integer parentRecordId, @Nullable final ArrFindingAid findingAid) {

        Set<Integer> scopeIdsForRecord = getScopeIdsByFindingAid(findingAid);

        if (StringUtils.isBlank(searchRecord) && parentRecordId == null) {
            return regRecordRepository.findRootRecordsByTypeCount(registerTypeIds, scopeIdsForRecord);
        }

        RegRecord parentRecord = null;
        if (parentRecordId != null) {
            parentRecord = regRecordRepository.getOne(parentRecordId);
            if (parentRecord == null) {
                throw new IllegalArgumentException("Rejstřík s identifikátorem " + parentRecordId + " neexistuje.");
            }
        }


        return regRecordRepository
                .findRegRecordByTextAndTypeCount(searchRecord, registerTypeIds, parentRecord, scopeIdsForRecord);
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

        List<ArrDataRecordRef> dataRecordRefList = dataRecordRefRepository.findByRecordId(record.getRecordId());
        if (CollectionUtils.isNotEmpty(dataRecordRefList)) {
            throw new IllegalStateException("Nalezeno použití hesla v tabulce ArrDataRecordRef.");
        }

        List<ArrNodeRegister> nodeRegisterList = nodeRegisterRepository.findByRecordId(record);
        if (CollectionUtils.isNotEmpty(nodeRegisterList)) {
            throw new IllegalStateException("Nalezeno použití hesla v tabulce ArrDataRecordRef.");
        }
    }


    /**
     * Uložení či update záznamu.
     *
     * @param record naplněný objekt, bez vazeb
     * @return výslendný objekt
     */
    public RegRecord saveRecord(final RegRecord record, boolean checkPartyType) {
        Assert.notNull(record);

        Assert.notNull(record.getRecord(), "Není vyplněné Record.");

        RegRegisterType regRegisterType = record.getRegisterType();
        Assert.notNull(regRegisterType, "Není vyplněné RegisterType.");
        Assert.notNull(regRegisterType.getRegisterTypeId(), "RegisterType nemá vyplněné ID.");
        regRegisterType = registerTypeRepository.findOne(regRegisterType.getRegisterTypeId());
        Assert.notNull(regRegisterType, "RegisterType nebylo nalezeno podle id " + regRegisterType.getRegisterTypeId());
        record.setRegisterType(regRegisterType);


        Assert.notNull(record.getScope(), "Není vyplněna třída rejstříku");
        Assert.notNull(record.getScope().getScopeId(), "Není vyplněno id třídy rejstříku");
        RegScope scope = scopeRepository.findOne(record.getScope().getScopeId());
        Assert.notNull(scope, "Nebyla nalezena třída rejstříku s id " + record.getScope().getScopeId());
        if (record.getRecordId() != null) {
            RegRecord dbRecord = regRecordRepository.findOne(record.getRecordId());
            if (!record.getScope().getScopeId().equals(dbRecord.getScope().getScopeId())) {
                throw new IllegalArgumentException("Nelze změnit třídu rejstříku.");
            }
        }
        record.setScope(scope);


        if (checkPartyType) {
            if (record.getRecordId() == null) {
                if (regRegisterType != null && regRegisterType.getPartyType() != null) {
                    throw new IllegalArgumentException("Typ hesla nesmí mít vazbu na typ osoby.");
                }
            } else {
                ParParty party = partyService.findParPartyByRecord(record);
                if (party != null) {
                    throw new IllegalArgumentException("Nelze editovat rejstříkové heslo, které má navázanou osobu.");
                }
            }
        }


        RegExternalSource externalSource = record.getExternalSource();
        if (externalSource != null) {
            Integer externalSourceId = externalSource.getExternalSourceId();
            Assert.notNull(externalSourceId, "ExternalSource nemá vyplněné ID.");
            externalSource = externalSourceRepository.findOne(externalSourceId);
            Assert.notNull(externalSource, "ExternalSource nebylo nalezeno podle id " + externalSourceId);
            record.setExternalSource(externalSource);
        }

        if(record.getParentRecord() != null && record.getParentRecord().getRecordId() != null){
            RegRecord parentRecord = regRecordRepository.findOne(record.getParentRecord().getRecordId());
            Assert.notNull(parentRecord,
                    "Nebylo nalezeno rejstříkové heslo s id " + record.getParentRecord().getRecordId());
            record.setParentRecord(parentRecord);
        }


        RegRecord result = regRecordRepository.save(record);
        EventType type = record.getRecordId() == null ? EventType.RECORD_CREATE : EventType.RECORD_UPDATE;
        eventNotificationService.publishEvent(EventFactory.createIdEvent(type, result.getRecordId()));

        return result;
    }


    /**
     * Smaže rej. heslo a jeho variantní hesla. Předpokládá, že již proběhlo ověření, že je možné ho smazat (vazby atd...).
     * @param record heslo
     */
    public void deleteRecord(final RegRecord record, final boolean checkUsage) {
        if(checkUsage){
            checkRecordUsage(record);
        }

        eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.PARTY_CREATE, record.getRecordId()));

        variantRecordRepository.delete(variantRecordRepository.findByRegRecordId(record.getRecordId()));
        regRecordRepository.delete(record);
    }


    /**
     * Uložení či update variantního záznamu.
     *
     * @param variantRecord variantní záznam, bez vazeb
     * @return výslendný objekt uložený do db
     */
    public RegVariantRecord saveVariantRecord(final RegVariantRecord variantRecord) {
        Assert.notNull(variantRecord);

        RegRecord regRecord = variantRecord.getRegRecord();
        Assert.notNull(regRecord, "RegRecord musí být vyplněno.");
        Integer recordId = regRecord.getRecordId();
        Assert.notNull(recordId, "RegRecord nemá vyplněno ID.");

        regRecord = regRecordRepository.findOne(recordId);
        Assert.notNull(regRecord, "RegRecord nebylo nalezeno podle id " + recordId);
        variantRecord.setRegRecord(regRecord);

        return variantRecordRepository.save(variantRecord);
    }

    public Map<RegRecord, List<RegRecord>> findChildren(List<RegRecord> records) {
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
    public void deleteScope(final RegScope scope) {
        Assert.notNull(scope);
        Assert.notNull(scope.getScopeId());

        List<RegRecord> scopeRecords = regRecordRepository.findByScope(scope);
        if (!scopeRecords.isEmpty()) {
            throw new IllegalStateException("Nelze smazat třídu rejstříku, která je nastavena na rejstříku.");
        }

        faRegisterScopeRepository.delete(faRegisterScopeRepository.findByScope(scope));
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
     * @param findingAid AP, podle jejíž tříd se má hledat (pokud je null, hledá se podle výchozích)
     * @return množina id tříd, podle kterých se bude hledat
     */
    public Set<Integer> getScopeIdsByFindingAid(@Nullable final ArrFindingAid findingAid){
        if(findingAid == null){
            return defaultScopeIds;
        }else{
            return scopeRepository.findIdsByFindingAid(findingAid);
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
}
