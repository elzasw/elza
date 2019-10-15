package cz.tacr.elza.drools;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.ResourcePathResolver;
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


/**
 * @author Martin Šlapa
 * @since 9.12.2015
 */
@Component
public class ScenarioOfNewLevelRules extends Rules {

    @Autowired
    private ScriptModelFactory scriptModelFactory;

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    @Autowired
    private RuleService ruleService;

    public synchronized List<ScenarioOfNewLevel> execute(final ArrLevel level,
                                                         final DirectionLevel directionLevel,
                                                         final ArrFundVersion version)
            throws IOException
    {

        NewLevelApproaches newLevelApproaches = new NewLevelApproaches();

        List<Level> levels = scriptModelFactory.createFactsForNewLevel(level, directionLevel, version);

        List<RulArrangementRule> rulArrangementRules = arrangementRuleRepository.findByRuleSetAndRuleTypeOrderByPriorityAsc(
                version.getRuleSet(), RulArrangementRule.RuleType.NEW_LEVEL);

        for (RulArrangementRule rulArrangementRule : rulArrangementRules) {
            Path path = resourcePathResolver.getDroolFile(rulArrangementRule);

            StatelessKieSession session = createNewStatelessKieSession(path);
            session.setGlobal("results", newLevelApproaches);
            session.execute(levels);
        }

        List<RulExtensionRule> rulExtensionRules = ruleService.findExtensionRuleByNode(level.getNode(), RulExtensionRule.RuleType.NEW_LEVEL);
        for (RulExtensionRule rulExtensionRule : rulExtensionRules) {
            Path path = resourcePathResolver.getDroolFile(rulExtensionRule);

            StatelessKieSession session = createNewStatelessKieSession(path);
            session.setGlobal("results", newLevelApproaches);
            session.execute(levels);
        }

        List<ScenarioOfNewLevel> scenarioOfNewLevelList = new LinkedList<>();
        for (NewLevelApproach newLevelApproach : newLevelApproaches.getNewLevelApproaches()) {
			scenarioOfNewLevelList
			        .add(scriptModelFactory.createScenarioOfNewLevel(newLevelApproach, version.getRuleSetId()));
        }

        return scenarioOfNewLevelList;
    }



}
