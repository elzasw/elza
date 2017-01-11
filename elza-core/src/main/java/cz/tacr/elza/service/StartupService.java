package cz.tacr.elza.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.api.ArrOutputDefinition.OutputState;
import cz.tacr.elza.asynchactions.UpdateConformityInfoService;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.repository.BulkActionRunRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.OutputDefinitionRepository;

/**
 * Serviska pro úlohy, které je nutné spustit těsně po spuštění.
 *
 * @author Martin Šlapa
 * @since 23.03.2016
 */
@Service
public class StartupService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    @Qualifier("transactionManager")
    protected PlatformTransactionManager txManager;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private UpdateConformityInfoService updateConformityInfoService;

    @Autowired
    private BulkActionRunRepository bulkActionRunRepository;

    @Autowired
    private OutputDefinitionRepository outputDefinitionRepository;

    @Autowired
    private RequestQueueService requestQueueService;

    @PostConstruct
    private void init() {
        clearBulkActions();
        revalidateNodes();
        clearOutputGeneration();
        runQueuedRequests();
    }

    /**
     * Provede spuštění neodeslaných požadavků ve frontě na externí systémy.
     */
    private void runQueuedRequests() {
        requestQueueService.restartQueuedRequests();
    }

    /**
     * Vyčistí hromadné akce
     * - ty které jsou po startu ve stavu generování do chyba
     */
    private void clearOutputGeneration() {
        TransactionTemplate tmpl = new TransactionTemplate(txManager);
        // načítání dat v samostatné transakci
        tmpl.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                int count = outputDefinitionRepository.setStateFromStateWithError(Arrays.asList(OutputState.GENERATING, OutputState.COMPUTING), OutputState.OPEN, "Server se restartoval v průběhu zpracování");
                if (count > 0) {
                    logger.warn("Bylo změněn stav " + count + " outputů na stav Otevřený z důvodu restartování serveru při jejich zpracování.");
                }
            }
        });

    }

    /**
     * Provede přidání do front uzly, které nemají záznam v arr_node_conformity.
     * Obvykle to jsou uzly, které se validovaly během ukončení aplikačního serveru.
     * <p>
     * Metoda je pouštěna po startu aplikačního serveru.
     */
    public void revalidateNodes() {
        TransactionTemplate tmpl = new TransactionTemplate(txManager);
        Map<Integer, ArrFundVersion> fundVersionMap = new HashMap<>();
        Map<Integer, List<ArrNode>> fundNodesMap = new HashMap<>();

        // načítání dat v samostatné transakci
        tmpl.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {

                // zjištění všech uzlů, které nemají validaci
                List<ArrNode> nodes = nodeRepository.findByNodeConformityIsNull();

                // roztřídění podle AF
                for (ArrNode node : nodes) {
                    Integer fundId = node.getFund().getFundId();
                    List<ArrNode> addedNodes = fundNodesMap.get(fundId);
                    if (addedNodes == null) {
                        addedNodes = new LinkedList<>();
                        fundNodesMap.put(fundId, addedNodes);
                    }
                    addedNodes.add(node);
                }

                // načtení otevřených verzí AF
                List<ArrFundVersion> openVersions = fundVersionRepository.findAllOpenVersion();

                // vytvoření převodní mapy "id AF->verze AF"
                for (ArrFundVersion openVersion : openVersions) {
                    fundVersionMap.put(openVersion.getFund().getFundId(), openVersion);
                }
            }
        });

        // projde všechny fondy
        for (Map.Entry<Integer, List<ArrNode>> entry : fundNodesMap.entrySet()) {
            Integer fundId = entry.getKey();
            ArrFundVersion version = fundVersionMap.get(fundId);

            if (version == null) {
                logger.error("Pro AF s ID=" + fundId + " byly nalezeny nezvalidované uzly (" + entry.getValue() +
                        "), které nejsou z otevřené verze AF");
                continue;
            }

            // přidávání nodů je nutné dělat ve vlastní transakci (podle updateInfoForNodesAfterCommit)
            tmpl = new TransactionTemplate(txManager);
            tmpl.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(final TransactionStatus status) {
                    logger.info("Přidání " + entry.getValue().size() + " uzlů do fronty pro zvalidování");
                    updateConformityInfoService.updateInfoForNodesAfterCommit(entry.getValue(), version);
                }
            });
        }
    }

    private void clearBulkActions() {
        TransactionTemplate tmpl = new TransactionTemplate(txManager);
        tmpl.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                bulkActionRunRepository.updateFromStateToState(ArrBulkActionRun.State.RUNNING, ArrBulkActionRun.State.ERROR);
            }
        });

    }
}
