package cz.tacr.elza.bulkaction.generator;

import cz.tacr.elza.api.ArrBulkActionRun.State;
import cz.tacr.elza.asynchactions.UpdateConformityInfoService;
import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionInterruptedException;
import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Hromadná akce pro kontrolu validace (stavů popisu) celé archivní pomůcky.
 *
 * @author Martin Šlapa
 * @since 30.11.2015
 */
@Component
@Scope("prototype")
public class FundValidationBulkAction extends BulkAction {

    /**
     * Identifikátor hromadné akce
     */
    public static final String TYPE = "FUND_VALIDATION";

    /**
     * Verze archivní pomůcky
     */
    private ArrFundVersion version;

    /**
     * Změna
     */
    private ArrChange change;

    /**
     * Stav hromadné akce
     */
    private ArrBulkActionRun bulkActionRun;

    @Autowired
    private UpdateConformityInfoService updateConformityInfoService;

    @Autowired
    private BulkActionService bulkActionService;

    private static final Logger logger = LoggerFactory.getLogger(FundValidationBulkAction.class);

    /**
     * Inicializace hromadné akce.
     *
     * @param bulkActionConfig nastavení hromadné akce
     */
    private void init(final BulkActionConfig bulkActionConfig) {
        Assert.notNull(bulkActionConfig);
    }

    /**
     * Generování hodnot - rekurzivní volání pro procházení celého stromu
     *
     * @param level uzel
     */
    private void generate(final ArrLevel level) {
        if (bulkActionRun.isInterrupted()) {
            bulkActionRun.setState(State.INTERRUPTED);
            throw new BulkActionInterruptedException("Hromadná akce " + toString() + " byla přerušena.");
        }

        List<ArrLevel> childLevels = getChildren(level);

        bulkActionService.setConformityInfoInNewTransaction(level.getLevelId(), version.getFundVersionId());

        for (ArrLevel childLevel : childLevels) {
            generate(childLevel);
        }
    }

    @Override
    @Transactional
    public void run(final List<Integer> inputNodeIds,
                    final BulkActionConfig bulkAction,
                    final ArrBulkActionRun bulkActionRun) {
        this.bulkActionRun = bulkActionRun;
        init(bulkAction);

        ArrFundVersion version = bulkActionRun.getFundVersion();

        Assert.notNull(version);
        checkVersion(version);
        this.version = version;

        // v případě, že existuje nějaké přepočítávání uzlů, je nutné to ukončit
        updateConformityInfoService.terminateWorkerInVersion(version);

        ArrNode rootNode = version.getRootNode();
        for (Integer nodeId : inputNodeIds) {
            ArrNode node = nodeRepository.findOne(nodeId);
            Assert.notNull("Node s nodeId=" + nodeId + " neexistuje");
            ArrLevel level = levelRepository.findNodeInRootTreeByNodeId(node, rootNode, null);
            Assert.notNull("Level neexistuje, nodeId=" + node.getNodeId() + ", rootNodeId=" + rootNode.getNodeId());

            generate(level);
        }
    }

    @Override
    public String toString() {
        return "FundValidationBulkAction{" +
                "version=" + version +
                ", change=" + change +
                '}';
    }
}