package cz.tacr.elza.packageimport;

import java.io.File;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;

/**
 * Context and basic infrastructure for rule update.
 *
 */
public class RuleUpdateContext {

    private final PackageContext puc;

	private final ResourcePathResolver resourcePathResolver;

	private final RulPackage rulPackage;

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
	private final String keyDirPath;

    public RuleUpdateContext(PackageContext puc, RulPackage rulPackage, RulRuleSet rulRuleSet,
            ResourcePathResolver resourcePathResolver, String ruleDirPath) {
        this.puc = puc;
		this.rulPackage = rulPackage;
		this.rulRuleSet = rulRuleSet;
		this.resourcePathResolver = resourcePathResolver;
		this.keyDirPath = ruleDirPath;
	}

	/**
	 * Initialize rule update context
	 */
	public void init()
	{
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
		return rulPackage;
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

}
