package cz.tacr.elza.drools;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulRule;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.drools.model.Level;
import cz.tacr.elza.drools.model.NewLevelApproach;
import cz.tacr.elza.drools.model.NewLevelApproaches;
import cz.tacr.elza.drools.service.ScriptModelFactory;


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

    public synchronized List<ScenarioOfNewLevel> execute(final ArrLevel level,
                                                         final DirectionLevel directionLevel,
                                                         final ArrFundVersion version)
            throws Exception {

        NewLevelApproaches newLevelApproaches = new NewLevelApproaches();

        List<Level> levels = scriptModelFactory.createFactsForNewLevel(level, directionLevel, version);

        Path path;
        List<RulRule> rulPackageRules = packageRulesRepository.findByRuleSetAndRuleTypeOrderByPriorityAsc(
                version.getRuleSet(), RulRule.RuleType.NEW_LEVEL);

        for (RulRule rulPackageRule : rulPackageRules) {
            path = Paths.get(rulesExecutor.getRootPath() + File.separator + rulPackageRule.getFilename());

            StatelessKieSession session = createNewStatelessKieSession(path);
            session.setGlobal("results", newLevelApproaches);
            execute(session, levels);
        }

        List<ScenarioOfNewLevel> scenarioOfNewLevelList = new LinkedList<>();
        for (NewLevelApproach newLevelApproach : newLevelApproaches.getNewLevelApproaches()) {
            scenarioOfNewLevelList.add(scriptModelFactory.createScenarioOfNewLevel(newLevelApproach));
        }

        return scenarioOfNewLevelList;
    }



}
