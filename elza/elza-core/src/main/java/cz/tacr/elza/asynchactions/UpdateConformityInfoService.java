package cz.tacr.elza.asynchactions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformity;

/**
 * Servisní třída pro spouštění vláken na aktualizaci {@link ArrNodeConformity}.
 */
@Service
@Configuration
public class UpdateConformityInfoService {

    private final Map<Integer, UpdateConformityInfoWorker> fundVersionIdWorkerMap = new HashMap<>();

    private final Map<Transaction, ConformitySyncAdapter> txSyncAdapterMap = new HashMap<>();

    private Executor taskExecutor;

    private EntityManager em;

    @Autowired
    public UpdateConformityInfoService(@Qualifier(value = "conformityUpdateTaskExecutor") Executor taskExecutor, EntityManager em) {
        this.taskExecutor = taskExecutor;
        this.em = em;
    }

    /**
     * Zapamatuje se uzly k přepočítání stavu a po dokončení transakce spustí jejich přepočet.
     *
     * @param nodes seznam nodů, které budou přepočítány.
     * @param version verze, ve které probíhá výpočet
     */
    public void updateInfoForNodesAfterCommit(Integer fundVersionId, Collection<ArrNode> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        Transaction tx = HibernateUtils.getCurrentTransaction(em);
        ConformitySyncAdapter adapter = txSyncAdapterMap.computeIfAbsent(tx, k -> new ConformitySyncAdapter());

        nodes.forEach(node -> adapter.addNode(fundVersionId, node));

        TransactionSynchronizationManager.registerSynchronization(adapter);
    }

    @Bean
    @Scope("prototype")
    public UpdateConformityInfoWorker createConformityInfoWorker(Integer fundVersionId) {
        return new UpdateConformityInfoWorker(fundVersionId);
    }

    private synchronized void executeConformityUpdate(Integer fundVersionId, Collection<ArrNode> nodes) {
        UpdateConformityInfoWorker worker = fundVersionIdWorkerMap.get(fundVersionId);
        if (worker != null && worker.addNodes(nodes)) {
            // nodes were added to existing worker
            return;
        }
        // create new runnable worker
        worker = createConformityInfoWorker(fundVersionId);
        worker.addNodes(nodes);

        fundVersionIdWorkerMap.put(fundVersionId, worker);

        taskExecutor.execute(worker);
    }

    /**
     * Provede ukončení běhu vlákna.
     */
    public synchronized void terminateWorkerInVersionAndWait(Integer fundVersionId) {
        UpdateConformityInfoWorker worker = fundVersionIdWorkerMap.remove(fundVersionId);
        if (worker != null && worker.isRunning()) {
            worker.terminateAndWait();
        }
    }

    /**
     * Zjistí, zda-li nad verzí AS neběží nějaká validace.
     *
     * @param version verze AS
     * @return běží nad verzí validace?
     */
    public synchronized boolean isRunning(Integer fundVersionId) {
        Validate.notNull(fundVersionId);

        UpdateConformityInfoWorker worker = fundVersionIdWorkerMap.get(fundVersionId);

        return worker != null ? worker.isRunning() : false;
    }

    private class ConformitySyncAdapter extends TransactionSynchronizationAdapter {

        private final Map<Integer, List<ArrNode>> fundVersionIdNodeMap = new HashMap<>();

        public void addNode(Integer fundVersionId, ArrNode node) {
            Validate.notNull(fundVersionId);
            Validate.notNull(node);

            List<ArrNode> nodes = fundVersionIdNodeMap.computeIfAbsent(fundVersionId, k -> new ArrayList<>());
            nodes.add(node);
        }

        @Override
        public void afterCommit() {
            fundVersionIdNodeMap.forEach(UpdateConformityInfoService.this::executeConformityUpdate);
        }
    }
}
