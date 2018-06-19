package cz.tacr.elza.core;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.domain.RulArrangementRule;
import cz.tacr.elza.domain.RulComponent;
import cz.tacr.elza.domain.RulExtensionRule;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.domain.RulTemplate;

@Service
public class ResourcePathResolver {

    private static final String TRANSFORMS_DIR = "transformations";
    private static final String PACKAGES_DIR = "packages";
    private static final String GROOVY_DIR = "groovy";
    private static final String DMS_DIR = "dms";

    private static final String EXPORT_XML_DIR = "export-xml";
    private static final String IMPORT_XML_DIR = "import-xml";

    private static final String RULESET_TEMPLATES_DIR = "templates";
    private static final String RULESET_FUNCTIONS = "functions";
    private static final String RULESET_DROOLS = "drools";
    private static final String RULESET_SCRIPTS = "scripts";

    private final StaticDataService staticDataService;

    private final String workDir;

    @Autowired
    public ResourcePathResolver(StaticDataService staticDataService, @Value("${elza.workingDir}") String workDir) {
        this.staticDataService = staticDataService;
        this.workDir = workDir;
    }

    /**
     * Return path to the Elza work directory
     *
     * @return Return path from configuration
     */
    public String getWorkDir() {
        return workDir;
    }

    /**
     * @return Path to data management system (DMS) directory (may not exist).
     */
    @Transactional
    public Path getDmsDir() {
        Path path = Paths.get(workDir, DMS_DIR);

        return path;
    }

    /**
     * @return Path to groovy script directory (may not exist).
     */
    @Transactional
    public Path getGroovyDir() {
        Path path = Paths.get(workDir, GROOVY_DIR);

        return path;
    }

    /**
     * @return Path to export XML transformations directory (may not exist).
     */
    @Transactional
    public Path getExportXmlTrasnformDir() {
        Path path = Paths.get(workDir, TRANSFORMS_DIR, EXPORT_XML_DIR);

        return path;
    }

    /**
     * @return Path to import XML transformations directory (may not exist).
     */
    @Transactional
    public Path getImportXmlTrasnformDir() {
        Path path = Paths.get(workDir, TRANSFORMS_DIR, IMPORT_XML_DIR);

        return path;
    }

    /**
     * @return Path to template directory (may not exist).
     *
     * @param template Persistent entity with initialized OutputType.
     */
    @Transactional(TxType.MANDATORY)
    public Path getTemplateDir(RulTemplate template) {
        RulOutputType outputType = template.getOutputType();
        Validate.isTrue(HibernateUtils.isInitialized(outputType));

        Path templatesPath = getTemplatesDir(template.getPackageId(), outputType.getRuleSetId());
        String templateDir = template.getDirectory();

        Path path = templatesPath.resolve(templateDir);

        return path;
    }

    /**
     * @return Path to templates directory (may not exist).
     */
    @Transactional
    public Path getTemplatesDir(int packageId, int ruleSetId) {
        StaticDataProvider staticData = staticDataService.getData();
        RulPackage rulPackage = staticData.getPackageById(packageId);
        RuleSystem ruleSystem = staticData.getRuleSystemById(ruleSetId);

        Path path = getTemplatesDir(rulPackage, ruleSystem.getRuleSet());

        return path;
    }

    /**
     * @return Path to templates directory (may not exist).
     */
    @Transactional(TxType.MANDATORY)
    public Path getTemplatesDir(RulPackage rulPackage, RulRuleSet ruleSet) {
        Path ruleSetPath = getRuleSetDir(rulPackage, ruleSet);

        Path path = ruleSetPath.resolve(RULESET_TEMPLATES_DIR);

        return path;
    }

    /**
     * @return Path to drool file (may not exist).
     */
    @Transactional(TxType.MANDATORY)
    public Path getDroolFile(RulArrangementRule rule) {
        Path droolsDir = getDroolsDir(rule.getPackageId(), rule.getRuleSetId());
        String droolFile = rule.getComponent().getFilename();

        Path path = droolsDir.resolve(droolFile);

        return path;
    }

    /**
     * @return Path to drool file (may not exist).
     */
    @Transactional(TxType.MANDATORY)
    public Path getDroolFile(RulExtensionRule rulExtensionRule) {
        Path droolsDir = getDroolsDir(rulExtensionRule.getPackage().getPackageId(),
                rulExtensionRule.getArrangementExtension().getRuleSet().getRuleSetId());
        String droolFile = rulExtensionRule.getComponent().getFilename();

        Path path = droolsDir.resolve(droolFile);

        return path;
    }


    /**
     * @return Path to drool file (may not exist). Return null if component does
     *         not exists
     */
    @Transactional(TxType.MANDATORY)
    public Path getDroolFile(RulOutputType outputType) {
        RulComponent component = outputType.getComponent();
        if (component == null) {
            return null;
        }

        Path droolsDir = getDroolsDir(outputType.getPackage().getPackageId(), outputType.getRuleSetId());
        String droolFile = component.getFilename();

        Path path = droolsDir.resolve(droolFile);

        return path;
    }

    /**
     * @return Path to drool file (may not exist).
     */
    @Transactional(TxType.MANDATORY)
    public Path getDroolsFile(RulStructureDefinition structureDefinition) {
        Path droolsDir = getDroolsDir(structureDefinition.getRulPackage().getPackageId());
        String droolFile = structureDefinition.getComponent().getFilename();

        Path path = droolsDir.resolve(droolFile);

        return path;
    }

    /**
     * @return Path to drool file (may not exist).
     */
    @Transactional(TxType.MANDATORY)
    public Path getDroolsFile(RulStructureExtensionDefinition structureExtensionDefinition) {
        Path droolsDir = getDroolsDir(structureExtensionDefinition.getRulPackage().getPackageId());
        String droolFile = structureExtensionDefinition.getComponent().getFilename();

        Path path = droolsDir.resolve(droolFile);

        return path;
    }

    /**
     * @return Path to rule set drools directory (may not exist).
     */
    @Transactional
    public Path getDroolsDir(int packageId, int ruleSetId) {
        StaticDataProvider staticData = staticDataService.getData();
        RulPackage rulPackage = staticData.getPackageById(packageId);
        RuleSystem ruleSystem = staticData.getRuleSystemById(ruleSetId);

        Path path = getDroolsDir(rulPackage, ruleSystem.getRuleSet());

        return path;
    }

    /**
     * @return Path to rule set drools directory (may not exist).
     */
    @Transactional
    public Path getDroolsDir(int packageId) {
        StaticDataProvider staticData = staticDataService.getData();
        RulPackage rulPackage = staticData.getPackageById(packageId);

        Path path = getDroolsDir(rulPackage);

        return path;
    }

    /**
     * @return Path to rule set drools directory (may not exist).
     */
    @Transactional(TxType.MANDATORY)
    public Path getDroolsDir(RulPackage rulPackage, RulRuleSet ruleSet) {
        Path ruleSetPath = getRuleSetDir(rulPackage, ruleSet);

        Path path = ruleSetPath.resolve(RULESET_DROOLS);

        return path;
    }

    /**
     * @return Path to rule set drools directory (may not exist).
     */
    @Transactional(TxType.MANDATORY)
    public Path getDroolsDir(RulPackage rulPackage) {
        Path ruleSetPath = getPackageDir(rulPackage);

        Path path = ruleSetPath.resolve(RULESET_DROOLS);

        return path;
    }

    /**
     * @return Path to function file (may not exist).
     */
    @Transactional(TxType.MANDATORY)
    public Path getFunctionFile(RulAction action) {
        Path functionsDir = getFunctionsDir(action.getPackageId(), action.getRuleSetId());
        String functionFile = action.getFilename();

        Path path = functionsDir.resolve(functionFile);

        return path;
    }

    @Transactional
    public Path getGroovyDir(int packageId, int ruleSetId) {
        StaticDataProvider staticData = staticDataService.getData();
        RulPackage rulPackage = staticData.getPackageById(packageId);
        RuleSystem ruleSystem = staticData.getRuleSystemById(ruleSetId);

        Path path = getGroovyDir(rulPackage, ruleSystem.getRuleSet());

        return path;
    }

    @Transactional
    public Path getGroovyDir(int packageId) {
        StaticDataProvider staticData = staticDataService.getData();
        RulPackage rulPackage = staticData.getPackageById(packageId);

        Path path = getGroovyDir(rulPackage);

        return path;
    }

    /**
     * @return Path to groovy directory (may not exist).
     */
    @Transactional(TxType.MANDATORY)
    public Path getGroovyDir(RulPackage rulPackage, RulRuleSet ruleSet) {
        Path ruleSetPath = getRuleSetDir(rulPackage, ruleSet);

        Path path = ruleSetPath.resolve(RULESET_SCRIPTS);

        return path;
    }

    /**
     * @return Path to groovy directory (may not exist).
     */
    @Transactional(TxType.MANDATORY)
    public Path getGroovyDir(RulPackage rulPackage) {
        Path ruleSetPath = getPackageDir(rulPackage);

        Path path = ruleSetPath.resolve(RULESET_SCRIPTS);

        return path;
    }

    /**
     * @return Path to rule set functions directory (may not exist).
     */
    @Transactional
    public Path getFunctionsDir(int packageId, int ruleSetId) {
        StaticDataProvider staticData = staticDataService.getData();
        RulPackage rulPackage = staticData.getPackageById(packageId);
        RuleSystem ruleSystem = staticData.getRuleSystemById(ruleSetId);

        Path path = getFunctionsDir(rulPackage, ruleSystem.getRuleSet());

        return path;
    }

    /**
     * @return Path to rule set functions directory (may not exist).
     */
    @Transactional(TxType.MANDATORY)
    public Path getFunctionsDir(RulPackage rulPackage, RulRuleSet ruleSet) {
        Path ruleSetPath = getRuleSetDir(rulPackage, ruleSet);

        Path path = ruleSetPath.resolve(RULESET_FUNCTIONS);

        return path;
    }

    private Path getRuleSetDir(RulPackage rulPackage, RulRuleSet ruleSet) {
        Path packagePath = getPackageDir(rulPackage);
        String ruleSetDir = ruleSet.getCode();

        Path path = packagePath.resolve(ruleSetDir);

        return path;
    }

    /**
     * Return path for the package
     * @param rulPackage
     * @return
     */
    @Transactional(TxType.MANDATORY)
    public Path getPackageDir(RulPackage rulPackage) {
        String packageDir = rulPackage.getCode();

        Path path = Paths.get(workDir, PACKAGES_DIR, packageDir + "." + rulPackage.getVersion().toString());

        return path;
    }

	public Path getPackageDirVersion(RulPackage rulPackage, Integer otherVersion) {
        String packageDir = rulPackage.getCode();

        Path path = Paths.get(workDir, PACKAGES_DIR, packageDir + "." + otherVersion.toString());

        return path;
	}
}
