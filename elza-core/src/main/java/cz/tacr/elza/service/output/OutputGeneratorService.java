package cz.tacr.elza.service.output;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.exception.ProcessException;
import cz.tacr.elza.service.OutputService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.google.common.collect.Sets;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.api.ArrOutputDefinition.OutputState;
import cz.tacr.elza.api.UsrPermission;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.NodeOutputRepository;
import cz.tacr.elza.repository.OutputDefinitionRepository;
import cz.tacr.elza.repository.OutputRepository;
import cz.tacr.elza.repository.OutputResultRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Zajišťuje asynchronní generování výstupu a jeho uložení do dms na základě vstupní definice.
 * Zajišťuje spuštění jedné jediné úlohy najednou, zbytek udržuje ve frontě.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 24.6.16
 */

@Service
public class OutputGeneratorService implements ListenableFutureCallback<OutputGeneratorWorkerAbstract> {

    public static final String OUTPUT_WEBSOCKET_ERROR_STATE = "ERROR";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Queue<OutputGeneratorWorkerAbstract> outputQueue = new LinkedList<>(); // fronta outputů ke zpracování

    /**
     * Synchronizační objekt pro zamykání - pro frontu (outputQueue).
     */
    private final Object lock = new Object();

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private OutputRepository outputRepository;

    @Autowired
    private OutputDefinitionRepository outputDefinitionRepository;

    @Autowired
    private NodeOutputRepository nodeOutputRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private OutputResultRepository outputResultRepository;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private OutputGeneratorWorkerFactory workerFactory;

    @Autowired
    private BulkActionService bulkActionService;

    @Autowired
    private OutputService outputService;

    @Autowired
    @Qualifier("threadPoolTaskExecutorOG")
    private ThreadPoolTaskExecutor taskExecutor;

    @Value("${elza.templates.templatesDir}")
    private String templatesDir;

    @Autowired
    @Qualifier("transactionManager")
    protected PlatformTransactionManager txManager;

    /**
     * Získání cesty ke složce šablon
     *
     * @return cesta ke složce šablon
     */
    public String getTemplatesDir() {
        return templatesDir;
    }

    /**
     * Spuštění generování výstupu a jeho uložení do DMS
     *
     * @param arrOutput definice outputu s definicí výstupu
     * @param userId    ID uživatele pod kterým bude vytvořená změna arrChange související s generováním
     * @param fund      AS výstupu
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public String generateOutput(final ArrOutput arrOutput,
                               final Integer userId,
                               @AuthParam(type = AuthParam.Type.FUND) final ArrFund fund,
                                 final boolean forced) {
        ArrOutputResult outputResult = outputResultRepository.findByOutputDefinition(arrOutput.getOutputDefinition());
        Assert.isNull(outputResult, "Tento výstup byl již vygenerován.");

        synchronized (lock) {
            if (outputQueue.stream().anyMatch(i -> arrOutput.getOutputId().equals(i.getArrOutputId()))) {
                throw new IllegalStateException("Tento výstup je již ve frontě generování");
            }
        }

        ArrOutputDefinition outputDefinition = arrOutput.getOutputDefinition();

        if (!forced) {

            // -- kontrola spuštění doporučených akcí
            Set<RulAction> recommendedActions = bulkActionService.getRecommendedActions(outputDefinition.getOutputType());
            ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(fund.getFundId());

            bulkActionService.checkOutdatedActions(fundVersion.getFundVersionId());

            Set<ArrNode> nodesForOutput = new HashSet<>(outputService.getNodesForOutput(arrOutput));
            List<ArrBulkActionRun> finishedAction = bulkActionService.findBulkActionsByNodes(fundVersion, nodesForOutput, ArrBulkActionRun.State.FINISHED);

            Map<RulAction, ArrChange> lastChangeAction = new HashMap<>();
            for (RulAction recommendedAction : recommendedActions) {
                boolean found = false;
                for (ArrBulkActionRun bulkActionRun : finishedAction) {
                    String bulkActionFilename = bulkActionRun.getBulkActionCode() + ".yaml";
                    if (bulkActionFilename.equalsIgnoreCase(recommendedAction.getFilename())) {
                        found = true;
                        ArrChange arrChange = lastChangeAction.get(recommendedAction);
                        if (arrChange == null) {
                            lastChangeAction.put(recommendedAction, bulkActionRun.getChange());
                        } else {
                            if (bulkActionRun.getChange().getChangeDate().isAfter(arrChange.getChangeDate())) {
                                lastChangeAction.put(recommendedAction, bulkActionRun.getChange());
                            }
                        }
                    }
                }
                if (!found) {
                    return "Nebyly spuštěny všechny doporučené akce";
                }
            }
            // -- !kontrola spuštění doporučených akcí

            Map<ArrChange, Boolean> changeBooleanMap = arrangementService.detectChangeNodes(nodesForOutput, new HashSet<>(lastChangeAction.values()), false, true);
            for (Map.Entry<ArrChange, Boolean> entry : changeBooleanMap.entrySet()) {
                if (BooleanUtils.isTrue(entry.getValue())) {
                    return "Byly detekovány změny v Pořádání";
                }
            }

        }

        if (outputDefinition.getTemplate() == null) {
            throw new IllegalStateException("Nelze spustit generování, protože výstup nemá vybranou šablonu");
        }

        setStateAndSave(outputDefinition, OutputState.GENERATING);
        publishOutputStateEvent(outputDefinition, null);
        OutputGeneratorWorkerAbstract worker = createWorker(arrOutput, userId);
        synchronized (lock) {
            outputQueue.add(worker);
        }
        runNextOutput(); // zkusí sputit frontu
        return null;
    }


    /**
     * Podívá se do fronty úkolů a pokusí se předat poolu vláken task. Pokud je poolem odmítnut, tak se vrací do fronty.
     */
    private synchronized void runNextOutput() {
        if (CollectionUtils.isNotEmpty(outputQueue)) {
            OutputGeneratorWorkerAbstract task = outputQueue.poll();
            try {
                ListenableFuture future = taskExecutor.submitListenable(task);
                //noinspection unchecked
                future.addCallback(this);
            } catch (RejectedExecutionException e) {
                // pokud je pool plný, vracím do fronty
                outputQueue.add(task);
            }
        }
    }


    /**
     * Založí nový worker pro úlohu.
     *
     * @param output definice požadovaného výstupu
     * @param userId ID uživatele pod kterým bude vytvořená změna arrChange související s generováním
     * @return worker pro úlohu
     */
    private OutputGeneratorWorkerAbstract createWorker(final ArrOutput output, final Integer userId) {
        final ArrOutputDefinition arrOutputDefinition = output.getOutputDefinition();
        final RulTemplate rulTemplate = arrOutputDefinition.getTemplate();

        final OutputGeneratorWorkerAbstract generatorWorker = workerFactory.getOutputGeneratorWorker(rulTemplate.getEngine());
        generatorWorker.init(output.getOutputId(), userId, rulTemplate);
        return generatorWorker;
    }

    public void publishOutputStateEvent(final ArrOutputDefinition arrOutputDefinition, final @Nullable String customState) {
        final ArrFund fund = fundRepository.findByOutputDefinitionId(arrOutputDefinition.getOutputDefinitionId());

        eventNotificationService.forcePublish(
                EventFactory.createStringAndIdInVersionEvent(
                        EventType.OUTPUT_STATE_CHANGE,
                        arrangementService.getOpenVersionByFundId(
                                fund.getFundId()
                        ).getFundVersionId(),
                        arrOutputDefinition.getOutputDefinitionId(),
                        customState == null ? arrOutputDefinition.getState().toString() : customState
                )
        );
    }

    private void setStateAndSave(final ArrOutputDefinition arrOutputDefinition, final OutputState state) {
        arrOutputDefinition.setState(state);
        outputDefinitionRepository.save(arrOutputDefinition);
    }

    @Override
    public void onFailure(final Throwable ex) {
        logger.error("Generování výstupu  dokončeno s chybou.", ex);
        try {
            if (ex instanceof ProcessException) {
                final ProcessException pe = (ProcessException) ex;
                Integer outputId = pe.getId();
                saveFailStatus(outputId, ex);
            } else {
                logger.error("Výjimka není typu ProcessException, takže nebylo možné rozeznat, ke kterému výstupu patří.");
            }
        } finally {
            runNextOutput();
        }
    }

    /**
     * Uložení chybového stavu k outputu.
     *
     * @param outputId identifikátor výstupu
     * @param ex zachycená výjimka
     */
    private void saveFailStatus(final Integer outputId, final Throwable ex) {
        if (outputId == null) {
            throw new IllegalStateException("Nepodařilo se uložit chybu k výstupu. ID není definováno.");
        }

        (new TransactionTemplate(txManager)).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                final ArrOutputDefinition outputDefinition = outputDefinitionRepository.findByOutputId(outputId);
                final StringBuilder stringBuffer = new StringBuilder();
                stringBuffer.append(ex.getLocalizedMessage()).append("\n");
                Throwable cause = ex.getCause();
                while (cause != null && stringBuffer.length() < 1000) {
                    stringBuffer.append(cause.getLocalizedMessage()).append("\n");
                    cause = cause.getCause();
                }
                outputDefinition.setError(stringBuffer.length() > 1000 ? stringBuffer.substring(0, 1000) : stringBuffer.toString());
                outputDefinition.setState(OutputState.OPEN);
                outputDefinitionRepository.save(outputDefinition);
                publishOutputStateEvent(outputDefinition, OutputGeneratorService.OUTPUT_WEBSOCKET_ERROR_STATE);
            }
        });
    }

    @Override
    public void onSuccess(final OutputGeneratorWorkerAbstract result) {
        runNextOutput();
    }
}
