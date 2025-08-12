package cz.tacr.elza.bulkaction.generator;

import cz.tacr.elza.bulkaction.ActionRunContext;
import cz.tacr.elza.bulkaction.BulkActionInterruptedException;
import cz.tacr.elza.bulkaction.BulkActionTransactional;
import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.service.AsyncRequestService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;

/**
 * Hromadná akce pro kontrolu validace (stavů popisu) celé archivní pomůcky.
 *
 */
public class FundValidation extends BulkActionTransactional {

    /**
     * Identifikátor hromadné akce
     */
    public static final String TYPE = "FUND_VALIDATION";

    @Autowired
    private AsyncRequestService asyncRequestService;

    @Autowired
    private BulkActionService bulkActionService;

    /**
     * Generování hodnot - rekurzivní volání pro procházení celého stromu
     *
     * @param level uzel
     * @throws InterruptedException 
     */
    private void generate(final ArrLevel level) {
        if (interrupt) {
			throw new BulkActionInterruptedException("The action was interrupted");
        }

        List<ArrLevel> childLevels = getChildren(level);

        bulkActionService.setConformityInfoInNewTransaction(level.getLevelId(), getFondsVersionId());

        for (ArrLevel childLevel : childLevels) {
            generate(childLevel);
        }
    }

    @Override
	public void run(ActionRunContext runContext) {

        // v případě, že existuje nějaké přepočítávání uzlů, je nutné to ukončit
        asyncRequestService.terminateNodeWorkersByFund(getFondsVersionId());

		for (Integer nodeId : runContext.getInputNodeIds()) {
            ArrLevel level = levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
            Objects.requireNonNull(level);

            generate(level);
        }
    }

    @Override
	public String getName() {
		return "FundValidationBulkAction";
    }
}
