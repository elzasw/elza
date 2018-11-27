package cz.tacr.elza.packageimport;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;

/**
 * Context and basic infrastructure for rule update.
 *
 */
public class RuleUpdateContext {

    public enum RuleState {
        // Rules are new or already exists
        UPDATE,
        // This is only addon to existing rules
        // Such rules are defined by another package
        ADDON,
        // Rules will be deleted
        DELETE
    }

    public interface RuleUpdateAction {
        void run(RuleUpdateContext ruc);
    }

    private final RuleState ruleState;

    private final PackageContext puc;

	private final ResourcePathResolver resourcePathResolver;

	private final RulRuleSet rulRuleSet;

	private File dirActions;

	private File dirRules;

	private File dirGroovies;

	private File dirTemplates;

	/**
	 * Base path into byteStreams map
	 *
	 * This is usualy parent folder for the input ZIP file
	 */
    private String keyDirPath;

    /**
     * Collection of actions to be run in second phase
     */
    List<RuleUpdateAction> actionsPhase2 = new ArrayList<>();

    public RuleUpdateContext(final RuleState ruleState,
                             final PackageContext puc,
                             final RulRuleSet rulRuleSet,
                             final ResourcePathResolver resourcePathResolver) {
        this.ruleState = ruleState;
        this.puc = puc;
		this.rulRuleSet = rulRuleSet;
		this.resourcePathResolver = resourcePathResolver;
        init();
	}

	/**
	 * Initialize rule update context
	 */
    private void init()
	{
        keyDirPath = PackageService.ZIP_DIR_RULE_SET + "/" + rulRuleSet.getCode() + "/";

        RulPackage rulPackage = puc.getPackage();
		dirActions = resourcePathResolver.getFunctionsDir(rulPackage, rulRuleSet).toFile();
		if (!dirActions.exists()) {
			dirActions.mkdirs();
		}

		dirRules = resourcePathResolver.getDroolsDir(rulPackage, rulRuleSet).toFile();
		if (!dirRules.exists()) {
			dirRules.mkdirs();
		}

		dirGroovies = resourcePathResolver.getGroovyDir(rulPackage, rulRuleSet).toFile();
		if (!dirGroovies.exists()) {
			dirGroovies.mkdirs();
		}

		dirTemplates = resourcePathResolver.getTemplatesDir(rulPackage, rulRuleSet).toFile();
		if (!dirTemplates.exists()) {
			dirTemplates.mkdirs();
		}

	}

    public PackageContext getPackageUpdateContext() {
        return puc;
    }

	public File getRulesDir() {
		return dirRules;
	}

	public File getTemplatesDir() {
		return dirTemplates;
	}

	public File getActionsDir() {
		return dirActions;
	}

	public RulPackage getRulPackage() {
        return puc.getPackage();
	}

	public String getRulSetCode() {
		return rulRuleSet.getCode();
	}

	public RulRuleSet getRulSet() {
		return rulRuleSet;
	}

    public String getKeyDirPath() {
        return this.keyDirPath;
    }

    public RuleState getRuleState() {
        return ruleState;
    }

    public <T> T convertXmlStreamToObject(final Class<T> classObject, String fileName) {
        return puc.convertXmlStreamToObject(classObject, keyDirPath + fileName);
    }

    public void addActionPhase2(RuleUpdateAction action) {
        actionsPhase2.add(action);
    }

    public void runActionsPhase2() {
        actionsPhase2.forEach(
                              action -> action.run(this));

    }

}
