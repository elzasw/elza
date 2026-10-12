package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.AbstractServiceTest;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.AbstractFilter;
import cz.tacr.elza.controller.vo.AipDetailVO;
import cz.tacr.elza.controller.vo.AipField;
import cz.tacr.elza.controller.vo.AipFieldName;
import cz.tacr.elza.controller.vo.FieldType;
import cz.tacr.elza.controller.vo.FilterType;
import cz.tacr.elza.controller.vo.LinkedNodeVO;
import cz.tacr.elza.controller.vo.LogicalFilter;
import cz.tacr.elza.controller.vo.OperationLogicalType;
import cz.tacr.elza.controller.vo.OperationTextType;
import cz.tacr.elza.controller.vo.SearchParams;
import cz.tacr.elza.controller.vo.TextValueFilter;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaChange;
import cz.tacr.elza.domain.DaChangeType;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.repository.DaChangeRepository;
import cz.tacr.elza.repository.DaDaoRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;
import cz.tacr.elza.service.AipService;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.FundLevelService.AddLevelDirection;

/**
 * The lookup behind the entry URL /da/&lt;repository&gt;/aip/&lt;aipCode&gt;, done by the client with
 * the AIP search: the AIP code of the digital archive within that archive, in any of the forms
 * a UUID is written, and the units linked to it together with the linked part.
 */
public class AipEntryLookupTest extends AbstractServiceTest {

    private static final String REPOSITORY = "DA-ENTRY";
    private static final String AIP_UUID = "1659488e-6b5c-4e3b-99aa-7da151f16ec7";
    private static final String PART_CODE = "uuid-973b4414-03e8-46dd-9746-a5a5c1e02f67";

    @Autowired
    private AipService aipService;
    @Autowired
    private DaService daService;
    @Autowired
    private ClientFactoryVO clientFactoryVO;
    @Autowired
    private AipRepository aipRepository;
    @Autowired
    private AipStateRepository aipStateRepository;
    @Autowired
    private DaChangeRepository changeRepository;
    @Autowired
    private DaDaoRepository daoRepository;
    @Autowired
    private ArrDaLinkRepository daLinkRepository;
    @Autowired
    private DigitalRepositoryRepository digitalRepositoryRepository;
    @Autowired
    private FundLevelService fundLevelService;

    private TransactionTemplate tx() {
        return new TransactionTemplate(txManager);
    }

    @AfterEach
    public void deleteCreatedRows() {
        tx().executeWithoutResult(t -> {
            daLinkRepository.deleteAll();
            daoRepository.deleteAll();
            aipStateRepository.deleteAll();
            changeRepository.deleteAll();
            aipRepository.deleteAll();
            digitalRepositoryRepository.deleteAll();
        });
    }

    private record Aip(Integer aipId, Integer partId) {
    }

    /** An imported AIP of the fund with one logical part, the way the package writes it. */
    private Aip createAip(String repositoryCode, String code, ArrFund fund) {
        return tx().execute(t -> {
            ArrDigitalRepository repository = new ArrDigitalRepository();
            repository.setCode(repositoryCode);
            repository.setName("Testovaci digitalni archiv " + repositoryCode);
            repository.setDigitalRepositoryType(DigitalRepositoryType.DA);
            repository.setSendNotification(false);
            repository.setMultipleLinks(true);
            digitalRepositoryRepository.save(repository);

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
            state.setFund(fund);
            aipStateRepository.save(state);

            DaDao part = daService.createDaDao(aip, change, PART_CODE, "slozka:ttt", DaDao.DaoType.LOGICAL);
            return new Aip(aip.getAipId(), part.getDaoId());
        });
    }

    private Integer childNodeId(FundInfo fund) {
        return tx().execute(t -> {
            ArrNode parent = nodeRepository.findById(fund.getRootNodeId()).orElseThrow();
            List<ArrLevel> levels = fundLevelService.addNewLevel(fund.getFundVersion(), parent, parent,
                    AddLevelDirection.CHILD, null, null, null, null, null);
            return levels.stream().max(Comparator.comparing(ArrLevel::getLevelId)).orElseThrow()
                    .getNode().getNodeId();
        });
    }

    private static TextValueFilter eq(AipFieldName name, String value) {
        TextValueFilter filter = new TextValueFilter(new AipField(name, FieldType.AIP_FIELD), OperationTextType.EQ,
                FilterType.TEXT_VALUE);
        filter.setValue(value);
        return filter;
    }

    /** The search the entry page sends. */
    private List<AipDetailVO> lookup(String repositoryCode, String... codes) {
        List<AbstractFilter> anyCode = List.of(codes).stream().<AbstractFilter>map(c -> eq(AipFieldName.CODE, c))
                .toList();
        SearchParams params = new SearchParams();
        params.setFilters(List.of(eq(AipFieldName.DIGITAL_REPOSITORY_CODE, repositoryCode),
                new LogicalFilter(anyCode, OperationLogicalType.OR, FilterType.LOGICAL)));
        params.setOffset(0);
        params.setSize(2);
        return tx().execute(t -> clientFactoryVO.createAips(aipService.findAipDetailsByFilter(params).getList()));
    }

    /** The client asks for both forms of a UUID; the AIP is found whichever form it is stored in. */
    @Test
    public void findsTheAipInTheGivenArchiveInEitherFormOfTheUuid() {
        for (String stored : List.of(AIP_UUID, "uuid-" + AIP_UUID)) {
            Aip aip = createAip(REPOSITORY, stored, null);

            List<AipDetailVO> found = lookup(REPOSITORY, AIP_UUID, "uuid-" + AIP_UUID);
            assertEquals(1, found.size(), stored);
            assertEquals(aip.aipId(), found.get(0).getAipId());
            assertEquals(REPOSITORY, found.get(0).getDigitalRepositoryCode());
            assertTrue(lookup("DA-OTHER", AIP_UUID, "uuid-" + AIP_UUID).isEmpty(),
                       "the code belongs to its digital archive");
            deleteCreatedRows();
        }
    }

    /** The links say which part they bind, so the entry URL can open the unit of a given part. */
    @Test
    public void linkedNodesCarryTheLinkedPart() {
        FundInfo fund = tx().execute(t -> createFund("F-da-entry"));
        Aip aip = createAip(REPOSITORY, AIP_UUID, fund.getFund());
        Integer wholeNodeId = childNodeId(fund);
        Integer partNodeId = childNodeId(fund);
        tx().executeWithoutResult(t -> daService.connectToJP(wholeNodeId, aip.aipId()));
        daService.createDaoLink(aip.aipId(), aip.partId(), partNodeId, ArrDaoLink.LinkType.PART_AIP);

        AipDetailVO found = lookup(REPOSITORY, AIP_UUID).get(0);

        assertEquals(fund.getFund().getFundId(), found.getFund().getId());
        Map<Integer, LinkedNodeVO> links = found.getLinkedNodes().stream()
                .collect(Collectors.toMap(LinkedNodeVO::getNodeId, l -> l));
        assertEquals(2, links.size());
        assertNull(links.get(wholeNodeId).getDaoCode(), "a link of the whole AIP has no part");
        assertEquals(PART_CODE, links.get(partNodeId).getDaoCode());
    }
}
