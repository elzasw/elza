package cz.tacr.elza.drools;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.PolicyTypeRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

	@Autowired
	private RulesExecutor rulesExecutor;

	@Autowired
	private PolicyTypeRepository policyTypeRepository;

	private Log logger = LogFactory.getLog(this.getClass());

	/**
	 * Spustí validaci atributů.
	 *
	 * @param level
	 *            level, na kterým spouštíme validaci
	 * @param version
	 *            verze, do které spadá uzel
	 * @return seznam validačních chyb nebo prázdný seznam
	 */
	public synchronized List<DataValidationResult> execute(final ArrLevel level, final ArrFundVersion version) throws Exception {

		LinkedList<Object> facts = new LinkedList<>();

		// prepare list of levels
		Level modelLevel = scriptModelFactory.createLevelModel(level, version);
		ActiveLevel activeLevel = scriptModelFactory.createActiveLevel(modelLevel, level, version);
		ModelFactory.addAll(activeLevel, facts);

		// Add arrangement type
		RulArrangementType arrangementType = version.getArrangementType();
		facts.add(arrangementType);

		DataValidationResults validationResults = new DataValidationResults();

		Path path;
		List<RulRule> rulPackageRules = packageRulesRepository
				.findByRuleSetAndRuleTypeOrderByPriorityAsc(version.getRuleSet(), RulRule.RuleType.CONFORMITY_INFO);

		for (RulRule rulPackageRule : rulPackageRules) {
			path = Paths.get(rulesExecutor.getRootPath() + File.separator + rulPackageRule.getFilename());
			StatelessKieSession session = createNewStatelessKieSession(path);
			session.setGlobal("results", validationResults);
			execute(session, facts);
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

		Map<String, RulPolicyType> policyTypesMap = getPolicyTypesMap();

		Iterator<DataValidationResult> iterator = validationResults.iterator();

		while (iterator.hasNext()) {
			DataValidationResult validationResult = iterator.next();

			RulPolicyType rulPolicyType = policyTypesMap.get(validationResult.getPolicyTypeCode());

			if (rulPolicyType == null) {
				logger.warn("Kód '" + validationResult.getPolicyTypeCode() + "' neexistuje. Je nutné upravit drools pravidla");
				iterator.remove();
				continue;
			}

			validationResult.setPolicyType(rulPolicyType);

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

	/**
	 * Získání mapy typů kontrol.
	 * @return mapa typů kontrol
     */
	private Map<String, RulPolicyType> getPolicyTypesMap() {
		List<RulPolicyType> policyTypes = policyTypeRepository.findAll();
		return policyTypes.stream().collect(Collectors.toMap(RulPolicyType::getCode, Function.identity()));
	}

}
