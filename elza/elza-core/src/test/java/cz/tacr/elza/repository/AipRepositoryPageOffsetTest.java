package cz.tacr.elza.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.controller.vo.AbstractFilter;
import cz.tacr.elza.controller.vo.AipField;
import cz.tacr.elza.controller.vo.AipFieldName;
import cz.tacr.elza.controller.vo.FieldType;
import cz.tacr.elza.controller.vo.FilterType;
import cz.tacr.elza.controller.vo.OperationTextType;
import cz.tacr.elza.controller.vo.SearchParams;
import cz.tacr.elza.controller.vo.Sorting;
import cz.tacr.elza.controller.vo.SortingOrder;
import cz.tacr.elza.controller.vo.TextValueFilter;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaChange;
import cz.tacr.elza.domain.DaChangeType;

/**
 * Skok na stránku s konkrétním balíčkem.
 *
 * Seznam AIP je stránkovaný a v jednom fondu jich bývají vyšší tisíce, takže odkaz na balíček
 * musí umět spočítat, na které stránce leží - ve zvoleném řazení a při uplatněném filtru.
 * Test staví sedm balíčků, kde se hodnota, podle které se řadí, u části opakuje a u části chybí:
 * to jsou dva případy, ve kterých se pořadí nejsnáz rozjede s tím, co vrátí stránkovaný dotaz.
 *
 * <pre>
 *   kód          aip-offset-1  -2   -3   -4   -5   -6   -7
 *   fundCode     "b"          null  "a"  null "c"  "a"  null
 * </pre>
 */
public class AipRepositoryPageOffsetTest extends AbstractTest {

    @Autowired
    private AipRepository aipRepository;
    @Autowired
    private AipStateRepository aipStateRepository;
    @Autowired
    private DaChangeRepository changeRepository;
    @Autowired
    private DigitalRepositoryRepository digitalRepositoryRepository;

    /** Kód balíčku -> jeho id, v pořadí, ve kterém byly založeny. */
    private final Map<String, Integer> aipIds = new LinkedHashMap<>();

    @BeforeEach
    public void createAips() {
        Map<String, String> fundCodes = new LinkedHashMap<>();
        fundCodes.put("aip-offset-1", "b");
        fundCodes.put("aip-offset-2", null);
        fundCodes.put("aip-offset-3", "a");
        fundCodes.put("aip-offset-4", null);
        fundCodes.put("aip-offset-5", "c");
        fundCodes.put("aip-offset-6", "a");
        fundCodes.put("aip-offset-7", null);

        new TransactionTemplate(txManager).executeWithoutResult(t -> {
            ArrDigitalRepository repository = new ArrDigitalRepository();
            repository.setCode("DA-PAGE-OFFSET");
            repository.setName("Testovaci digitalni archiv");
            repository.setDigitalRepositoryType(DigitalRepositoryType.DA);
            repository.setSendNotification(false);
            repository.setMultipleLinks(true);
            digitalRepositoryRepository.save(repository);

            fundCodes.forEach((code, fundCode) -> {
                DaAip aip = new DaAip();
                aip.setCode(code);
                aip.setDigitalRepository(repository);
                aipRepository.save(aip);

                DaChange change = new DaChange();
                change.setChangeDate(LocalDateTime.now());
                change.setDaAip(aip);
                change.setType(DaChangeType.AIP_CREATE);
                changeRepository.save(change);

                DaAipState state = new DaAipState();
                state.setDaAip(aip);
                state.setCreateChange(change);
                state.setAipVersion("1");
                state.setFundCode(fundCode);
                aipStateRepository.save(state);

                aipIds.put(code, aip.getAipId());
            });
        });
    }

    /** Balíčky drží digitální archiv, který úklid mezi testy sám neodstraní. */
    @AfterEach
    public void deleteCreatedRows() {
        new TransactionTemplate(txManager).executeWithoutResult(t -> {
            aipStateRepository.deleteAll();
            changeRepository.deleteAll();
            aipRepository.deleteAll();
            digitalRepositoryRepository.deleteAll();
        });
        aipIds.clear();
    }

    // --- the position itself --------------------------------------------------------------

    /**
     * Bez zvoleného řazení se řadí podle kódu, takže pátý balíček leží na třetí dvoustránce.
     */
    @Test
    public void testOffsetInDefaultSorting() {
        assertEquals(4, offsetOf("aip-offset-5", params(2)));
    }

    /**
     * Řazení podle pole, kde se hodnoty opakují i chybí: pořadí je "a", "a", "b", "c" a teprve
     * pak prázdné hodnoty, mezi shodnými hodnotami rozhoduje identifikátor.
     */
    @Test
    public void testOffsetBySharedAndMissingValues() {
        SearchParams params = sortedBy(SortingOrder.ASC, 2);

        assertEquals(0, offsetOf("aip-offset-3", params));
        assertEquals(0, offsetOf("aip-offset-6", params));
        assertEquals(2, offsetOf("aip-offset-1", params));
        assertEquals(2, offsetOf("aip-offset-5", params));
        // prázdné hodnoty jsou při vzestupném řazení až za vyplněnými
        assertEquals(4, offsetOf("aip-offset-2", params));
        assertEquals(4, offsetOf("aip-offset-4", params));
        assertEquals(6, offsetOf("aip-offset-7", params));
    }

    /**
     * Při sestupném řazení jsou prázdné hodnoty naopak první; identifikátor řadí vzestupně
     * i tam, protože je to jen rozhodčí shodných hodnot, ne volba uživatele.
     */
    @Test
    public void testOffsetInDescendingSorting() {
        SearchParams params = sortedBy(SortingOrder.DESC, 3);

        assertEquals(0, offsetOf("aip-offset-2", params));
        assertEquals(0, offsetOf("aip-offset-4", params));
        assertEquals(0, offsetOf("aip-offset-7", params));
        assertEquals(3, offsetOf("aip-offset-5", params));
        assertEquals(3, offsetOf("aip-offset-1", params));
        assertEquals(3, offsetOf("aip-offset-3", params));
        assertEquals(6, offsetOf("aip-offset-6", params));
    }

    /**
     * Spočtený offset musí opravdu vést na stránku, na které balíček je - jinak by uživatel
     * skončil u sousedů.
     */
    @Test
    public void testPageAtTheOffsetHoldsTheAip() {
        for (SearchParams params : List.of(params(2), sortedBy(SortingOrder.ASC, 2),
                sortedBy(SortingOrder.DESC, 3))) {
            for (String code : aipIds.keySet()) {
                Integer offset = aipRepository.findAipPageOffset(params, aipIds.get(code));
                params.setOffset(offset);
                List<String> page = aipRepository.findAipsByFilter(params).getList().stream()
                        .map(DaAip::getCode).toList();
                assertTrue(page.contains(code), "stránka od " + offset + " neobsahuje " + code + ": " + page);
            }
        }
    }

    /**
     * Balíček, který filtru neodpovídá, žádnou stránku v seznamu nemá.
     */
    @Test
    public void testAipOutsideTheFilterHasNoPage() {
        SearchParams params = params(2);
        List<AbstractFilter> filters = new ArrayList<>(params.getFilters());
        filters.add(text(AipFieldName.FUND_CODE, OperationTextType.EQ, "a"));
        params.setFilters(filters);

        assertNull(aipRepository.findAipPageOffset(params, aipIds.get("aip-offset-5")));
        assertEquals(0, aipRepository.findAipPageOffset(params, aipIds.get("aip-offset-3")));
    }

    // --- what the position rests on --------------------------------------------------------

    /**
     * Stránkování se nesmí rozjet ani při řazení podle pole s málo různými hodnotami: pořadí
     * mezi shodnými hodnotami drží až identifikátor, jinak by se řádek mohl na dvou stránkách
     * zopakovat a na třetí chybět.
     */
    @Test
    public void testPagingIsStableOverSharedValues() {
        SearchParams params = sortedBy(SortingOrder.ASC, 2);
        List<String> seen = new ArrayList<>();
        for (int offset = 0; offset < aipIds.size(); offset += 2) {
            params.setOffset(offset);
            aipRepository.findAipsByFilter(params).getList().forEach(aip -> seen.add(aip.getCode()));
        }

        assertEquals(aipIds.keySet().stream().sorted().toList(), seen.stream().sorted().toList());
    }

    // --- helpers ---------------------------------------------------------------------------

    private Integer offsetOf(final String code, final SearchParams params) {
        return aipRepository.findAipPageOffset(params, aipIds.get(code));
    }

    /**
     * Balíčky tohoto testu; ostatní data v databázi do pořadí nesmí mluvit.
     */
    private SearchParams params(final int pageSize) {
        SearchParams params = new SearchParams();
        params.setFilters(List.of(text(AipFieldName.CODE, OperationTextType.CONTAINS, "aip-offset-")));
        params.setOffset(0);
        params.setSize(pageSize);
        return params;
    }

    private SearchParams sortedBy(final SortingOrder order, final int pageSize) {
        SearchParams params = params(pageSize);
        params.setSort(List.of(new Sorting(AipFieldName.FUND_CODE.getValue()).order(order)));
        return params;
    }

    private static TextValueFilter text(final AipFieldName name, final OperationTextType operation,
                                        final String value) {
        TextValueFilter filter = new TextValueFilter(new AipField(name, FieldType.AIP_FIELD), operation,
                FilterType.TEXT_VALUE);
        filter.setValue(value);
        return filter;
    }
}
