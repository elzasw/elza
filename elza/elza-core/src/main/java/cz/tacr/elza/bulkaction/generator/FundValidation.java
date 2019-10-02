package cz.tacr.elza.bulkaction.generator;

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.asynchactions.UpdateConformityInfoService;
import cz.tacr.elza.bulkaction.ActionRunContext;
import cz.tacr.elza.bulkaction.BulkAction;
import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.domain.ArrBulkActionRun.State;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;

/**
 * Hromadná akce pro kontrolu validace (stavů popisu) celé archivní pomůcky.
 *
 */
public class FundValidation extends BulkAction {

    /**
     * Identifikátor hromadné akce
     */
    public static final String TYPE = "FUND_VALIDATION";

    @Autowired
    private UpdateConformityInfoService updateConformityInfoService;

    @Autowired
    private BulkActionService bulkActionService;

    /**
     * Generování hodnot - rekurzivní volání pro procházení celého stromu
     *
     * @param level uzel
     */
    private void generate(final ArrLevel level) {
        if (bulkActionRun.isInterrupted()) {
            bulkActionRun.setState(State.INTERRUPTED);
			throw new BusinessException("Hromadná akce " + getName() + " byla přerušena.",
			        ArrangementCode.BULK_ACTION_INTERRUPTED).set("code", bulkActionRun.getBulkActionCode());
        }

        List<ArrLevel> childLevels = getChildren(level);

        bulkActionService.setConformityInfoInNewTransaction(level.getLevelId(), version.getFundVersionId());

        for (ArrLevel childLevel : childLevels) {
            generate(childLevel);
        }
    }

    @Override
	public void run(ActionRunContext runContext) {

        // v případě, že existuje nějaké přepočítávání uzlů, je nutné to ukončit
        updateConformityInfoService.terminateWorkerInVersionAndWait(version.getFundVersionId());

		for (Integer nodeId : runContext.getInputNodeIds()) {
			ArrNode nodeRef = nodeRepository.getOne(nodeId);
            ArrLevel level = levelRepository.findByNodeAndDeleteChangeIsNull(nodeRef);
			Validate.notNull(level);

            generate(level);
        }
    }

    @Override
	public String getName() {
		return "FundValidationBulkAction";
    }
}
