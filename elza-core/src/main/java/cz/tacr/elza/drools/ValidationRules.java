package cz.tacr.elza.drools;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.RulRule;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.domain.vo.DataValidationResults;
import cz.tacr.elza.drools.model.ActiveLevel;
import cz.tacr.elza.drools.model.Level;
import cz.tacr.elza.drools.service.ModelFactory;
import cz.tacr.elza.drools.service.ScriptModelFactory;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;

/**
 * Zpracování pravidel pro validaci parametrů uzlu.
 *
 * @author Tomáš Kubový [
 *         <a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @author Petr Pytelka [
 *         <a href="mailto:petr.pytelka@lightcomp.cz">petr.pytelka@lightcomp.cz
 *         </a>]
 * @since 1.12.2015
 */

@Component
public class ValidationRules extends Rules {

	@Autowired
	private ScriptModelFactory scriptModelFactory;
	@Autowired
	private DescItemTypeRepository descItemTypeRepository;

	@Autowired
	private DescItemRepository descItemRepository;

	/**
	 * Spustí validaci atributů.
	 *
	 * @param level
	 *            level, na kterým spouštíme validaci
	 * @param version
	 *            verze, do které spadá uzel
	 * @param strategies
	 *            strategie vyhodnocování
	 * @return seznam validačních chyb nebo prázdný seznam
	 */
	public synchronized List<DataValidationResult> execute(final ArrLevel level, final ArrFindingAidVersion version,
			final Set<String> strategies) throws Exception {

		LinkedList<Object> facts = new LinkedList<>();

		// prepare list of levels
		Level modelLevel = scriptModelFactory.createLevelModel(level, version);
		ActiveLevel activeLevel = scriptModelFactory.createActiveLevel(modelLevel, level, version);
		ModelFactory.addAll(activeLevel, facts);

		// Add arrangement type
		RulArrangementType arrangementType = version.getArrangementType();
		facts.add(arrangementType);

		// Add strategies
		facts.addAll(ModelFactory.createStrategies(strategies));

		DataValidationResults validationResults = new DataValidationResults();

		Path path;
		List<RulRule> rulPackageRules = packageRulesRepository
				.findByRuleSetAndRuleTypeOrderByPriorityAsc(version.getRuleSet(), RulRule.RuleType.CONFORMITY_INFO);

		for (RulRule rulPackageRule : rulPackageRules) {
			path = Paths.get(RulesExecutor.ROOT_PATH + File.separator + rulPackageRule.getFilename());
			StatelessKieSession session = createNewStatelessKieSession(version.getRuleSet(), path);
			session.setGlobal("results", validationResults);
			execute(session, facts, path);
		}
		
		List<DataValidationResult> results = validationResults.getResults();

		finalizeValidationResults(results);

		return results;
	}

	/**
	 * Donačte typy a atributy podle jejich id, která se zadávají ve scriptu.
	 *
	 * @param validationResults
	 *            seznam validačních chyb
	 */
	private void finalizeValidationResults(final List<DataValidationResult> validationResults) {

		for (DataValidationResult validationResult : validationResults) {
			switch (validationResult.getResultType()) {

			case MISSING:
				String missingTypeCode = validationResult.getTypeCode();
				if (missingTypeCode == null) {
					throw new IllegalStateException("Neni vyplnen kod chybejiciho typu.");
				}
				validationResult.setType(descItemTypeRepository.getOneByCode(missingTypeCode));
				break;
			case ERROR:
				Integer descItemId = validationResult.getDescItemId();
				if (descItemId == null) {
					throw new IllegalStateException("Neni vyplneno id chybneho atributu.");
				}
				validationResult.setDescItem(descItemRepository.findOne(descItemId));
				break;
			default:
				throw new IllegalArgumentException(
						"Neznamy typ vysledku validace " + validationResult.getResultType().name());
			}
		}
	}

}
