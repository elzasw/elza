package cz.tacr.elza.service.da;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.api.DaAipActionItemState;
import cz.tacr.elza.api.DaAipActionType;
import cz.tacr.elza.repository.DaAipActionItemRepository;

/**
 * Carries out one AIP of an action, addressed by the item that records it.
 *
 * Sits between the worker and {@link DaService} so the worker holds nothing but an identifier: the
 * step runs on a pooled thread, in transactions of its own, long after the request that asked for
 * it was answered.
 */
@Service
public class DaAipStepService {

    private static final Logger logger = LoggerFactory.getLogger(DaAipStepService.class);

    @Autowired
    private DaService daService;

    @Autowired
    private DaAipActionService actionService;

    @Autowired
    private DaAipActionItemRepository actionItemRepository;

    /**
     * What the step needs, read in a transaction of its own so nothing is held over from the
     * request that asked for it.
     */
    private record StepInput(DaAipActionType actionType, Integer aipId) {
    }

    /**
     * A projection, not the item: the worker runs on a pooled thread with no transaction of its
     * own, where the associations of an item read earlier cannot be navigated.
     */
    private StepInput readInput(Integer actionItemId) {
        List<Object[]> rows = actionItemRepository.findActionTypeAndAip(actionItemId);
        if (rows.isEmpty()) {
            return null;
        }
        Object[] row = rows.get(0);
        return new StepInput((DaAipActionType) row[0], (Integer) row[1]);
    }

    /**
     * Runs the work of one AIP. The outcome is recorded by the work itself, through the sink of
     * the item; only a failure that escapes it is recorded by the caller.
     */
    public void runStep(Integer actionItemId) {
        StepInput input = readInput(actionItemId);
        if (input == null) {
            logger.debug("Položka akce ID={} už neexistuje, krok se přeskakuje", actionItemId);
            return;
        }
        AipOutcomeSink sink = actionService.sinkForItem(actionItemId, input.aipId());
        List<Integer> oneAip = List.of(input.aipId());
        switch (input.actionType()) {
            case DB_UPDATE -> daService.doCreateDaoStructure(oneAip, false, sink);
            case FORCE_UPDATE -> daService.doCreateDaoStructure(oneAip, true, sink);
            case REMAP_REFERENCES -> daService.remapReferences(oneAip, sink);
            default -> {
                // Only the work ELZA does on its own is carried out here; the rest is waiting for
                // the digital archive and is finished by the synchronization queue.
                logger.warn("Typ akce {} se asynchronně neprovádí", input.actionType());
                actionService.recordOutcome(actionItemId, DaAipActionItemState.ERROR,
                                            "Typ akce nelze provést na pozadí.");
            }
        }
    }

    /**
     * Records a failure that escaped the work itself, in a transaction of its own - the one the
     * work ran in is already rolled back.
     */
    public void recordUnexpectedFailure(Integer actionItemId, Throwable failure) {
        try {
            actionService.recordOutcome(actionItemId, DaAipActionItemState.ERROR,
                                        AipProblem.of(failure).description());
        } catch (Exception e) {
            logger.error("Výsledek položky akce ID={} se nepodařilo zapsat", actionItemId, e);
        }
    }
}
