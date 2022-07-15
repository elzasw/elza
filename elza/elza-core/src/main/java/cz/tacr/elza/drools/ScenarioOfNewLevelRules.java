package cz.tacr.elza.drools;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.RuleSet;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulArrangementRule;
import cz.tacr.elza.domain.RulExtensionRule;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
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
        StaticDataProvider sdp = staticDataService.getData();
        RuleSet ruleSet = sdp.getRuleSetById(version.getRuleSetId());
        List<RulArrangementRule> rulArrangementRules = ruleSet.getRulesByType(RulArrangementRule.RuleType.NEW_LEVEL);

        NewLevelApproaches newLevelApproaches = new NewLevelApproaches();

        LinkedList<Object> facts = new LinkedList<>();
        facts.addAll(scriptModelFactory.createFactsForNewLevel(level, directionLevel, version));

        for (RulArrangementRule rulArrangementRule : rulArrangementRules) {
            Path path = resourcePathResolver.getDroolFile(rulArrangementRule);

            KieSession session = createKieSession(path);
            session.setGlobal("results", newLevelApproaches);
            executeSession(session, facts);
        }

        List<RulExtensionRule> rulExtensionRules = ruleService.findExtensionRuleByNode(level.getNode(), RulExtensionRule.RuleType.NEW_LEVEL);
        for (RulExtensionRule rulExtensionRule : rulExtensionRules) {
            Path path = resourcePathResolver.getDroolFile(rulExtensionRule);

            KieSession session = createKieSession(path);
            session.setGlobal("results", newLevelApproaches);
            executeSession(session, facts);
        }

        List<ScenarioOfNewLevel> scenarioOfNewLevelList = new LinkedList<>();
        for (NewLevelApproach newLevelApproach : newLevelApproaches.getNewLevelApproaches()) {
			scenarioOfNewLevelList
			        .add(scriptModelFactory.createScenarioOfNewLevel(newLevelApproach, version.getRuleSetId()));
        }

        return scenarioOfNewLevelList;
    }



}
