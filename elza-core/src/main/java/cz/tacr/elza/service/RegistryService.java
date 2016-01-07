package cz.tacr.elza.service;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;
import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegExternalSource;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.ExternalSourceRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.VariantRecordRepository;


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
    private ExternalSourceRepository externalSourceRepository;

    @Autowired
    private PartyService partyService;

    @Autowired
    private DataRecordRefRepository dataRecordRefRepository;

    @Autowired
    private NodeRegisterRepository nodeRegisterRepository;


    /**
     * Nalezne takové záznamy rejstříku, které mají daný typ a jejich textová pole (record, charateristics, comment),
     * nebo pole variantního záznamu obsahují hledaný řetězec. V případě, že hledaný řetězec je null, nevyhodnocuje se.
     *
     * @param searchRecord    hledaný řetězec, může být null
     * @param registerTypeIds typ záznamu
     * @param firstResult     index prvního záznamu, začíná od 0
     * @param maxResults      počet výsledků k vrácení
     * @return vybrané záznamy dle popisu seřazené za record, nbeo prázdná množina
     */
    public List<RegRecord> findRegRecordByTextAndType(@Nullable final String searchRecord,
                                                      @Nullable final Collection<Integer> registerTypeIds,
                                                      final Boolean local,
                                                      final Integer firstResult,
                                                      final Integer maxResults) {

        return regRecordRepository
                .findRegRecordByTextAndType(searchRecord, registerTypeIds, local, firstResult, maxResults);
    }

    /**
     * Celkový počet záznamů v DB pro funkci {@link #findRegRecordByTextAndType(String, Collection, Boolean, Integer,
     * Integer)}
     *
     * @param searchRecord    hledaný řetězec, může být null
     * @param registerTypeIds typ záznamu
     * @return celkový počet záznamů, který je v db za dané parametry
     */
    public long findRegRecordByTextAndTypeCount(@Nullable final String searchRecord,
                                                @Nullable final Collection<Integer> registerTypeIds,
                                                final Boolean local) {


        return regRecordRepository.findRegRecordByTextAndTypeCount(searchRecord, registerTypeIds, local);
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
    @Transactional
    public RegRecord saveRecord(final RegRecord record, boolean checkPartyType) {
        Assert.notNull(record);

        Assert.notNull(record.getRecord(), "Není vyplněné Record.");
        Assert.notNull(record.getCharacteristics(), "Není vyplněné Characteristics.");
        Assert.notNull(record.getLocal(), "Není vyplněné Local.");

        RegRegisterType regRegisterType = record.getRegisterType();
        Assert.notNull(regRegisterType, "Není vyplněné RegisterType.");
        Integer registerTypeId = regRegisterType.getRegisterTypeId();
        Assert.notNull(registerTypeId, "RegisterType nemá vyplněné ID.");
        regRegisterType = registerTypeRepository.findOne(registerTypeId);
        Assert.notNull(regRegisterType, "RegisterType nebylo nalezeno podle id " + registerTypeId);
        record.setRegisterType(regRegisterType);

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

        return regRecordRepository.save(record);
    }


    /**
     * Smaže rej. heslo a jeho variantní hesla. Předpokládá, že již proběhlo ověření, že je možné ho smazat (vazby atd...).
     * @param record heslo
     */
    public void deleteRecord(final RegRecord record, final boolean checkUsage) {
        if(checkUsage){
            checkRecordUsage(record);
        }

        variantRecordRepository.delete(variantRecordRepository.findByRegRecordId(record.getRecordId()));
        regRecordRepository.delete(record);
    }


    /**
     * Uložení či update variantního záznamu.
     *
     * @param variantRecord variantní záznam, bez vazeb
     * @return výslendný objekt uložený do db
     */
    @Transactional
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

}
