package cz.tacr.elza.drools;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulArrangementRule;
import cz.tacr.elza.domain.RulExtensionRule;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.drools.model.Level;
import cz.tacr.elza.drools.model.NewLevelApproach;
import cz.tacr.elza.drools.model.NewLevelApproaches;
import cz.tacr.elza.drools.service.ScriptModelFactory;
import cz.tacr.elza.service.RuleService;
import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;


/**
 * @author Martin Šlapa
 * @since 9.12.2015
 */
@Component
public class ScenarioOfNewLevelRules extends Rules {

    @Autowired
    private ScriptModelFactory scriptModelFactory;

    @Autowired
    private RulesExecutor rulesExecutor;

    @Autowired
    private RuleService ruleService;

    public synchronized List<ScenarioOfNewLevel> execute(final ArrLevel level,
                                                         final DirectionLevel directionLevel,
                                                         final ArrFundVersion version)
            throws Exception {

        NewLevelApproaches newLevelApproaches = new NewLevelApproaches();

        List<Level> levels = scriptModelFactory.createFactsForNewLevel(level, directionLevel, version);

        Path path;
        List<RulArrangementRule> rulArrangementRules = arrangementRuleRepository.findByRuleSetAndRuleTypeOrderByPriorityAsc(
                version.getRuleSet(), RulArrangementRule.RuleType.NEW_LEVEL);

        for (RulArrangementRule rulArrangementRule : rulArrangementRules) {
            path = Paths.get(rulesExecutor.getDroolsDir(rulArrangementRule.getPackage().getCode(), rulArrangementRule.getRuleSet().getCode()) + File.separator + rulArrangementRule.getComponent().getFilename());

            StatelessKieSession session = createNewStatelessKieSession(path);
            session.setGlobal("results", newLevelApproaches);
            execute(session, levels);
        }

        List<RulExtensionRule> rulExtensionRules = ruleService.findExtensionRuleByNode(level.getNode(), RulExtensionRule.RuleType.NEW_LEVEL);
        for (RulExtensionRule rulExtensionRule : rulExtensionRules) {
            path = Paths.get(rulesExecutor.getDroolsDir(rulExtensionRule.getPackage().getCode(), rulExtensionRule.getArrangementExtension().getRuleSet().getCode()) + File.separator + rulExtensionRule.getComponent().getFilename());
            StatelessKieSession session = createNewStatelessKieSession(path);
            execute(session, levels);
        }

        List<ScenarioOfNewLevel> scenarioOfNewLevelList = new LinkedList<>();
        for (NewLevelApproach newLevelApproach : newLevelApproaches.getNewLevelApproaches()) {
            scenarioOfNewLevelList.add(scriptModelFactory.createScenarioOfNewLevel(newLevelApproach));
        }

        return scenarioOfNewLevelList;
    }



}
