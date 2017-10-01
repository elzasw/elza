package cz.tacr.elza.bulkaction;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrBulkActionRun.State;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.BulkActionRunRepository;

/**
 * Úloha hromadné akce.
 *
 */
public class BulkActionWorker implements Callable<BulkActionWorker> {

	private static final Logger logger = LoggerFactory.getLogger(BulkActionWorker.class);

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
     * Hromadná akce reprezentovaná v DB
     */
    private ArrBulkActionRun bulkActionRun;

    private BulkActionService bulkActionService;

    private BulkActionRunRepository bulkActionRunRepository;

    /**
     * Identfikátor uživatele, který spustil hromadnou akci (null, pokud to bylo systémové - od admina)
     */
    //private Integer userId;

    private Integer processId;

	public BulkActionWorker(BulkActionService bulkActionService, BulkActionRunRepository bulkActionRunRepository) {
		this.bulkActionService = bulkActionService;
		this.bulkActionRunRepository = bulkActionRunRepository;
	}

    public void init(final int bulkActionRunId) {
        bulkActionRun = bulkActionRunRepository.findOne(bulkActionRunId);
        if (bulkActionRun == null) {
            throw new SystemException("Proces hromadné akce" + bulkActionRunId + " nebyl nalezen", BaseCode.ID_NOT_EXIST);
        }
        versionId = bulkActionRun.getFundVersionId();
        inputNodeIds = bulkActionService.getBulkActionNodeIds(bulkActionRun);
		// create bulk action object
		bulkAction = bulkActionService.getBulkAction(bulkActionRun.getBulkActionCode());
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

        if (state.equals(State.FINISHED)) {
            bulkActionService.finished(bulkActionRun);
        }
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
            //bulkActionRun.setChange(createChange(userId));
            bulkActionService.storeBulkActionRun(bulkActionRun);
			// prepare context object
			ActionRunContext runContext = new ActionRunContext(inputNodeIds, bulkActionRun);

			bulkAction.run(runContext);

            //Thread.sleep(30000); // PRO TESTOVÁNÍ A DALŠÍ VÝVOJ

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

    @Override
    public String toString() {
        return "BulkActionWorker{" +
                "bulkAction=" + bulkAction +
                ", versionId=" + versionId +
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
                throw new SystemException("Chyba při ukončování vlákna hromadné akce.", e);
            }
        }

        setStateAndPublish(State.INTERRUPTED);

        bulkActionRun.setInterrupted(false);
    }
}
