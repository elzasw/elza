package cz.tacr.elza.drools;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.drools.model.VOLevel;
import cz.tacr.elza.drools.model.VOScenarioOfNewLevel;
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

        StatelessKieSession session = createNewStatelessKieSession(version.getRuleSet());
        List<VOScenarioOfNewLevel> voScenarioOfNewLevelList = new LinkedList<>();

        session.setGlobal("results", voScenarioOfNewLevelList);

        VOLevel voLevel = scriptModelFactory.createLevelStructure(level, directionLevel, version);

        execute(session, version.getRuleSet(), Arrays.asList(voLevel));

        List<ScenarioOfNewLevel> scenarioOfNewLevelList = new LinkedList<>();
        for (VOScenarioOfNewLevel voScenarioOfNewLevel : voScenarioOfNewLevelList) {
            scenarioOfNewLevelList.add(scriptModelFactory.createScenarioOfNewLevel(voScenarioOfNewLevel));
        }

        return scenarioOfNewLevelList;
    }


    @Override
    protected String getFileName() {
        return "scenarioOfNewLevel" + RulesExecutor.FILE_EXTENSION;
    }
}
