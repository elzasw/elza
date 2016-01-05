package cz.tacr.elza.service;

import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;


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
     * Smaže rej. heslo a jeho variantní hesla. Předpokládá, že již proběhlo ověření, že je možné ho smazat (vazby atd...).
     * @param record heslo
     */
    public void deleteRecord(final RegRecord record) {
        variantRecordRepository.delete(variantRecordRepository.findByRegRecordId(record.getRecordId()));
        regRecordRepository.delete(record);
    }

}
