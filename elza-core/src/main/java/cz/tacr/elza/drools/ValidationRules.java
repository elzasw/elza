package cz.tacr.elza.drools;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.kie.api.runtime.StatelessKieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulPolicyType;
import cz.tacr.elza.domain.RulRule;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.domain.vo.DataValidationResults;
import cz.tacr.elza.drools.model.ActiveLevel;
import cz.tacr.elza.drools.service.ModelFactory;
import cz.tacr.elza.drools.service.ScriptModelFactory;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.PolicyTypeRepository;

/**
 * Zpracování pravidel pro validaci parametrů uzlu.
 *
 */

@Component
public class ValidationRules extends Rules {

	@Autowired
	private ScriptModelFactory scriptModelFactory;

	@Autowired
	private ItemTypeRepository itemTypeRepository;

	@Autowired
	private DescItemRepository descItemRepository;

	@Autowired
	private RulesExecutor rulesExecutor;

	@Autowired
	private PolicyTypeRepository policyTypeRepository;

	@Autowired
	private StaticDataService staticDataService;

	private static final Logger logger = LoggerFactory.getLogger(ValidationRules.class);

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

		ActiveLevel activeLevel = scriptModelFactory.createActiveLevel(level, version);

		ModelFactory.addLevelWithParents(activeLevel, facts);

		DataValidationResults validationResults = new DataValidationResults();

		Path path;
		List<RulRule> rulPackageRules = packageRulesRepository
				.findByRuleSetAndRuleTypeOrderByPriorityAsc(version.getRuleSet(), RulRule.RuleType.CONFORMITY_INFO);

		for (RulRule rulPackageRule : rulPackageRules) {
			path = Paths.get(rulesExecutor.getDroolsDir(rulPackageRule.getRuleSet().getCode()) + File.separator + rulPackageRule.getFilename());
			StatelessKieSession session = createNewStatelessKieSession(path);
			session.setGlobal("results", validationResults);
			execute(session, facts);
		}

		List<DataValidationResult> results = validationResults.getResults();

		finalizeValidationResults(results, version.getRuleSetId());

		return results;
	}

	/**
	 * Donačte typy a atributy podle jejich id, která se zadávají ve scriptu.
	 *
	 * @param validationResults
	 *            seznam validačních chyb
	 * @param ruleSetId
	 */
	public void finalizeValidationResults(final List<DataValidationResult> validationResults, Integer ruleSetId) {

		StaticDataProvider sdp = staticDataService.getData();
		RuleSystem ruleSystem = sdp.getRuleSystems().getByRuleSetId(ruleSetId);

		Map<String, RulPolicyType> policyTypesMap = getPolicyTypesMap();

		Iterator<DataValidationResult> iterator = validationResults.iterator();

		while (iterator.hasNext()) {
			DataValidationResult validationResult = iterator.next();

			RulPolicyType rulPolicyType = policyTypesMap.get(validationResult.getPolicyTypeCode());

			if (rulPolicyType == null) {
				logger.warn("Kód '" + validationResult.getPolicyTypeCode()
				        + "' neexistuje. Je nutné upravit drools pravidla");
				iterator.remove();
				continue;
			}

			validationResult.setPolicyType(rulPolicyType);

			switch (validationResult.getResultType()) {

			case MISSING:
				String missingTypeCode = validationResult.getTypeCode();
				if (missingTypeCode == null) {
					throw new SystemException("Neni vyplnen kod chybejiciho typu.", BaseCode.PROPERTY_NOT_EXIST)
					        .set("property", "typeCode");
				}
				validationResult.setType(ruleSystem.getItemTypeByCode(missingTypeCode).getEntity());
				break;
			case ERROR:
				Integer descItemId = validationResult.getDescItemId();
				if (descItemId == null) {
					throw new SystemException("Neni vyplneno id chybneho atributu.", BaseCode.PROPERTY_NOT_EXIST)
					        .set("property", "descItemId");
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
