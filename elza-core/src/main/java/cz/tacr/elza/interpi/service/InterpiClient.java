package cz.tacr.elza.interpi.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.RegExternalSystem;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ExternalCode;
import cz.tacr.elza.interpi.ws.WssoapSoap;
import cz.tacr.elza.interpi.ws.wo.EntitaTyp;
import cz.tacr.elza.interpi.ws.wo.SetTyp;
import cz.tacr.elza.utils.WSUtils;
import cz.tacr.elza.utils.XmlUtils;

/**
 * Klient pro WS INTERPI.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 20. 12. 2016
 */
@Service
public class InterpiClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** Stránkování. */
    private static final String FROM = "1";
    private static final String TO = "500";

    /** Prefix pro chybovou hlášku. */
    private static final String ERROR_PREFIX = "ERR";

    public List<EntitaTyp> findRecords(final String query, final Integer count, final RegExternalSystem regExternalSystem) {
        WssoapSoap client = createClient(regExternalSystem);

        Integer maxCount = count == null ? Integer.valueOf(TO) : count;

        logger.info("Vyhledávání v interpi: " + query);
        long start = System.currentTimeMillis();
        String searchResult = client.findData(query, null, FROM, maxCount.toString(), regExternalSystem.getUsername(), regExternalSystem.getPassword());
        long end = System.currentTimeMillis();
        logger.debug("Dotaz do INTERPI trval " + (end - start) + " ms.");

        SetTyp setTyp = unmarshall(searchResult);

        List<EntitaTyp> records = setTyp.getEntita();
        int searchResultCount = records.size() > maxCount ? maxCount : records.size();

        return records.subList(0, searchResultCount); // INTERPI neumí stránkovat
    }

    public EntitaTyp findOneRecord(final String id, final RegExternalSystem regExternalSystem) {
        WssoapSoap client = createClient(regExternalSystem);

        logger.info("Načítání záznamu s identifikátorem " + id + " z interpi.");
        long start = System.currentTimeMillis();
        String oneRecord = client.getOneRecord(id, regExternalSystem.getUsername(), regExternalSystem.getPassword());
        long end = System.currentTimeMillis();
        logger.debug("Dotaz do INTERPI trval " + (end - start) + " ms.");

        if (oneRecord.startsWith(ERROR_PREFIX)) {
             throw new SystemException("Nastala chyba " + oneRecord + " při hledání záznamu s identifikátorem " + id + " v systému " + regExternalSystem, ExternalCode.EXTERNAL_SYSTEM_ERROR);
        }
        SetTyp setTyp = unmarshall(oneRecord);

        if (setTyp.getEntita().isEmpty()) {
            throw new SystemException("Záznam s identifikátorem " + id + " nebyl nalezen v systému " + regExternalSystem);
        }

        return setTyp.getEntita().iterator().next();
    }

    private WssoapSoap createClient(final RegExternalSystem regExternalSystem) {
        return WSUtils.createClient(regExternalSystem.getUrl(), WssoapSoap.class);
    }

    private SetTyp unmarshall(final String oneRecord) {
        return XmlUtils.unmarshall(oneRecord, SetTyp.class);
    }
}
