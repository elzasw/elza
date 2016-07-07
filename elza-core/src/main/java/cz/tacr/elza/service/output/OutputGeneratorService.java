package cz.tacr.elza.service.output;

import com.google.common.collect.Sets;
import cz.tacr.elza.api.ArrOutputDefinition.OutputState;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventType;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * Zajišťuje asynchronní generování výstupu a jeho uložení do dms na základě vstupní definice.
 * Zajišťuje spuštění jedné jediné úlohy najednou, zbytek udržuje ve frontě.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 24.6.16
 */

@Service
public class OutputGeneratorService implements ListenableFutureCallback<OutputGeneratorWorker> {

    public static final String OUTPUT_WEBSOCKET_ERROR_STATE = "ERROR";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Queue<OutputGeneratorWorker> outputQueue = new LinkedList<>(); // fronta outputů ke zpracování
    private OutputGeneratorWorker worker = null; // aktuálně zpracovávaný output

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
    @Qualifier("threadPoolTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Value("${elza.templates.templatesDir}")
    private String templatesDir;

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
     * @param userId ID uživatele pod kterým bude vytvořená změna arrChange související s generováním
     */
    public void generateOutput(ArrOutput arrOutput, Integer userId) {
        ArrOutputResult outputResult = outputResultRepository.findByOutputDefinition(arrOutput.getOutputDefinition());
        Assert.isNull(outputResult, "Tento výstup byl již vygenerován.");
        setStateAndSave(arrOutput.getOutputDefinition(), OutputState.GENERATING);
        outputQueue.add(getWorker(arrOutput, userId));
        runNextOutput(); // zkusí sputit frontu
    }


    /**
     * Podívá se do fronty úkolů a zda aktuálně probíhá generování,
     * pokud generování NEprobíhá a existuje ve frontě existujuje zařazená úloha, bude spuštěna.
     */
    private void runNextOutput() {
        if ((worker == null) && CollectionUtils.isNotEmpty(outputQueue)) {
            worker = outputQueue.poll();

            ListenableFuture future = taskExecutor.submitListenable(worker);
            future.addCallback(this);
        }
    }


    /**
     * Založí nový worker pro úlohu.
     *
     * @param output definice požadovaného výstupu
     * @param userId ID uživatele pod kterým bude vytvořená změna arrChange související s generováním
     * @return worker pro úlohu
     */
    private OutputGeneratorWorker getWorker(ArrOutput output, Integer userId) {
        final OutputGeneratorWorker generatorWorker = workerFactory.getOutputGeneratorWorker();
        generatorWorker.init(output.getOutputId(), userId);
        return generatorWorker;
    }

    private void publicOutputStateEvent(final ArrOutputDefinition arrOutputDefinition, final @Nullable String customState) {
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

    private void setStateAndSave(ArrOutputDefinition arrOutputDefinition, OutputState state) {
        arrOutputDefinition.setState(state);
        outputDefinitionRepository.save(arrOutputDefinition);
    }

    @Override
    public void onFailure(Throwable ex) {
        final Integer arrOutputId = worker.getArrOutputId();
        if (worker != null && worker.getArrOutputId() != null) {
            ArrOutput arrOutput = outputRepository.findOne(worker.getArrOutputId());
            ArrOutputDefinition arrOutputDefinition = outputDefinitionRepository.findByOutputId(arrOutput.getOutputId());
            arrOutputDefinition.setError(ex.getLocalizedMessage());
            setStateAndSave(arrOutputDefinition, OutputState.OPEN);
            publicOutputStateEvent(arrOutputDefinition, OUTPUT_WEBSOCKET_ERROR_STATE);
        }
        worker = null;
        logger.error("Generování výstupu pro arr_output id="+arrOutputId+" dokončeno s chybou.", ex);
        runNextOutput();
    }

    @Override
    public void onSuccess(OutputGeneratorWorker result) {
        final Integer arrOutputId = result.getArrOutputId();
        final ArrChange change = result.getChange();
        ArrOutput arrOutput = outputRepository.findOne(arrOutputId);
        ArrOutputDefinition arrOutputDefinition = outputDefinitionRepository.findByOutputId(arrOutput.getOutputId());
        List<ArrNodeOutput> nodesList = nodeOutputRepository.findByOutputDefinition(arrOutputDefinition);
        Map<ArrChange, Boolean> arrChangeBooleanMap = arrangementService.detectChangeNodes(nodesList.stream().map(ArrNodeOutput::getNode).collect(Collectors.toSet()), Sets.newHashSet(change), false, true);

        if (arrChangeBooleanMap.containsKey(change) && arrChangeBooleanMap.get(change)) {
            setStateAndSave(arrOutputDefinition, OutputState.OUTDATED);
        } else {
            setStateAndSave(arrOutputDefinition, OutputState.FINISHED);
        }
        publicOutputStateEvent(arrOutputDefinition, null);
        worker = null;
        logger.info("Generování výstupu pro arr_output id="+arrOutputId+" dokončeno úspěšně.", arrOutputId);
        runNextOutput();
    }
}
