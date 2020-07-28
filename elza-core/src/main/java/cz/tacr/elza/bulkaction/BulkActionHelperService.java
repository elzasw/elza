package cz.tacr.elza.bulkaction;

import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.Result;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.BulkActionNodeRepository;
import cz.tacr.elza.repository.BulkActionRunRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.*;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static cz.tacr.elza.repository.ExceptionThrow.bulkAction;
import static cz.tacr.elza.repository.ExceptionThrow.version;

/**
 * Pomocá třída pro zpracovávání požadavků při vykonávání hromadných akcí
 */
@Service
public class BulkActionHelperService {

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private BulkActionConfigManager bulkActionConfigManager;

    @Autowired
    private BulkActionRunRepository bulkActionRepository;

    @Autowired
    private IEventNotificationService eventNotificationService;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private OutputServiceInternal outputServiceInternal;

    @Autowired
    private UserService userService;

    @Autowired
    private BulkActionNodeRepository bulkActionNodeRepository;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    private static final Logger logger = LoggerFactory.getLogger(AsyncRequestService.class);

    public BulkActionHelperService() {
        logger.info("bulkActionHelper init");
    }

    /**
     * Method will update action
     *
     * @param bulkActionRun
     */
    @Transactional(Transactional.TxType.REQUIRED)
    public synchronized void updateAction(ArrBulkActionRun bulkActionRun) {
        storeBulkActionRun(bulkActionRun);
        eventPublishBulkAction(bulkActionRun);
    }

    /**
     * Store bulk action run.
     *
     * @param bulkActionRun the bulk action run
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public void storeBulkActionRun(final ArrBulkActionRun bulkActionRun) {
        if (bulkActionRun.getBulkActionRunId() == null) {
            BulkActionConfig bulkActionConfigOrig = bulkActionConfigManager.get(bulkActionRun.getBulkActionCode());

            if (bulkActionConfigOrig == null) {
                throw new IllegalArgumentException("Hromadná akce neexistuje!");
            }
            Integer fundVersionId = bulkActionRun.getFundVersion().getFundVersionId();
            ArrFundVersion version = fundVersionRepository.findById(fundVersionId)
                    .orElseThrow(version(fundVersionId));

            if (version.getLockChange() != null) {
                throw new IllegalArgumentException("Verze archivní pomůcky je uzamčená!");
            }
            bulkActionRun.setFundVersion(version);
        }
        bulkActionRepository.save(bulkActionRun);
    }

    /**
     * Event publish bulk action.
     *
     * @param bulkActionRun the bulk action run
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public void eventPublishBulkAction(final ArrBulkActionRun bulkActionRun) {
        eventNotificationService.publishEvent(
                EventFactory.createIdInVersionEvent(
                        EventType.BULK_ACTION_STATE_CHANGE,
                        bulkActionRun.getFundVersion(),
                        bulkActionRun.getBulkActionRunId(),
                        bulkActionRun.getBulkActionCode(),
                        bulkActionRun.getState()
                )
        );
    }

    /**
     * Doběhnutí hromadné akce.
     *
     * @param bulkActionRun objekt hromadné akce
     */
    // TODO: implements concurrent strategy like sub-tree exclusive execute of action or output
    @Transactional(Transactional.TxType.MANDATORY)
    public void onFinished(final ArrBulkActionRun bulkActionRun) {
        // find action nodes for output update
        Integer bulkActionRunId = bulkActionRun.getBulkActionRunId();
        ArrBulkActionRun bulkActionRunReload = bulkActionRepository.findById(bulkActionRunId)
                .orElseThrow(bulkAction(bulkActionRunId));
        List<ArrBulkActionNode> arrBulkActionNodes = bulkActionRunReload.getArrBulkActionNodes();
        List<Integer> nodeIds = arrBulkActionNodes.stream().map(ArrBulkActionNode::getNodeId).collect(Collectors.toList());

        // find all related output outputss
        List<ArrOutput> outputs = outputServiceInternal.findOutputsByNodes(bulkActionRunReload.getFundVersion(), nodeIds, ArrOutput.OutputState.OPEN, ArrOutput.OutputState.COMPUTING);

        // prepare ArrChange provider applied only if update occurs, change shared between connectors
        Supplier<ArrChange> changeSupplier = new Supplier<ArrChange>() {
            private ArrChange change;

            @Override
            public ArrChange get() {
                if (change == null) {
                    change = arrangementService.createChange(ArrChange.Type.UPDATE_OUTPUT);
                }
                return change;
            }
        };

        // update each output output
        logger.debug("Dispatching result to outputs");
        for (ArrOutput output : outputs) {
            OutputItemConnector connector = outputServiceInternal.createItemConnector(bulkActionRunReload.getFundVersion(), output);
            connector.setChangeSupplier(changeSupplier);

            // update output by each result
            Result actionResult = bulkActionRunReload.getResult();
            if (actionResult != null) {
                for (ActionResult result : actionResult.getResults()) {
                    result.createOutputItems(connector);
                }
            }

            // update to open state
            output.setState(ArrOutput.OutputState.OPEN); // saved by commit
            outputServiceInternal.publishOutputStateChanged(output, bulkActionRunReload.getFundVersionId());
        }
        logger.debug("Result dispatched to outputs");
    }

    public SecurityContext createSecurityContext(ArrBulkActionRun bulkActionRun) {

        SecurityContext ctx = SecurityContextHolder.createEmptyContext();

        // read user from db
        String username = null, encodePassword = null;

        UserDetail userDetail = userService.createUserDetail(bulkActionRun.getUserId());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, encodePassword,
                null);
        auth.setDetails(userDetail);
        ctx.setAuthentication(auth);

        return ctx;
    }

    public ArrBulkActionRun getArrBulkActionRun(Integer bulkActionRunId) {
        return bulkActionRepository.findById(bulkActionRunId)
                .orElseThrow(bulkAction(bulkActionRunId));
    }

    public BulkAction prepareToRun(ArrBulkActionRun bulkActionRun) {
        List<Integer> nodeIds;
        BulkAction bulkAction;
        // initialization of worker may fail
        try {
            nodeIds = getBulkActionNodeIds(bulkActionRun);
            // create bulk action object
            bulkAction = bulkActionConfigManager.getBulkAction(bulkActionRun.getBulkActionCode());

        } catch (Exception e) {
            logger.debug("Failed to run action, bulkActionRunId = " + bulkActionRun.getBulkActionRunId(), e);
            // on error -> action have to be marked as failed
            bulkActionRun.setState(ArrBulkActionRun.State.ERROR);
            bulkActionRun.setError(e.getLocalizedMessage());
            storeBulkActionRun(bulkActionRun);
            eventPublishBulkAction(bulkActionRun);
            return null;
        }

        // změna stavu výstupů na počítání
        outputServiceInternal.changeOutputsStateByNodes(bulkActionRun.getFundVersion(),
                nodeIds,
                ArrOutput.OutputState.COMPUTING,
                ArrOutput.OutputState.OPEN);

        // save and propagate action
        bulkActionRun.setState(ArrBulkActionRun.State.PLANNED);
        updateAction(bulkActionRun);
        logger.debug("Hromadná akce naplánována ke spuštění: " + bulkActionRun.getBulkActionRunId());

        eventPublishBulkAction(bulkActionRun);
        return bulkAction;
    }

    /**
     * Gets node ids.
     *
     * @param bulkActionRun the bulk action run
     * @return the node ids
     */
    public List<Integer> getBulkActionNodeIds(final ArrBulkActionRun bulkActionRun) {
        List<Integer> nodeIds = bulkActionNodeRepository.findNodeIdsByBulkActionRun(bulkActionRun);

        return levelTreeCacheService.sortNodesByTreePosition(new HashSet<>(nodeIds), bulkActionRun.getFundVersion());
    }

    public void changeOutputStateByNodes(ArrBulkActionRun bulkActionRun) {
        //změna stavů na open
        outputServiceInternal.changeOutputsStateByNodes(bulkActionRun.getFundVersion(),
                bulkActionRun.getArrBulkActionNodes().stream()
                        .map(ArrBulkActionNode::getNodeId)
                        .collect(Collectors.toList()),
                ArrOutput.OutputState.OPEN,
                ArrOutput.OutputState.COMPUTING);
    }




}
