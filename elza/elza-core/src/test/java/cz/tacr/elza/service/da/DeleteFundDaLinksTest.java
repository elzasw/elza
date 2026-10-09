package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.AbstractServiceTest;
import cz.tacr.elza.api.AipProblemType;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaChange;
import cz.tacr.elza.domain.DaChangeType;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.repository.DaChangeRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;
import cz.tacr.elza.repository.FundRepository;

/**
 * Deleting a fund whose nodes are or were linked to AIPs of a digital archive.
 */
public class DeleteFundDaLinksTest extends AbstractServiceTest {

    private static final String FUND_CODE = "F-DEL-DA";

    @Autowired
    private DaService daService;
    @Autowired
    private ArrDaLinkRepository daLinkRepository;
    @Autowired
    private AipRepository aipRepository;
    @Autowired
    private AipStateRepository aipStateRepository;
    @Autowired
    private DaChangeRepository changeRepository;
    @Autowired
    private DigitalRepositoryRepository digitalRepositoryRepository;
    @Autowired
    private FundRepository fundRepository;

    private TransactionTemplate tx() {
        return new TransactionTemplate(txManager);
    }

    @AfterEach
    public void deleteCreatedRows() {
        tx().executeWithoutResult(t -> {
            daLinkRepository.deleteAll();
            aipStateRepository.deleteAll();
            changeRepository.deleteAll();
            aipRepository.deleteAll();
            digitalRepositoryRepository.deleteAll();
        });
    }

    private record PairedAip(Integer aipId, Integer activeStateId, Integer historicalStateId) {
    }

    /**
     * An AIP paired with the fund: both its active and a superseded state reference the fund.
     */
    private PairedAip createPairedAip(final ArrFund fund) {
        ArrDigitalRepository repository = new ArrDigitalRepository();
        repository.setCode("DA-DEL-FUND-TEST");
        repository.setName("Testovaci digitalni archiv");
        repository.setDigitalRepositoryType(DigitalRepositoryType.DA);
        repository.setSendNotification(false);
        repository.setMultipleLinks(false);
        digitalRepositoryRepository.save(repository);

        DaAip aip = new DaAip();
        aip.setCode("aip-del-fund-test");
        aip.setDigitalRepository(repository);
        aipRepository.save(aip);

        DaChange createChange = createChange(aip, DaChangeType.AIP_CREATE);
        DaChange updateChange = createChange(aip, DaChangeType.AIP_UPDATE);

        DaAipState historical = createState(aip, fund, createChange);
        historical.setDeleteChange(updateChange);
        aipStateRepository.save(historical);
        DaAipState active = createState(aip, fund, updateChange);

        return new PairedAip(aip.getAipId(), active.getAipStateId(), historical.getAipStateId());
    }

    private DaChange createChange(final DaAip aip, final DaChangeType type) {
        DaChange change = new DaChange();
        change.setChangeDate(LocalDateTime.now());
        change.setDaAip(aip);
        change.setType(type);
        return changeRepository.save(change);
    }

    private DaAipState createState(final DaAip aip, final ArrFund fund, final DaChange change) {
        DaAipState state = new DaAipState();
        state.setDaAip(aip);
        state.setCreateChange(change);
        state.setAipVersion("1");
        state.setFundCode(FUND_CODE);
        state.setFund(fund);
        state.setInstitution(firstInstitution);
        return aipStateRepository.save(state);
    }

    @Test
    public void activeLinkBlocksTheDeletion() {
        FundInfo fund = tx().execute(t -> createFund("F-del-da-active", FUND_CODE));
        PairedAip aip = tx().execute(t -> createPairedAip(fund.getFund()));
        tx().executeWithoutResult(t -> daService.connectToJP(fund.getRootNodeId(), aip.aipId()));

        BusinessException e = assertThrows(BusinessException.class,
                () -> arrangementService.deleteFund(fund.getFund().getFundId()));
        assertEquals(ArrangementCode.FUND_HAS_DA_LINKS, e.getErrorCode());

        tx().executeWithoutResult(t -> {
            assertTrue(fundRepository.existsById(fund.getFund().getFundId()));
            assertEquals(1, daLinkRepository.findByAipIdAndDeleteChangeIsNull(aip.aipId()).size());
            assertNotNull(aipStateRepository.findById(aip.activeStateId()).orElseThrow().getFund());
        });
    }

    @Test
    public void deletedLinkDoesNotBlockAndTheAipIsUnpaired() {
        FundInfo fund = tx().execute(t -> createFund("F-del-da-deleted", FUND_CODE));
        PairedAip aip = tx().execute(t -> createPairedAip(fund.getFund()));
        Integer linkId = tx().execute(t -> daService.connectToJP(fund.getRootNodeId(), aip.aipId()).getDaoLinkId());
        tx().executeWithoutResult(t -> daService.deleteDaoLink(linkId));

        arrangementService.deleteFund(fund.getFund().getFundId());

        tx().executeWithoutResult(t -> {
            assertFalse(fundRepository.existsById(fund.getFund().getFundId()));
            assertFalse(daLinkRepository.existsById(linkId), "the historical link goes with the nodes");

            DaAipState active = aipStateRepository.findById(aip.activeStateId()).orElseThrow();
            assertNull(active.getFund());
            assertEquals(AipProblemType.UNKNOWN_FUND, active.getProblemType());
            assertNotNull(active.getProblemDescription());

            assertNull(aipStateRepository.findById(aip.historicalStateId()).orElseThrow().getFund());
        });
    }
}
