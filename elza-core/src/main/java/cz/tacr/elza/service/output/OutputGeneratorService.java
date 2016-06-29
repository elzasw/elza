package cz.tacr.elza.service.output;

import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.repository.OutputRepository;
import cz.tacr.elza.repository.OutputResultRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
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

import java.util.LinkedList;
import java.util.Queue;

/**
 * Zajišťuje generování výstupu a jeho uložení do dms na základě vstupní definice.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 24.6.16
 */

@Service
public class OutputGeneratorService implements ListenableFutureCallback<OutputGeneratorWorker> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Queue<OutputGeneratorWorker> outputQueue = new LinkedList<>(); // fronta outputů ke zpracování
    private OutputGeneratorWorker worker = null; // aktuálně zpracovávaný output

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private OutputRepository outputRepository;

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

    // TODO - JavaDoc - Lebeda
    public void generateOutput(ArrOutput arrOutput) {
        ArrOutputResult outputResult = outputResultRepository.findByOutputDefinition(arrOutput.getOutputDefinition());
        Assert.isNull(outputResult, "Tento výstup byl již vygenerován.");
        outputQueue.add(getWorker(arrOutput));
        runNextOutput(); // zkusí sputit frontu


        // TODO Lebeda - zapracovat po novu
        // eventNotificationService.publishEvent(EventFactory.createIdInVersionEvent(EventType.OUTPUT_GENERATED, arrangementService.getOpenVersionByFundId(arrOutput.getOutputDefinition().getFund().getFundId()), arrOutput.getOutputId()));
    }

    // TODO - JavaDoc - Lebeda
    private void runNextOutput() {
        if ((worker == null) && CollectionUtils.isNotEmpty(outputQueue)) {
            worker = outputQueue.poll();

            ListenableFuture future = taskExecutor.submitListenable(worker);
            future.addCallback(this);
        }
    }

    private OutputGeneratorWorker getWorker(ArrOutput output) {
        final OutputGeneratorWorker generatorWorker = workerFactory.getOutputGeneratorWorker();
//        final OutputGeneratorWorker generatorWorker = new OutputGeneratorWorker();
        generatorWorker.init(output.getOutputId());
        return generatorWorker;
    }

    @Override
    public void onFailure(Throwable ex) {
        worker = null;
        logger.error("Worker nedoběhl správně", ex);
        runNextOutput();
    }

    @Override
    public void onSuccess(OutputGeneratorWorker result) {
        final Integer arrOutputId = result.getArrOutputId();
        ArrOutput arrOutput = outputRepository.findOne(arrOutputId);

        // TODO Lebeda - dořešit
//        eventNotificationService.publishEvent(EventFactory.createIdInVersionEvent(EventType.OUTPUT_GENERATED,
//                arrangementService.getOpenVersionByFundId(arrOutput.getOutputDefinition().getFund().getFundId()),
//                arrOutputId));

        worker = null;
        logger.info("Worker doběhl správně");
        runNextOutput();
    }
}
