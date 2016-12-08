package cz.tacr.elza.interpi.service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.RegExternalSystem;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.interpi.service.vo.ConditionVO;
import cz.tacr.elza.interpi.service.vo.EntityValueType;
import cz.tacr.elza.interpi.service.vo.ExternalRecordVO;
import cz.tacr.elza.interpi.ws.WssoapSoap;
import cz.tacr.elza.interpi.ws.wo.EntitaTyp;
import cz.tacr.elza.interpi.ws.wo.SetTyp;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.RegExternalSystemRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.service.PartyService;
import cz.tacr.elza.service.RegistryService;
import cz.tacr.elza.utils.WSUtils;
import cz.tacr.elza.utils.XmlUtils;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 11. 2016
 */
@Service
public class InterpiService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** Stránkování. */
    private static final String FROM = "1";
    private static final String TO = "500";

    @Autowired
    private PartyService partyService;

    @Autowired
    private RegistryService registryService;

    @Autowired
    private InterpiFactory interpiFactory;

    @Autowired
    private RegExternalSystemRepository regExternalSystemRepository;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private RegRecordRepository regRecordRepository;

    @Autowired
    private PartyRepository partyRepository;

    /**
     * Vyhledá záznamy v INTERPI.
     *
     * @param isParty příznak zda se mají hledat osoby nebo rejstříky
     * @param conditions seznam podmínek
     * @param count počet záznamů které se mají vrátit
     * @param systemId id externího systému
     *
     * @return seznam nalezených záznamů
     */
    public List<ExternalRecordVO> findRecords(final boolean isParty,
            final List<ConditionVO> conditions,
            final Integer count,
            final Integer systemId) {
        Assert.assertNotNull(systemId);

        String query = interpiFactory.createSearchQuery(conditions, isParty);
        if (StringUtils.isBlank(query)) {
            return Collections.emptyList();
        }

        String maxCount = count == null ? TO : count.toString();
        RegExternalSystem regExternalSystem = getExternalSystem(systemId);

        WssoapSoap client = createClient(regExternalSystem);
        logger.info("Vyhledávání v interpi: " + query);
        String searchResult = client.findData(query, null, FROM, maxCount,
                regExternalSystem.getUsername(), regExternalSystem.getPassword());

        SetTyp setTyp = unmarshall(searchResult);

        List<ExternalRecordVO> result = new LinkedList<>();
        for (EntitaTyp entitaTyp : setTyp.getEntita()) {
            result.add(interpiFactory.convertToExternalRecordVO(entitaTyp, regExternalSystem));
        }

        return result;
    }

    /**
     * Načte konkrétní záznam podle externího id.
     *
     * @param id id v externím systému
     * @param systemId id externího systému
     *
     * @return požadovaný záznam
     */
    public ExternalRecordVO getRecordById(final String id, final Integer systemId) {
        Assert.assertNotNull(id);
        Assert.assertNotNull(systemId);

        RegExternalSystem interpiSystem = getExternalSystem(systemId);
        SetTyp setTyp = findOneRecord(id, interpiSystem);

        return interpiFactory.convertToExternalRecordVO(setTyp.getEntita().iterator().next(), interpiSystem);
    }

    /**
     * Import rejstříku. Založí nebo aktualizuje rejstřík.
     *
     * @param recordId id rejstříku, pokud je null vytvoří se nový záznam, jinak se aktualizuje
     * @param interpiRecordId id záznamu v INTERPI
     * @param scopeId id scope
     * @param systemId id systému
     *
     * @return nový/aktualizovaný rejstřík
     */
    public RegRecord importRecord(final Integer recordId, final String interpiRecordId, final Integer scopeId, final Integer systemId) {
        Assert.assertNotNull(interpiRecordId);
        Assert.assertNotNull(scopeId);
        Assert.assertNotNull(systemId);

        logger.info("Import záznamu s identifikátorem " + interpiRecordId + " z interpi.");

        RegExternalSystem regExternalSystem = getExternalSystem(systemId);
        SetTyp setTyp = findOneRecord(interpiRecordId, regExternalSystem);

        Map<EntityValueType, List<Object>> valueMap = interpiFactory.convertToMap(setTyp.getEntita().iterator().next());

        RegRecord originalRecord = null;
        List<ParParty> originalParties = null;
        if (recordId != null) {
            originalRecord = regRecordRepository.findOne(recordId);
            originalParties = partyRepository.findParPartyByRecordId(recordId);
        }

        RegScope regScope = scopeRepository.findOne(scopeId);
        RegRecord regRecord = interpiFactory.createRecord(valueMap, interpiRecordId, regExternalSystem, regScope);
        if (originalRecord != null) {
            regRecord.setRecordId(originalRecord.getRecordId());
            regRecord.setVersion(originalRecord.getVersion());
        }
        ParPartyType partyType = regRecord.getRegisterType().getPartyType();
        boolean saveOnlyRecord = partyType == null;

        ParParty newParty = interpiFactory.createParty(regRecord, valueMap);
        newParty.setPartyId(recordId); // TODO vyzkoušet zda se to aktualizuje

        if (saveOnlyRecord) {
            regRecord = registryService.saveRecord(regRecord, false);
        } else {
            if (CollectionUtils.isEmpty(originalParties)) {
                regRecord = partyService.saveParty(newParty).getRecord();
            } else {
                for (ParParty parParty : originalParties) {
                    newParty.setPartyId(parParty.getPartyId());
                    newParty.setVersion(parParty.getVersion());
                    regRecord = partyService.saveParty(newParty).getRecord();
                }
            }
        }

        return regRecord;
    }

    private SetTyp findOneRecord(final String id, final RegExternalSystem regExternalSystem) {
        WssoapSoap client = createClient(regExternalSystem);

        logger.info("Načítání záznamu s identifikátorem " + id + " z interpi.");
        String oneRecord = client.getOneRecord(id, regExternalSystem.getUsername(), regExternalSystem.getPassword());
        SetTyp setTyp = unmarshall(oneRecord);

        if (setTyp.getEntita().isEmpty()) {
            throw new IllegalStateException("Záznam s identifikátorem " + id + " nebyl nalezen v systému " + regExternalSystem);
        }

        return setTyp;
    }

    private WssoapSoap createClient(final RegExternalSystem regExternalSystem) {
        WssoapSoap client = WSUtils.createClient(regExternalSystem.getUrl(), WssoapSoap.class);
        return client;
    }

    private RegExternalSystem getExternalSystem(final Integer systemId) {
        RegExternalSystem regExternalSystem = regExternalSystemRepository.findOne(systemId);
        return regExternalSystem;
    }

    private SetTyp unmarshall(final String oneRecord) {
        return XmlUtils.unmarshallDataWithIntrospector(oneRecord, SetTyp.class);
    }
}
