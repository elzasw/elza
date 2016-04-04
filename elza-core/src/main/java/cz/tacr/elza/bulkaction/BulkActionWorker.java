package cz.tacr.elza.bulkaction;


import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.api.vo.BulkActionState.State;
import cz.tacr.elza.bulkaction.generator.BulkAction;


/**
 * Úloha hromadné akce.
 *
 * @author Martin Šlapa
 * @since 11.11.2015
 */
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
     * Stav hromadné akce
     */
    private BulkActionState bulkActionState;

    /**
     * Konstruktor úlohy.
     *
     * @param bulkAction       hromadná akce
     * @param bulkActionConfig nastavení hromadné akce
     * @param versionId        identifikátor verze archivní pomůcky
     * @param inputNodeIds     seznam vstupních uzlů (podstromů AS)
     */
    public BulkActionWorker(final BulkAction bulkAction,
                            final BulkActionConfig bulkActionConfig,
                            final Integer versionId,
                            final List<Integer> inputNodeIds) {
        bulkActionState = new BulkActionState();
        bulkActionState.setBulkActionCode(bulkActionConfig.getCode());
        this.bulkAction = bulkAction;
        this.bulkActionConfig = bulkActionConfig;
        this.versionId = versionId;
        this.inputNodeIds = inputNodeIds;
    }

    /**
     * Vrací stav hromadné akce.
     *
     * @return stav
     */
    public BulkActionState getBulkActionState() {
        return bulkActionState;
    }

    @Override
    public BulkActionWorker call() throws Exception {
        logger.info("Spuštěna hromadná akce: " + this);
        bulkActionState.setProcessId((int) Thread.currentThread().getId());
        bulkActionState.setState(State.RUNNING);
        try {
            bulkAction.run(versionId, inputNodeIds, bulkActionConfig, bulkActionState);

            //Thread.sleep(10000); // PRO TESTOVÁNÍ A DALŠÍ VÝVOJ

            bulkActionState.setState(State.FINISH);
            logger.info("Hromadná akce úspěšně dokončena: " + this);
        } catch (Exception e) {
            bulkActionState.setState(State.ERROR);
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
                ", bulkActionState=" + bulkActionState +
                '}';
    }

    /**
     * Ukončí běžící hromadnou akci.
     */
    public void terminate() {
        if (bulkActionState.getState() != State.RUNNING) {
            return;
        }

        bulkActionState.setInterrupt(true);

        while (bulkActionState.getState() == State.RUNNING) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Chyba při ukončování vlákna hromadné akce.", e);
            }
        }
        bulkActionState.setInterrupt(false);
    }

    public List<Integer> getInputNodeIds() {
        return inputNodeIds;
    }
}
