package cz.tacr.elza.drools;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.tacr.elza.core.data.ItemType;
import org.kie.api.runtime.StatelessKieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulArrangementRule;
import cz.tacr.elza.domain.RulExtensionRule;
import cz.tacr.elza.domain.RulPolicyType;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.domain.vo.DataValidationResults;
import cz.tacr.elza.drools.model.ActiveLevel;
import cz.tacr.elza.drools.service.ModelFactory;
import cz.tacr.elza.drools.service.ScriptModelFactory;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.PolicyTypeRepository;
import cz.tacr.elza.service.RuleService;

/**
 * Zpracování pravidel pro validaci parametrů uzlu.
 *
 */

@Component
public class ValidationRules extends Rules {

	@Autowired
	private ScriptModelFactory scriptModelFactory;

	@Autowired
	private DescItemRepository descItemRepository;

	@Autowired
	private ResourcePathResolver resourcePathResolver;

	@Autowired
	private PolicyTypeRepository policyTypeRepository;

	@Autowired
	private StaticDataService staticDataService;
	@Autowired
	private RuleService ruleService;

	private static final Logger logger = LoggerFactory.getLogger(ValidationRules.class);

	/**
     * Spustí validaci atributů.
     *
     * @param level
     *            level, na kterým spouštíme validaci
     * @param version
     *            verze, do které spadá uzel
     * @return seznam validačních chyb nebo prázdný seznam
     * @throws IOException
     */
    public synchronized List<DataValidationResult> execute(final ArrLevel level, final ArrFundVersion version)
            throws IOException {

		LinkedList<Object> facts = new LinkedList<>();

		ActiveLevel activeLevel = scriptModelFactory.createActiveLevel(level, version);

		ModelFactory.addLevelWithParents(activeLevel, facts);

		DataValidationResults validationResults = new DataValidationResults();

		List<RulArrangementRule> rulPackageRules = arrangementRuleRepository
				.findByRuleSetAndRuleTypeOrderByPriorityAsc(version.getRuleSet(), RulArrangementRule.RuleType.CONFORMITY_INFO);

        for (RulArrangementRule rulPackageRule : rulPackageRules) {
			Path path = resourcePathResolver.getDroolFile(rulPackageRule);
			StatelessKieSession session = createNewStatelessKieSession(path);
			session.setGlobal("results", validationResults);
            session.execute(facts);
		}

		List<RulExtensionRule> rulExtensionRules = ruleService.findExtensionRuleByNode(level.getNode(), RulExtensionRule.RuleType.CONFORMITY_INFO);
		for (RulExtensionRule rulExtensionRule : rulExtensionRules) {
            Path path = resourcePathResolver.getDroolFile(rulExtensionRule);
			StatelessKieSession session = createNewStatelessKieSession(path);
            session.execute(facts);
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

		Map<String, RulPolicyType> policyTypesMap = getPolicyTypesMap();

		Iterator<DataValidationResult> iterator = validationResults.iterator();

		while (iterator.hasNext()) {
			DataValidationResult validationResult = iterator.next();

            // policy code has to be set
            String polCode = validationResult.getPolicyTypeCode();
            if (polCode == null) {
                throw new SystemException("Policy code not found", BaseCode.INVALID_STATE)
                        .set("message", validationResult.getMessage())
                        .set("resultType", validationResult.getResultType());
            }

            RulPolicyType rulPolicyType = policyTypesMap.get(polCode);
            if (rulPolicyType == null) {
                throw new SystemException("Policy code not found", BaseCode.INVALID_STATE)
                        .set("message", validationResult.getMessage())
                        .set("resultType", validationResult.getResultType())
                        .set("policyCode", validationResult.getPolicyTypeCode());
            }

            validationResult.setPolicyType(rulPolicyType);

			switch (validationResult.getResultType()) {

			case MISSING:
				String missingTypeCode = validationResult.getTypeCode();
				if (missingTypeCode == null) {
					throw new SystemException("Neni vyplnen kod chybejiciho typu.", BaseCode.PROPERTY_NOT_EXIST)
					        .set("property", "typeCode");
				}
				ItemType itemType = sdp.getItemTypeByCode(missingTypeCode);
				if (itemType == null) {
					throw new SystemException("Item type not found", BaseCode.INVALID_STATE)
							.set("message", validationResult.getMessage())
							.set("typeCode", validationResult.getTypeCode());
				}
				validationResult.setType(itemType.getEntity());
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
