package cz.tacr.elza.bulkaction.generator;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import cz.tacr.elza.api.ArrNodeConformityInfo;
import cz.tacr.elza.api.ArrNodeConformityInfoExt;
import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionState;
import cz.tacr.elza.controller.RuleManager;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;


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

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private RuleManager ruleManager;

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
    private ArrFindingAidVersion.State generate(final ArrLevel level) {

        List<ArrLevel> childLevels = getChildren(level);

        ArrFindingAidVersion.State state = ArrFindingAidVersion.State.OK;

        ArrNodeConformityInfoExt nodeConformityInfoExt = ruleManager
                .setConformityInfo(level.getLevelId(), version.getFindingAidVersionId());

        ArrNodeConformityInfo.State stateLevel = nodeConformityInfoExt.getState();

        if (stateLevel.equals(ArrNodeConformityInfo.State.ERR)) {
            state = ArrFindingAidVersion.State.ERR;
        }

        for (ArrLevel childLevel : childLevels) {
            ArrFindingAidVersion.State stateChild = generate(childLevel);
            if (!stateChild.equals(ArrFindingAidVersion.State.OK)) {
                state = ArrFindingAidVersion.State.ERR;
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

        ArrFindingAidVersion.State state = generate(version.getRootLevel());
        //entityManager.refresh(version);
        version.setState(state);
        findingAidVersionRepository.save(version);
    }

    @Override
    public String toString() {
        return "FindingAidValidationBulkAction{" +
                "version=" + version +
                ", change=" + change +
                '}';
    }
}