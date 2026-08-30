package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.AbstractServiceTest;
import cz.tacr.elza.api.AipLinkState;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDigitalRepository;
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
import cz.tacr.elza.repository.DaDaoRelationRepository;
import cz.tacr.elza.repository.DaDaoRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.FundLevelService.AddLevelDirection;

/**
 * How much of an AIP hangs on the archival description.
 *
 * The package built here has the shape a real one has: seven files, each sitting both under the
 * representation and under a level of the logical structure, so every file is reachable by two
 * different paths. That is what makes the answer a question about what the links reach rather than
 * about which links exist.
 *
 * <pre>
 *   root → l1 → l2 ─┬→ file0
 *                   └→ leaf1..leaf6 → file1..file6
 *   representation ─→ file0..file6
 * </pre>
 */
public class DaAipLinkStateResolverTest extends AbstractServiceTest {

    private static final int FILE_COUNT = 7;

    @Autowired
    private DaService daService;
    @Autowired
    private DaAipLinkStateResolver linkStateResolver;
    @Autowired
    private ArrDaLinkRepository daLinkRepository;
    @Autowired
    private AipRepository aipRepository;
    @Autowired
    private AipStateRepository aipStateRepository;
    @Autowired
    private DaChangeRepository changeRepository;
    @Autowired
    private DaDaoRepository daoRepository;
    @Autowired
    private DaDaoRelationRepository daoRelationRepository;
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
            daoRelationRepository.deleteAll();
            daoRepository.deleteAll();
            aipStateRepository.deleteAll();
            changeRepository.deleteAll();
            aipRepository.deleteAll();
            digitalRepositoryRepository.deleteAll();
        });
    }

    /** What the test needs to reach into the package it built. */
    private record Package(Integer aipId, Integer representationId, Integer rootLogicalId,
                           Integer leafLogicalId, List<Integer> fileIds) {
    }

    private DaAip createAipWithState(String suffix) {
        ArrDigitalRepository repository = new ArrDigitalRepository();
        repository.setCode("DA-LINK-STATE-" + suffix);
        repository.setName("Testovaci digitalni archiv");
        repository.setDigitalRepositoryType(DigitalRepositoryType.DA);
        repository.setSendNotification(false);
        repository.setMultipleLinks(true);
        digitalRepositoryRepository.save(repository);

        DaAip aip = new DaAip();
        aip.setCode("aip-link-state-" + suffix);
        aip.setDigitalRepository(repository);
        aipRepository.save(aip);

        DaChange change = new DaChange();
        change.setChangeDate(java.time.LocalDateTime.now());
        change.setDaAip(aip);
        change.setType(DaChangeType.AIP_CREATE);
        changeRepository.save(change);

        DaAipState state = new DaAipState();
        state.setDaAip(aip);
        state.setCreateChange(change);
        state.setAipVersion("1");
        aipStateRepository.save(state);
        return aip;
    }

    /** An AIP whose package has never been read - no digital entities at all. */
    private Integer createEmptyAip() {
        return tx().execute(t -> createAipWithState("empty").getAipId());
    }

    private Package createPackage() {
        return tx().execute(t -> {
            DaAip aip = createAipWithState("pkg");
            DaChange change = daService.createDaChange(aip, DaChangeType.AIP_UPDATE);

            DaDao representation = daService.createDaDao(aip, change, "repr", "Reprezentace",
                                                          DaDao.DaoType.REPRESENTATION);
            DaDao root = daService.createDaDao(aip, change, "root", "Root", DaDao.DaoType.LOGICAL);
            DaDao l1 = daService.createDaDao(aip, change, "l1", "L1", DaDao.DaoType.LOGICAL);
            DaDao l2 = daService.createDaDao(aip, change, "l2", "L2", DaDao.DaoType.LOGICAL);
            daService.createDaDaoRelation(l1, root, change);
            daService.createDaDaoRelation(l2, l1, change);

            // metadata entities hang off nothing, exactly as the package processor leaves them
            daService.createDaDao(aip, change, "amd", "PREMIS", DaDao.DaoType.METAAMD);
            daService.createDaDao(aip, change, "dmd", "EAD", DaDao.DaoType.METADMDINHERENT);

            List<Integer> fileIds = new java.util.ArrayList<>();
            for (int i = 0; i < FILE_COUNT; i++) {
                DaDao file = daService.createDaDao(aip, change, "file" + i, "Soubor " + i,
                                                   DaDao.DaoType.FILE);
                fileIds.add(file.getDaoId());
                daService.createDaDaoRelation(file, representation, change);
                if (i == 0) {
                    // the first file hangs directly under l2, the rest each under their own leaf
                    daService.createDaDaoRelation(file, l2, change);
                } else {
                    DaDao leaf = daService.createDaDao(aip, change, "leaf" + i, "Leaf " + i,
                                                       DaDao.DaoType.LOGICAL);
                    daService.createDaDaoRelation(leaf, l2, change);
                    daService.createDaDaoRelation(file, leaf, change);
                }
            }
            return new Package(aip.getAipId(), representation.getDaoId(), root.getDaoId(),
                               l2.getDaoId(), fileIds);
        });
    }

    private Integer nodeId(FundInfo fund) {
        return tx().execute(t -> {
            ArrNode parent = nodeRepository.findById(fund.getRootNodeId()).orElseThrow();
            List<ArrLevel> levels = fundLevelService.addNewLevel(fund.getFundVersion(), parent, parent,
                    AddLevelDirection.CHILD, null, null, null, null, null);
            return levels.stream().max(Comparator.comparing(ArrLevel::getLevelId)).orElseThrow()
                    .getNode().getNodeId();
        });
    }

    private AipLinkState storedState(Integer aipId) {
        return tx().execute(t -> aipStateRepository
                .findByDaAipAndDeleteChangeIsNull(aipRepository.findById(aipId).orElseThrow())
                .getLinkState());
    }

    /** The stored value must always equal what a fresh computation says. */
    private void assertNoDrift(Integer aipId) {
        tx().executeWithoutResult(t -> {
            DaAip aip = aipRepository.findById(aipId).orElseThrow();
            assertEquals(linkStateResolver.computeLinkState(aip),
                         aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip).getLinkState(),
                         "the stored link state has drifted from the truth");
        });
    }

    @Test
    public void anAipNobodyAttachedIsNotLinked() {
        Package pkg = createPackage();
        assertEquals(AipLinkState.NOT_LINKED, storedState(pkg.aipId()));
        assertNoDrift(pkg.aipId());
    }

    @Test
    public void attachingTheWholePackageIsFullyLinked() {
        FundInfo fund = tx().execute(t -> createFund("F-link-state-whole"));
        Package pkg = createPackage();

        tx().executeWithoutResult(t -> daService.connectToJP(fund.getRootNodeId(), pkg.aipId()));

        assertEquals(AipLinkState.FULLY_LINKED, storedState(pkg.aipId()));
        assertNoDrift(pkg.aipId());
    }

    @Test
    public void attachingOneFileIsPartiallyLinked() {
        FundInfo fund = tx().execute(t -> createFund("F-link-state-one-file"));
        Package pkg = createPackage();

        tx().executeWithoutResult(t -> daService.createDaoLink(pkg.aipId(), pkg.fileIds().get(0),
                fund.getRootNodeId(), ArrDaoLink.LinkType.COMPONENT_AIP));

        assertEquals(AipLinkState.PARTIALLY_LINKED, storedState(pkg.aipId()));
        assertNoDrift(pkg.aipId());
    }

    /** The representation holds every file directly, so attaching it attaches all of them. */
    @Test
    public void attachingTheRepresentationIsFullyLinked() {
        FundInfo fund = tx().execute(t -> createFund("F-link-state-repr"));
        Package pkg = createPackage();

        tx().executeWithoutResult(t -> daService.createDaoLink(pkg.aipId(), pkg.representationId(),
                fund.getRootNodeId(), ArrDaoLink.LinkType.PART_AIP));

        assertEquals(AipLinkState.FULLY_LINKED, storedState(pkg.aipId()));
        assertNoDrift(pkg.aipId());
    }

    /** The root of the logical structure reaches every file through the levels below it. */
    @Test
    public void attachingTheRootOfTheLogicalStructureIsFullyLinked() {
        FundInfo fund = tx().execute(t -> createFund("F-link-state-root"));
        Package pkg = createPackage();

        tx().executeWithoutResult(t -> daService.createDaoLink(pkg.aipId(), pkg.rootLogicalId(),
                fund.getRootNodeId(), ArrDaoLink.LinkType.PART_AIP));

        assertEquals(AipLinkState.FULLY_LINKED, storedState(pkg.aipId()));
        assertNoDrift(pkg.aipId());
    }

    /** Detaching the only link puts the AIP back where it started. */
    @Test
    public void detachingReturnsTheAipToNotLinked() {
        FundInfo fund = tx().execute(t -> createFund("F-link-state-detach"));
        Package pkg = createPackage();

        Integer linkId = tx().execute(t ->
                daService.connectToJP(fund.getRootNodeId(), pkg.aipId()).getDaoLinkId());
        assertEquals(AipLinkState.FULLY_LINKED, storedState(pkg.aipId()));

        tx().executeWithoutResult(t -> daService.deleteDaoLink(linkId));

        assertEquals(AipLinkState.NOT_LINKED, storedState(pkg.aipId()));
        assertNoDrift(pkg.aipId());
    }

    /**
     * An AIP whose package has not been read yet must never read as fully linked off an empty set of
     * files - "everything is attached" would be true of nothing and would say more than is known.
     */
    @Test
    public void anAipWithoutContentIsNeverFullyLinkedByAPartLink() {
        FundInfo fund = tx().execute(t -> createFund("F-link-state-empty"));
        Package pkg = createPackage();
        Integer emptyAipId = createEmptyAip();

        // a part link on an AIP that has no files at all
        tx().executeWithoutResult(t -> daService.createDaoLink(emptyAipId, pkg.fileIds().get(0),
                fund.getRootNodeId(), ArrDaoLink.LinkType.COMPONENT_AIP));

        assertEquals(AipLinkState.PARTIALLY_LINKED, storedState(emptyAipId));
        assertNoDrift(emptyAipId);
    }

    /** Attaching the whole package says everything is attached whatever it turns out to hold. */
    @Test
    public void anAipWithoutContentIsFullyLinkedByAWholePackageLink() {
        FundInfo fund = tx().execute(t -> createFund("F-link-state-empty-whole"));
        Integer emptyAipId = createEmptyAip();

        tx().executeWithoutResult(t -> daService.connectToJP(fund.getRootNodeId(), emptyAipId));

        assertEquals(AipLinkState.FULLY_LINKED, storedState(emptyAipId));
        assertNoDrift(emptyAipId);
    }

    /**
     * Attaching every leaf one at a time: partial until the last one, full once nothing is left out.
     * This is the case a definition based on link rows alone would get wrong.
     */
    @Test
    public void attachingEveryFileOneByOneEndsFullyLinked() {
        FundInfo fund = tx().execute(t -> createFund("F-link-state-each"));
        Package pkg = createPackage();
        Integer targetNodeId = nodeId(fund);

        for (int i = 0; i < FILE_COUNT; i++) {
            Integer fileId = pkg.fileIds().get(i);
            tx().executeWithoutResult(t -> daService.createDaoLink(pkg.aipId(), fileId, targetNodeId,
                    ArrDaoLink.LinkType.COMPONENT_AIP));
            assertNoDrift(pkg.aipId());
            assertEquals(i == FILE_COUNT - 1 ? AipLinkState.FULLY_LINKED : AipLinkState.PARTIALLY_LINKED,
                         storedState(pkg.aipId()),
                         "after attaching " + (i + 1) + " of " + FILE_COUNT + " files");
        }
    }
}
