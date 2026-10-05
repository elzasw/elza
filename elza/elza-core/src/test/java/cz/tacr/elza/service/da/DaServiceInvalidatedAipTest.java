package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.da.controller.vo.UpdatedInfo;
import cz.tacr.elza.AbstractServiceTest;
import cz.tacr.elza.api.AipType;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.controller.vo.SearchParams;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDaLink;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaChange;
import cz.tacr.elza.domain.DaChangeType;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.repository.DaAipActionItemRepository;
import cz.tacr.elza.repository.DaAipActionRepository;
import cz.tacr.elza.repository.DaChangeRepository;
import cz.tacr.elza.repository.DaDaoRepository;
import cz.tacr.elza.repository.DaLocalCacheRepository;
import cz.tacr.elza.repository.DaSyncQueueItemRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;
import cz.tacr.elza.service.AipService;

/**
 * The digital archive reports an AIP as invalidated: the package no longer exists for it, so it has
 * to disappear from ELZA - from the archival description it was attached to, from the AIP list and
 * from the queue - while what happened stays readable in the history.
 */
public class DaServiceInvalidatedAipTest extends AbstractServiceTest {

    private static final String CODE = "3f0b9a52-6c1e-4a59-9f6b-2a7d1b0c8e11";

    @Autowired
    private DaService daService;
    @Autowired
    private AipService aipService;
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
    private DaSyncQueueItemRepository syncQueueItemRepository;
    @Autowired
    private DaAipActionRepository actionRepository;
    @Autowired
    private DaAipActionItemRepository actionItemRepository;
    @Autowired
    private DigitalRepositoryRepository digitalRepositoryRepository;
    @Autowired
    private DaLocalCacheRepository localCacheRepository;

    private TransactionTemplate tx() {
        return new TransactionTemplate(txManager);
    }

    @AfterEach
    public void deleteCreatedRows() {
        tx().executeWithoutResult(t -> {
            localCacheRepository.deleteAll();
            syncQueueItemRepository.deleteAll();
            actionItemRepository.deleteAll();
            actionRepository.deleteAll();
            daLinkRepository.deleteAll();
            daoRepository.deleteAll();
            aipStateRepository.deleteAll();
            changeRepository.deleteAll();
            aipRepository.deleteAll();
            digitalRepositoryRepository.deleteAll();
        });
    }

    private ArrDigitalRepository createRepository() {
        ArrDigitalRepository repository = new ArrDigitalRepository();
        repository.setCode("DA-INVALIDATED");
        repository.setName("Testovaci digitalni archiv");
        repository.setDigitalRepositoryType(DigitalRepositoryType.DA);
        repository.setSendNotification(false);
        repository.setMultipleLinks(false);
        return digitalRepositoryRepository.save(repository);
    }

    /** An AIP as the import leaves it: with an active state and one digital entity. */
    private Integer createAip(ArrDigitalRepository repository) {
        DaAip aip = new DaAip();
        aip.setCode(CODE);
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
        aipStateRepository.save(state);

        daService.createDaDao(aip, change, "dao-1", "Digitalni entita", DaDao.DaoType.LOGICAL);
        return aip.getAipId();
    }

    private static UpdatedInfo update(String version, Boolean invalidated) {
        UpdatedInfo info = new UpdatedInfo();
        info.setAipId(CODE);
        info.setAipVersion(version);
        info.setInvalidated(invalidated);
        return info;
    }

    private void sync(Integer repositoryId, UpdatedInfo info) {
        tx().executeWithoutResult(t -> daService.processUpdates(
                digitalRepositoryRepository.findById(repositoryId).orElseThrow(), List.of(info)));
    }

    private List<DaSyncQueueItem> activeQueueItems() {
        return syncQueueItemRepository.findAll().stream().filter(q -> Boolean.TRUE.equals(q.getActive())).toList();
    }

    @Test
    public void invalidatedAipIsWithdrawnWithItsHistoryKept() {
        FundInfo fund = tx().execute(t -> createFund("F-da-invalidated"));
        Integer repositoryId = tx().execute(t -> createRepository().getExternalSystemId());
        Integer aipId = tx().execute(t -> createAip(digitalRepositoryRepository.findById(repositoryId).orElseThrow()));
        Integer linkId = tx().execute(t -> daService.connectToJP(fund.getRootNodeId(), aipId).getDaoLinkId());
        // a download still waiting for the AIP
        tx().executeWithoutResult(t -> daService.createSyncQueueItem(CODE, aipRepository.findById(aipId).orElseThrow(),
                digitalRepositoryRepository.findById(repositoryId).orElseThrow(),
                DaSyncQueueItem.QueueItemState.UPDATE, "1", AipType.PACKAGE_INFO, true));

        sync(repositoryId, update("2", true));

        tx().executeWithoutResult(t -> {
            DaAip aip = aipRepository.findById(aipId).orElseThrow();
            assertNull(aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip), "the state is closed");

            DaAipState closed = aipStateRepository.findAll().get(0);
            assertNotNull(closed.getDeleteChange());
            assertEquals(DaChangeType.AIP_INVALIDATE, closed.getDeleteChange().getType());

            ArrDaLink link = daLinkRepository.findById(linkId).orElseThrow();
            assertNotNull(link.getDeleteChange(), "the link to the unit of description is closed");
            assertEquals(ArrChange.Type.DELETE_DAO_LINK, link.getDeleteChange().getType());

            assertTrue(daoRepository.findByAipAndDeleteChangeIsNull(aip).isEmpty(), "the digital entities are closed");
            assertEquals(1, daoRepository.findAll().size(), "the digital entities are kept for the history");

            assertTrue(activeQueueItems().isEmpty(), "the waiting download is withdrawn");
        });

        assertEquals(0, aipService.findAipDetailsByFilter(new SearchParams()).getTotalCount(),
                     "the AIP is not listed");
        assertThrows(ObjectNotFoundException.class, () -> aipService.getAip(aipId));
    }

    /** An export waiting to be sent is dropped together with the package prepared for it. */
    @Test
    public void pendingExportIsDropped() throws IOException {
        Integer repositoryId = tx().execute(t -> createRepository().getExternalSystemId());
        Integer aipId = tx().execute(t -> createAip(digitalRepositoryRepository.findById(repositoryId).orElseThrow()));
        Path exportZip = Files.createTempFile("da-export", ".zip");
        tx().executeWithoutResult(t -> {
            DaAip aip = aipRepository.findById(aipId).orElseThrow();
            DaSyncQueueItem item = daService.createSyncQueueItem(CODE, aip, aip.getDigitalRepository(),
                    DaSyncQueueItem.QueueItemState.EXPORT_NEW, "1", AipType.PACKAGE_INFO, true);
            daService.createExportLocalCache(aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip),
                    AipType.PACKAGE_INFO, exportZip, item);
        });

        sync(repositoryId, update("2", true));

        tx().executeWithoutResult(t -> {
            assertTrue(activeQueueItems().isEmpty(), "the waiting export is withdrawn");
            assertTrue(localCacheRepository.findAll().isEmpty(), "the prepared package is forgotten");
        });
        assertFalse(Files.exists(exportZip), "the prepared package is deleted");
    }

    @Test
    public void invalidatedAipCannotBeAttached() {
        FundInfo fund = tx().execute(t -> createFund("F-da-invalidated-attach"));
        Integer repositoryId = tx().execute(t -> createRepository().getExternalSystemId());
        Integer aipId = tx().execute(t -> createAip(digitalRepositoryRepository.findById(repositoryId).orElseThrow()));

        sync(repositoryId, update("2", true));

        assertThrows(BusinessException.class,
                () -> tx().executeWithoutResult(t -> daService.connectToJP(fund.getRootNodeId(), aipId)));
        tx().executeWithoutResult(t -> assertTrue(daLinkRepository.findByAipIdAndDeleteChangeIsNull(aipId).isEmpty()));
    }

    @Test
    public void unknownInvalidatedAipIsNotImported() {
        Integer repositoryId = tx().execute(t -> createRepository().getExternalSystemId());

        sync(repositoryId, update("1", true));

        tx().executeWithoutResult(t -> {
            assertNull(aipRepository.findByCode(CODE));
            assertTrue(syncQueueItemRepository.findAll().isEmpty(), "nothing is queued for download");
        });
    }

    /**
     * A valid version of an invalidated AIP is a new package for ELZA: it is downloaded like one never
     * seen, and the links it had before are not restored.
     */
    @Test
    public void validVersionOfInvalidatedAipIsImportedAsNew() {
        FundInfo fund = tx().execute(t -> createFund("F-da-invalidated-again"));
        Integer repositoryId = tx().execute(t -> createRepository().getExternalSystemId());
        Integer aipId = tx().execute(t -> createAip(digitalRepositoryRepository.findById(repositoryId).orElseThrow()));
        tx().executeWithoutResult(t -> daService.connectToJP(fund.getRootNodeId(), aipId));

        sync(repositoryId, update("2", true));
        sync(repositoryId, update("3", null));

        tx().executeWithoutResult(t -> {
            List<DaSyncQueueItem> queued = activeQueueItems();
            assertEquals(1, queued.size());
            DaSyncQueueItem item = queued.get(0);
            assertEquals(DaSyncQueueItem.QueueItemState.IMPORT_NEW, item.getState());
            assertEquals(AipType.PACKAGE_INFO, item.getAipType());
            assertEquals("3", item.getAipVersion());
            assertEquals(aipId, item.getAip().getAipId());

            assertTrue(daLinkRepository.findByAipIdAndDeleteChangeIsNull(aipId).isEmpty(),
                       "the links from before the invalidation stay closed");
            assertFalse(daLinkRepository.findAll().isEmpty());
        });
    }
}
