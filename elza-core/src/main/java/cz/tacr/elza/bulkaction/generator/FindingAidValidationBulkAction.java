package cz.tacr.elza.bulkaction.generator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import cz.tacr.elza.api.ArrNodeConformity;
import cz.tacr.elza.api.ArrNodeConformityExt;
import cz.tacr.elza.api.vo.BulkActionState.State;
import cz.tacr.elza.asynchactions.UpdateConformityInfoService;
import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionInterruptedException;
import cz.tacr.elza.bulkaction.BulkActionState;
import cz.tacr.elza.controller.RuleManager;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrVersionConformity;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.service.RuleService;


/**
 * Hromadná akce pro kontrolu validace (stavů popisu) celé archivní pomůcky.
 *
 * @author Martin Šlapa
 * @since 30.11.2015
 */
@Component
@Scope("prototype")
public class FindingAidValidationBulkAction extends BulkAction {

    /**
     * Identifikátor hromadné akce
     */
    public static final String TYPE = "FINDING_AID_VALIDATION";

    /**
     * Verze archivní pomůcky
     */
    private ArrFindingAidVersion version;

    /**
     * Změna
     */
    private ArrChange change;

    /**
     * Stav hromadné akce
     */
    private BulkActionState bulkActionState;

    /**
     * Strategie vyhodnocování
     */
    private Set<String> strategies;

    /**
     * Počet chybných uzlů
     */
    private Integer errorCount;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private UpdateConformityInfoService updateConformityInfoService;

    /**
     * Inicializace hromadné akce.
     *
     * @param bulkActionConfig nastavení hromadné akce
     */
    private void init(final BulkActionConfig bulkActionConfig) {

        Assert.notNull(bulkActionConfig);

        String evaluationTypeString = (String) bulkActionConfig.getProperty("evaluation_strategies");

        if (evaluationTypeString == null) {
            strategies = new HashSet<>();
        } else {
            strategies = new HashSet<>(Arrays.asList(evaluationTypeString.split("\\|")));
        }

        errorCount = 0;
    }

    /**
     * Generování hodnot - rekurzivní volání pro procházení celého stromu
     *
     * @param level uzel
     */
    private ArrVersionConformity.State generate(final ArrLevel level) {
        if (bulkActionState.isInterrupt()) {
            bulkActionState.setState(State.ERROR);
            throw new BulkActionInterruptedException("Hromadná akce " + toString() + " byla přerušena.");
        }

        List<ArrLevel> childLevels = getChildren(level);

        ArrVersionConformity.State state = ArrVersionConformity.State.OK;

        ArrNodeConformityExt nodeConformityInfoExt = ruleService
                .setConformityInfo(level.getLevelId(), version.getFindingAidVersionId(), strategies);

        ArrNodeConformity.State stateLevel = nodeConformityInfoExt.getState();

        if (stateLevel.equals(ArrNodeConformity.State.ERR)) {
            errorCount++;
            state = ArrVersionConformity.State.ERR;
        }

        for (ArrLevel childLevel : childLevels) {
            ArrVersionConformity.State stateChild = generate(childLevel);
            if (!stateChild.equals(ArrVersionConformity.State.OK)) {
                state = ArrVersionConformity.State.ERR;
            }
        }

        return state;

    }

    @Override
    @Transactional
    public void run(final Integer faVersionId,
                    final BulkActionConfig bulkAction,
                    final BulkActionState bulkActionState) {
        this.bulkActionState = bulkActionState;
        init(bulkAction);

        ArrFindingAidVersion version = findingAidVersionRepository.findOne(faVersionId);

        Assert.notNull(version);
        checkVersion(version);
        this.version = version;

        this.change = createChange();
        this.bulkActionState.setRunChange(this.change);

        // v případě, že existuje nějaké přepočítávání uzlů, je nutné to ukončit
        updateConformityInfoService.terminateWorkerInVersion(version);

        ArrVersionConformity.State state = generate(version.getRootLevel());

        String stateDescription;
        if (state.equals(ArrVersionConformity.State.ERR)) {
            stateDescription = "Validace uzlů archivní pomůcky zjistila nejméně jednu chybu: " + errorCount;
        } else {
            stateDescription = null;
        }

        ruleService.setVersionConformityInfo(state, stateDescription, version);
    }

    @Override
    public String toString() {
        return "FindingAidValidationBulkAction{" +
                "version=" + version +
                ", change=" + change +
                '}';
    }
}