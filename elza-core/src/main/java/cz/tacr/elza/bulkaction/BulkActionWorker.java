package cz.tacr.elza.bulkaction;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import cz.tacr.elza.api.ArrBulkActionRun.State;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.repository.BulkActionRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.bulkaction.generator.BulkAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Úloha hromadné akce.
 *
 * @author Martin Šlapa
 * @since 11.11.2015
 */
@Component
@Scope("prototype")
public class BulkActionWorker implements Callable<BulkActionWorker> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Hromadná akce
     */
    private BulkAction bulkAction;

    /**
     * Identifikátor verze archivní pomůcky
     */
    private Integer versionId;

    /**
     * Seznam vstupních uzlů (podstromů AS)
     */
    private List<Integer> inputNodeIds;

    /**
     * Nastavení hromadné akce
     */
    private BulkActionConfig bulkActionConfig;

    /**
     * Hromadná akce reprezentovaná v DB
     */
    private ArrBulkActionRun bulkActionRun;

    @Autowired
    private BulkActionService bulkActionService;

    @Autowired
    private BulkActionRunRepository bulkActionRunRepository;

    /**
     * Identfikátor uživatele, který spustil hromadnou akci (null, pokud to bylo systémové - od admina)
     */
    private Integer userId;

    private Integer processId;

    @Transactional(readOnly = true)
    public void init(final int bulkActionRunId) {
        bulkActionRun = bulkActionRunRepository.findOne(bulkActionRunId);
        if (bulkActionRun == null) {
            throw new IllegalArgumentException("Proces hromadné akce" + bulkActionRunId + " nebyl nalezen");
        }
        versionId = bulkActionRun.getFundVersionId();
        inputNodeIds = bulkActionService.getBulkActionNodeIds(bulkActionRun);
        bulkActionConfig = bulkActionService.getBulkActionConfig(bulkActionRun.getBulkActionCode());
        bulkAction = bulkActionService.getBulkAction((String) bulkActionConfig.getString("code_type_bulk_action"));
    }


    /**
     * Vrací stav hromadné akce.
     *
     * @return stav
     */
    public ArrBulkActionRun getBulkActionRun() {
        return bulkActionRun;
    }

    public void setStateAndPublish(State state) {
        bulkActionRun.setState(state);
        bulkActionService.storeBulkActionRun(bulkActionRun);
        bulkActionService.eventPublishBulkAction(bulkActionRun);
    }


    /**
     * Vytvoření nové změny.
     *
     * @return vytvořená změna
     */
    protected ArrChange createChange(final Integer userId) {
        return bulkActionService.createChange(userId);
    }

    /**
     * Vrací stav hromadné akce.
     *
     * @return stav
     */
    public State getState() {
        return bulkActionRun.getState();
    }

    @Override
    public BulkActionWorker call() throws Exception {
        logger.info("Spuštěna hromadná akce: " + this);
        bulkActionRun.setDateStarted(new Date());
        setStateAndPublish(State.RUNNING);
        processId = ((int) Thread.currentThread().getId());
        try {
            bulkActionRun.setChange(createChange(userId));
            bulkActionService.storeBulkActionRun(bulkActionRun);
            bulkAction.run(inputNodeIds, bulkActionConfig, bulkActionRun);

            //Thread.sleep(10000); // PRO TESTOVÁNÍ A DALŠÍ VÝVOJ

            bulkActionRun.setDateFinished(new Date());
            setStateAndPublish(State.FINISHED);
            logger.info("Hromadná akce úspěšně dokončena: " + this);
        } catch (Exception e) {
            bulkActionRun.setError(e.getLocalizedMessage());
            setStateAndPublish(State.ERROR);

            logger.error("Hromadná akce skončila chybou: " + this, e);
        }
        return this;
    }

    /**
     * Vrací hromadnou akci.
     *
     * @return hromadná akce
     */
    public BulkAction getBulkAction() {
        return bulkAction;
    }

    /**
     * Vrací identifikátor verze archivní pomůcky.
     *
     * @return identifikátor verze archivní pomůcky
     */
    public Integer getVersionId() {
        return versionId;
    }

    /**
     * Vrací nastavení hromadné akce.
     *
     * @return nastavení hromadné akce
     */
    public BulkActionConfig getBulkActionConfig() {
        return bulkActionConfig;
    }

    @Override
    public String toString() {
        return "BulkActionWorker{" +
                "bulkAction=" + bulkAction +
                ", versionId=" + versionId +
                ", bulkActionConfig=" + bulkActionConfig +
                ", bulkActionRun=" + bulkActionRun +
                '}';
    }

    /**
     * Ukončí běžící hromadnou akci.
     */
    public void terminate() {
        if (getState() != State.RUNNING) {
            return;
        }

        bulkActionRun.setInterrupted(true);

        while (getState() == State.RUNNING) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Chyba při ukončování vlákna hromadné akce.", e);
            }
        }

        setStateAndPublish(State.INTERRUPTED);

        bulkActionRun.setInterrupted(false);
    }
}
