package cz.tacr.elza.drools;

import static cz.tacr.elza.repository.ExceptionThrow.descItem;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.kie.api.runtime.StatelessKieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.RuleSet;
import cz.tacr.elza.core.data.StaticDataProvider;
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
import cz.tacr.elza.service.RuleService;

/**
 * Zpracování pravidel pro validaci parametrů uzlu.
 *
 */

@Component
public class ValidationRules extends Rules {

    private static final Logger logger = LoggerFactory.getLogger(ValidationRules.class);

	@Autowired
	private ScriptModelFactory scriptModelFactory;

	@Autowired
	private DescItemRepository descItemRepository;

	@Autowired
	private ResourcePathResolver resourcePathResolver;

	@Autowired
    private RuleService ruleService;

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

        long startTime = System.currentTimeMillis();

        StaticDataProvider sdp = staticDataService.getData();
        RuleSet ruleSet = sdp.getRuleSetById(version.getRuleSetId());
        List<RulArrangementRule> rulPackageRules = ruleSet.getRulesByType(RulArrangementRule.RuleType.CONFORMITY_INFO);

        LinkedList<Object> facts = new LinkedList<>();

        ActiveLevel activeLevel = scriptModelFactory.createActiveLevel(level, version);
        logger.debug("Model (workerId: {}) for active level created in {}ms from start",
                     Thread.currentThread().getId(),
                     System.currentTimeMillis() - startTime);

        ModelFactory.addLevelWithParents(activeLevel, facts);
        logger.debug("Added level for parents to model (workerId: {}) in {}ms from start",
                     Thread.currentThread().getId(),
                     System.currentTimeMillis() - startTime);

		DataValidationResults validationResults = new DataValidationResults();

        for (RulArrangementRule rulPackageRule : rulPackageRules) {
            try {
                Path path = resourcePathResolver.getDroolFile(rulPackageRule);
                logger.debug("Executing rule (workerId: {}), path: {} in {}ms from start",
                             Thread.currentThread().getId(), path,
                             System.currentTimeMillis() - startTime);

                StatelessKieSession ksession = createKieStatelessSession(path);
                ksession.setGlobal("results", validationResults);
                executeStateless(ksession, facts);

                long endTime = System.currentTimeMillis();
                logger.debug("Rule executed (workerId: {}) in {}ms",
                             Thread.currentThread().getId(),
                             endTime - startTime);
            } catch (Exception e) {
                logger.error("Failed to validate, exception: ", e);
            }
		}

		List<RulExtensionRule> rulExtensionRules = ruleService.findExtensionRuleByNode(level.getNode(), RulExtensionRule.RuleType.CONFORMITY_INFO);
		for (RulExtensionRule rulExtensionRule : rulExtensionRules) {
            try {
                Path path = resourcePathResolver.getDroolFile(rulExtensionRule);

                logger.debug("Executing extension (workerId: {}), path: {}", Thread.currentThread().getId(), path);

                StatelessKieSession ksession = createKieStatelessSession(path);
                ksession.setGlobal("results", validationResults);
                executeStateless(ksession, facts);

                long endTime = System.currentTimeMillis();
                logger.debug("Extension executed (workerId: {}) in {}ms",
                             Thread.currentThread().getId(),
                             endTime - startTime);
            } catch (Exception e) {
                logger.error("Failed to validate extension, exception: ", e);
            }
		}

		List<DataValidationResult> results = validationResults.getResults();

		finalizeValidationResults(results, version.getRuleSetId());

        logger.debug("Finalized results (workerId: {}) in {}ms from start",
                     Thread.currentThread().getId(),
                     System.currentTimeMillis() - startTime);

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

        long startTime = System.currentTimeMillis();
        logger.debug("Started results finalization (workerId: {})",
                     Thread.currentThread().getId());

		StaticDataProvider sdp = staticDataService.getData();

        Map<String, RulPolicyType> policyTypesMap = sdp.getPolicyTypesMap();

		Iterator<DataValidationResult> iterator = validationResults.iterator();

		while (iterator.hasNext()) {

            DataValidationResult validationResult = iterator.next();

            logger.debug("Storing DataValidationResult (workerId: {}, object: {}) in {}ms from start",
                         Thread.currentThread().getId(),
                         validationResult,
                         System.currentTimeMillis() - startTime);

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
				validationResult.setDescItem(descItemRepository.findById(descItemId).orElseThrow(descItem(descItemId)));
				break;
			default:
				throw new IllegalArgumentException(
				        "Neznamy typ vysledku validace " + validationResult.getResultType().name());
			}
            logger.debug("Stored DataValidationResult (workerId: {}, object: {}) in {}ms from start",
                         Thread.currentThread().getId(),
                         validationResult,
                         System.currentTimeMillis() - startTime);
		}

        logger.debug("Stored all DataValidationResult (workerId: {}) in {}ms from start",
                     Thread.currentThread().getId(),
                     System.currentTimeMillis() - startTime);
	}
}
