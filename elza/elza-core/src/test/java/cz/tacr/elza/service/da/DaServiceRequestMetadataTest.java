package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.AbstractServiceTest;
import cz.tacr.elza.api.AipType;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaChange;
import cz.tacr.elza.domain.DaChangeType;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.DaAipActionItemRepository;
import cz.tacr.elza.repository.DaAipActionRepository;
import cz.tacr.elza.repository.DaChangeRepository;
import cz.tacr.elza.repository.DaSyncQueueItemRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;

/**
 * Requesting the metadata of an AIP puts it in the download queue.
 *
 * The whole request runs in one transaction of the service's own, not in one the caller happens to
 * hold. Two things depend on it: the AIPs and their states are read by separate queries and paired
 * by identity, which needs one persistence context, and deactivating the queue items the new one
 * replaces is a bulk update JPA refuses outside a transaction. The service is therefore called with
 * no transaction around it - one of the test's own would hide exactly what is being checked - and
 * against a database, because neither of the two can be seen without one.
 */
public class DaServiceRequestMetadataTest extends AbstractServiceTest {

    @Autowired
    private DaService daService;
    @Autowired
    private AipRepository aipRepository;
    @Autowired
    private AipStateRepository aipStateRepository;
    @Autowired
    private DaChangeRepository changeRepository;
    @Autowired
    private DaSyncQueueItemRepository syncQueueItemRepository;
    @Autowired
    private DigitalRepositoryRepository digitalRepositoryRepository;
    @Autowired
    private DaAipActionRepository actionRepository;
    @Autowired
    private DaAipActionItemRepository actionItemRepository;

    private TransactionTemplate tx() {
        return new TransactionTemplate(txManager);
    }

    /**
     * The shared cleanup of the base class does not know the tables of the digital archive, and the
     * rows created here would keep the next test from deleting the external systems they point at.
     */
    @AfterEach
    public void deleteCreatedRows() {
        tx().executeWithoutResult(t -> {
            actionItemRepository.deleteAll();
            actionRepository.deleteAll();
            syncQueueItemRepository.deleteAll();
            aipStateRepository.deleteAll();
            changeRepository.deleteAll();
            aipRepository.deleteAll();
            digitalRepositoryRepository.deleteAll();
        });
    }

    private ArrDigitalRepository createRepository() {
        ArrDigitalRepository repository = new ArrDigitalRepository();
        repository.setCode("DA-REQUEST-METADATA");
        repository.setName("Testovaci digitalni archiv");
        repository.setDigitalRepositoryType(DigitalRepositoryType.DA);
        repository.setSendNotification(false);
        return digitalRepositoryRepository.save(repository);
    }

    /**
     * An AIP attached to a fund and with nothing downloaded yet - the only combination that reaches
     * the queue; everything else is skipped with a reason and touches no row.
     */
    private DaAip createAipToDownload(ArrDigitalRepository repository, ArrFund fund) {
        DaAip aip = new DaAip();
        aip.setCode("aip-request-metadata");
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
        return aip;
    }

    @Test
    public void requestingMetadataQueuesTheDownload() {
        ArrFund fund = tx().execute(t -> createFund("F-da-request-metadata").getFund());
        Integer aipId = tx().execute(t -> createAipToDownload(createRepository(), fund).getAipId());

        // deliberately outside tx(): the service has to bring its own transaction
        daService.requestMetadata(List.of(aipId));

        tx().executeWithoutResult(t -> {
            DaAip aip = aipRepository.findById(aipId).orElseThrow();
            DaSyncQueueItem queued = syncQueueItemRepository.findByAipAndStateInAndActiveIsTrue(aip,
                    List.of(DaSyncQueueItem.QueueItemState.UPDATE));
            assertNotNull(queued, "requesting the metadata has to leave an item in the queue");
            assertEquals(AipType.METADATA_BASE, queued.getAipType());
        });
    }
}
