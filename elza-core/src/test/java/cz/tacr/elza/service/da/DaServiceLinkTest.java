package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.AbstractServiceTest;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.api.DaAipActionState;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipAction;
import cz.tacr.elza.domain.DaAipActionItem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.repository.DaAipActionItemRepository;
import cz.tacr.elza.repository.DaAipActionRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.FundLevelService.AddLevelDirection;

/**
 * "Vícenásobné napojení" on the path of the digital archive.
 *
 * The rule was implemented twice before this - for the legacy DAO and for a filesystem repository -
 * and not a third time when the digital archive was added, so an AIP could be attached to any number
 * of units of description whatever the repository said. These pin the behaviour that replaced it,
 * against a database, because attaching is only meaningful once the links are really stored.
 */
public class DaServiceLinkTest extends AbstractServiceTest {

    @Autowired
    private DaService daService;
    @Autowired
    private ArrDaLinkRepository daLinkRepository;
    @Autowired
    private AipRepository aipRepository;
    @Autowired
    private DigitalRepositoryRepository digitalRepositoryRepository;
    @Autowired
    private FundLevelService fundLevelService;
    @Autowired
    private DaAipActionRepository actionRepository;
    @Autowired
    private DaAipActionItemRepository actionItemRepository;
    @Autowired
    private DaAipActionService actionService;

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
            daLinkRepository.deleteAll();
            aipRepository.deleteAll();
            digitalRepositoryRepository.deleteAll();
        });
    }

    private ArrDigitalRepository createRepository(final boolean multipleLinks) {
        ArrDigitalRepository repository = new ArrDigitalRepository();
        repository.setCode("DA-LINK-TEST");
        repository.setName("Testovaci digitalni archiv");
        repository.setDigitalRepositoryType(DigitalRepositoryType.DA);
        repository.setSendNotification(false);
        repository.setMultipleLinks(multipleLinks);
        return digitalRepositoryRepository.save(repository);
    }

    private DaAip createAip(final ArrDigitalRepository repository) {
        return createAip(repository, "aip-link-test");
    }

    private DaAip createAip(final ArrDigitalRepository repository, final String code) {
        DaAip aip = new DaAip();
        aip.setCode(code);
        aip.setDigitalRepository(repository);
        return aipRepository.save(aip);
    }

    /** A second unit of description under the root, to attach the same AIP to twice. */
    private Integer secondNodeId(final FundInfo fund) {
        return tx().execute(t -> {
            ArrNode parent = nodeRepository.findById(fund.getRootNodeId()).orElseThrow();
            List<ArrLevel> levels = fundLevelService.addNewLevel(fund.getFundVersion(), parent, parent,
                    AddLevelDirection.CHILD, null, null, null, null, null);
            return levels.stream().max(Comparator.comparing(ArrLevel::getLevelId)).orElseThrow()
                    .getNode().getNodeId();
        });
    }

    @Test
    public void attachingTwiceToTheSameNodeCreatesOneLink() {
        FundInfo fund = tx().execute(t -> createFund("F-da-link-same-node"));
        Integer aipId = tx().execute(t -> createAip(createRepository(false)).getAipId());

        tx().executeWithoutResult(t -> daService.connectToJP(fund.getRootNodeId(), aipId));
        tx().executeWithoutResult(t -> daService.connectToJP(fund.getRootNodeId(), aipId));

        tx().executeWithoutResult(t -> assertEquals(1,
                daLinkRepository.findByAipIdAndDeleteChangeIsNull(aipId).size(),
                "attaching where it already hangs must not create a second link"));
    }

    @Test
    public void aSecondNodeIsRefusedWhenMultipleLinksIsOff() {
        FundInfo fund = tx().execute(t -> createFund("F-da-link-refused"));
        Integer aipId = tx().execute(t -> createAip(createRepository(false)).getAipId());
        Integer otherNodeId = secondNodeId(fund);

        tx().executeWithoutResult(t -> daService.connectToJP(fund.getRootNodeId(), aipId));

        BusinessException e = assertThrows(BusinessException.class,
                () -> tx().executeWithoutResult(t -> daService.connectToJP(otherNodeId, aipId)));
        assertEquals(ArrangementCode.DAO_ALREADY_LINKED, e.getErrorCode());

        tx().executeWithoutResult(t -> assertEquals(1,
                daLinkRepository.findByAipIdAndDeleteChangeIsNull(aipId).size(),
                "the refused attempt must leave the AIP on the one node it had"));
    }

    @Test
    public void aSecondNodeIsAllowedWhenMultipleLinksIsOn() {
        FundInfo fund = tx().execute(t -> createFund("F-da-link-allowed"));
        Integer aipId = tx().execute(t -> createAip(createRepository(true)).getAipId());
        Integer otherNodeId = secondNodeId(fund);

        tx().executeWithoutResult(t -> daService.connectToJP(fund.getRootNodeId(), aipId));
        tx().executeWithoutResult(t -> daService.connectToJP(otherNodeId, aipId));

        tx().executeWithoutResult(t -> assertEquals(2,
                daLinkRepository.findByAipIdAndDeleteChangeIsNull(aipId).size()));
    }

    /**
     * The bulk action checks every AIP before it opens: nothing is queued and no AIP is touched
     * when one of them cannot go where it is asked to.
     */
    @Test
    public void bulkAttachingIsRefusedForAnAipThatAlreadyHangsElsewhere() {
        FundInfo fund = tx().execute(t -> createFund("F-da-link-bulk"));
        Integer aipId = tx().execute(t -> createAip(createRepository(false)).getAipId());
        Integer otherNodeId = secondNodeId(fund);

        tx().executeWithoutResult(t -> daService.connectToJP(fund.getRootNodeId(), aipId));

        BusinessException e = assertThrows(BusinessException.class,
                () -> daService.submitBulkConnectToJP(otherNodeId, List.of(aipId)));
        assertEquals(ArrangementCode.DAO_ALREADY_LINKED, e.getErrorCode());

        tx().executeWithoutResult(t -> assertEquals(0, actionRepository.count(),
                "a refused request must not leave an action behind"));
    }

    /** Waits for the worker to carry the action out; it runs on a thread of its own. */
    private void awaitFinished(Integer actionId) {
        long deadline = System.currentTimeMillis() + 60_000;
        while (System.currentTimeMillis() < deadline) {
            DaAipActionState state = tx().execute(t ->
                    actionService.stateOf(actionRepository.findById(actionId).orElseThrow()));
            if (state == DaAipActionState.FINISHED || state == DaAipActionState.ERROR) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        throw new AssertionError("akce se nedokončila v časovém limitu");
    }

    /**
     * A bulk connect answers the request with an action and attaches the AIPs afterwards, one step
     * per AIP, on a thread of its own.
     */
    @Test
    public void bulkConnectQueuesOneStepPerAipAndAttachesThemOneByOne() {
        FundInfo fund = tx().execute(t -> createFund("F-da-connect-async"));
        Integer[] aipIds = tx().execute(t -> {
            ArrDigitalRepository repository = createRepository(false);
            return new Integer[] {
                    createAip(repository, "aip-async-1").getAipId(),
                    createAip(repository, "aip-async-2").getAipId() };
        });

        DaAipAction action = daService.submitBulkConnectToJP(fund.getRootNodeId(),
                                                             List.of(aipIds[0], aipIds[1]));

        List<Integer> itemIds = tx().execute(t -> actionItemRepository
                .findByAipActionOrderByAipActionItemId(actionRepository.findById(action.getAipActionId())
                        .orElseThrow())
                .stream().map(DaAipActionItem::getAipActionItemId).toList());
        assertEquals(2, itemIds.size(), "one step per AIP");

        awaitFinished(action.getAipActionId());

        tx().executeWithoutResult(t -> {
            for (Integer aipId : aipIds) {
                assertEquals(1, daLinkRepository.findByAipIdAndDeleteChangeIsNull(aipId).size(),
                             "every AIP of the action has to end up attached exactly once");
            }
            DaAipAction reloaded = actionRepository.findById(action.getAipActionId()).orElseThrow();
            assertEquals(DaAipActionState.FINISHED, actionService.stateOf(reloaded));
        });
    }

    /**
     * The check answers with the AIPs that cannot be attached, and answers the same way the
     * attaching itself would - it is the same rule, asked without doing anything.
     */
    @Test
    public void theCheckNamesTheAipsThatCannotBeAttached() {
        FundInfo fund = tx().execute(t -> createFund("F-da-connect-check"));
        Integer[] aipIds = tx().execute(t -> {
            ArrDigitalRepository repository = createRepository(false);
            return new Integer[] {
                    createAip(repository, "aip-free").getAipId(),
                    createAip(repository, "aip-taken").getAipId() };
        });
        Integer otherNodeId = secondNodeId(fund);

        tx().executeWithoutResult(t -> daService.connectToJP(otherNodeId, aipIds[1]));

        List<DaService.BlockedAip> blocked = tx().execute(t ->
                daService.checkConnect(fund.getRootNodeId(), List.of(aipIds[0], aipIds[1]), false));

        assertEquals(1, blocked.size(), "only the AIP that already hangs elsewhere is blocked");
        assertEquals(aipIds[1], blocked.get(0).aipId());
        assertEquals("aip-taken", blocked.get(0).aipCode());

        // and the submission refuses for the same reason, so the two cannot disagree
        assertThrows(BusinessException.class, () -> daService.submitBulkConnectToJP(
                fund.getRootNodeId(), List.of(aipIds[0], aipIds[1])));
    }

    /** Attaching where it already hangs is not blocked - that is a no-op, not a refusal. */
    @Test
    public void theCheckDoesNotBlockTheNodeTheAipAlreadyHangsOn() {
        FundInfo fund = tx().execute(t -> createFund("F-da-check-same-node"));
        Integer aipId = tx().execute(t -> createAip(createRepository(false), "aip-same").getAipId());

        tx().executeWithoutResult(t -> daService.connectToJP(fund.getRootNodeId(), aipId));

        assertEquals(0, tx().execute(t ->
                daService.checkConnect(fund.getRootNodeId(), List.of(aipId), false)).size());
        // but a unit of description that does not exist yet is a different one, so that is blocked
        assertEquals(1, tx().execute(t ->
                daService.checkConnect(fund.getRootNodeId(), List.of(aipId), true)).size());
    }
}
