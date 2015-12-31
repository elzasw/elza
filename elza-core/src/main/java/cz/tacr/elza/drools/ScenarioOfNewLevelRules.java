package cz.tacr.elza.drools;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulPackageRules;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.drools.model.Level;
import cz.tacr.elza.drools.model.NewLevel;
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

    public synchronized List<ScenarioOfNewLevel> execute(final ArrLevel level,
                                                         final DirectionLevel directionLevel,
                                                         final ArrFindingAidVersion version)
            throws Exception {

        NewLevelApproaches newLevelApproaches = new NewLevelApproaches();

        Level voLevel = scriptModelFactory.createLevelStructure(level, version);
        List<Level> levels = scriptModelFactory.convertLevelTreeToList(voLevel);
        
        NewLevel newLevel = scriptModelFactory.createNewLevel(voLevel, level, directionLevel, version);
        levels.add(newLevel);

        Path path;
        List<RulPackageRules> rulPackageRules = packageRulesRepository.findByRuleSetAndRuleTypeOrderByPriorityAsc(
                version.getRuleSet(), RulPackageRules.RuleType.NEW_LEVEL);

        for (RulPackageRules rulPackageRule : rulPackageRules) {
            path = Paths.get(RulesExecutor.ROOT_PATH + File.separator + rulPackageRule.getFilename());

            StatelessKieSession session = createNewStatelessKieSession(version.getRuleSet(), path);
            session.setGlobal("results", newLevelApproaches);
            execute(session, levels, path);
        }

        List<ScenarioOfNewLevel> scenarioOfNewLevelList = new LinkedList<>();
        for (NewLevelApproach newLevelApproach : newLevelApproaches.getNewLevelApproaches()) {
            scenarioOfNewLevelList.add(scriptModelFactory.createScenarioOfNewLevel(newLevelApproach));
        }

        return scenarioOfNewLevelList;
    }



}
