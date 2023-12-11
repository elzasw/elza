package cz.tacr.elza.packageimport;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileSystemUtils;

import cz.tacr.elza.bulkaction.BulkActionConfigManager;
import cz.tacr.elza.common.AutoDeletingTempFile;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.RuleSet;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.domain.ApExternalIdType;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.domain.RulActionRecommended;
import cz.tacr.elza.domain.RulArrangementExtension;
import cz.tacr.elza.domain.RulArrangementRule;
import cz.tacr.elza.domain.RulComponent;
import cz.tacr.elza.domain.RulExportFilter;
import cz.tacr.elza.domain.RulExtensionRule;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeAction;
import cz.tacr.elza.domain.RulItemTypeSpecAssign;
import cz.tacr.elza.domain.RulOutputFilter;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPackageDependency;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.RulPolicyType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UISettings.EntityType;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.WfIssueState;
import cz.tacr.elza.domain.WfIssueType;
import cz.tacr.elza.domain.bridge.IndexConfigurationReader;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.packageimport.RuleUpdateContext.RuleState;
import cz.tacr.elza.packageimport.autoimport.PackageInfoWrapper;
import cz.tacr.elza.packageimport.xml.APTypeXml;
import cz.tacr.elza.packageimport.xml.APTypes;
import cz.tacr.elza.packageimport.xml.ActionItemType;
import cz.tacr.elza.packageimport.xml.ActionRecommended;
import cz.tacr.elza.packageimport.xml.ActionXml;
import cz.tacr.elza.packageimport.xml.ActionsXml;
import cz.tacr.elza.packageimport.xml.ArrangementExtension;
import cz.tacr.elza.packageimport.xml.ArrangementExtensions;
import cz.tacr.elza.packageimport.xml.ArrangementRule;
import cz.tacr.elza.packageimport.xml.ArrangementRules;
import cz.tacr.elza.packageimport.xml.ExportFilterXml;
import cz.tacr.elza.packageimport.xml.ExportFiltersXml;
import cz.tacr.elza.packageimport.xml.ExtensionRule;
import cz.tacr.elza.packageimport.xml.ExtensionRules;
import cz.tacr.elza.packageimport.xml.ExternalIdType;
import cz.tacr.elza.packageimport.xml.ExternalIdTypes;
import cz.tacr.elza.packageimport.xml.IssueState;
import cz.tacr.elza.packageimport.xml.IssueStates;
import cz.tacr.elza.packageimport.xml.IssueType;
import cz.tacr.elza.packageimport.xml.IssueTypes;
import cz.tacr.elza.packageimport.xml.ItemSpec;
import cz.tacr.elza.packageimport.xml.ItemSpecs;
import cz.tacr.elza.packageimport.xml.ItemType;
import cz.tacr.elza.packageimport.xml.ItemTypes;
import cz.tacr.elza.packageimport.xml.OutputFilterXml;
import cz.tacr.elza.packageimport.xml.OutputFiltersXml;
import cz.tacr.elza.packageimport.xml.OutputType;
import cz.tacr.elza.packageimport.xml.OutputTypes;
import cz.tacr.elza.packageimport.xml.PackageDependency;
import cz.tacr.elza.packageimport.xml.PackageInfo;
import cz.tacr.elza.packageimport.xml.PartType;
import cz.tacr.elza.packageimport.xml.PartTypes;
import cz.tacr.elza.packageimport.xml.PolicyType;
import cz.tacr.elza.packageimport.xml.PolicyTypes;
import cz.tacr.elza.packageimport.xml.RuleSetXml;
import cz.tacr.elza.packageimport.xml.RuleSets;
import cz.tacr.elza.packageimport.xml.Setting;
import cz.tacr.elza.packageimport.xml.SettingFavoriteItemSpecs;
import cz.tacr.elza.packageimport.xml.Settings;
import cz.tacr.elza.packageimport.xml.StructureDefinition;
import cz.tacr.elza.packageimport.xml.StructureDefinitions;
import cz.tacr.elza.packageimport.xml.StructureType;
import cz.tacr.elza.packageimport.xml.StructureTypes;
import cz.tacr.elza.packageimport.xml.TemplateXml;
import cz.tacr.elza.packageimport.xml.Templates;
import cz.tacr.elza.repository.ActionRecommendedRepository;
import cz.tacr.elza.repository.ActionRepository;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApExternalIdTypeRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.ArrangementExtensionRepository;
import cz.tacr.elza.repository.ArrangementRuleRepository;
import cz.tacr.elza.repository.ComponentRepository;
import cz.tacr.elza.repository.ExportFilterRepository;
import cz.tacr.elza.repository.ExtensionRuleRepository;
import cz.tacr.elza.repository.ItemAptypeRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeActionRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.ItemTypeSpecAssignRepository;
import cz.tacr.elza.repository.OutputFilterRepository;
import cz.tacr.elza.repository.OutputRepository;
import cz.tacr.elza.repository.OutputResultRepository;
import cz.tacr.elza.repository.OutputTemplateRepository;
import cz.tacr.elza.repository.OutputTypeRepository;
import cz.tacr.elza.repository.PackageDependencyRepository;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.repository.Packaging;
import cz.tacr.elza.repository.PartTypeRepository;
import cz.tacr.elza.repository.PolicyTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.SettingsRepository;
import cz.tacr.elza.repository.StructureDefinitionRepository;
import cz.tacr.elza.repository.StructureExtensionDefinitionRepository;
import cz.tacr.elza.repository.StructuredTypeExtensionRepository;
import cz.tacr.elza.repository.StructuredTypeRepository;
import cz.tacr.elza.repository.TemplateRepository;
import cz.tacr.elza.repository.WfIssueStateRepository;
import cz.tacr.elza.repository.WfIssueTypeRepository;
import cz.tacr.elza.search.IndexWorkProcessor;
import cz.tacr.elza.security.AuthorizationRequest;
import cz.tacr.elza.service.AsyncRequestService;
import cz.tacr.elza.service.CacheService;
import cz.tacr.elza.service.SettingsService;
import cz.tacr.elza.service.SpringContext;
import cz.tacr.elza.service.StructObjService;
import cz.tacr.elza.service.StructObjValueService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.event.CacheInvalidateEvent;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.ActionEvent;
import cz.tacr.elza.service.eventnotification.events.EventType;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;


/**
 * Service pro správu importovaných balíčků s pravidly, hromadnými akcemi apod.
 *
 * @since 14.12.2015
 */
@Service
public class PackageService {

    private static final Logger logger = LoggerFactory.getLogger(PackageService.class);

    @Value("${elza.package.testing:false}")
    private Boolean testing;

    /**
     * pravidla v zipu
     */
    public static final String RULE_SET_XML = "rul_rule_set.xml";

    /**
     * typy kontrol, validací, archivního popisu
     */
    public static final String POLICY_TYPE_XML = "rul_policy_type.xml";

    /**
     * specifikace atributů v zipu
     */
    public static final String ITEM_SPEC_XML = "rul_item_spec.xml";

    /**
     * typy atributů v zipu
     */
    public static final String ITEM_TYPE_XML = "rul_item_type.xml";

    /**
     * hromadné akce v zipu
     */
    public static final String PACKAGE_ACTIONS_XML = "rul_package_actions.xml";

    /**
     * výstupní filtry v zipu
     */
    public static final String PACKAGE_OUTPUT_FILTERS_XML = "rul_package_output_filters.xml";

    /**
     * exportní filtry v zipu
     */
    public static final String PACKAGE_EXPORT_FILTERS_XML = "rul_package_export_filters.xml";

    /**
     * základní pravidla v zipu
     */
    public static final String ARRANGEMENT_RULE_XML = "rul_arrangement_rule.xml";

    /**
     *
     */
    public static final String ARRANGEMENT_EXTENSION_XML = "rul_arrangement_extension.xml";

    /**
     *
     */
    public static final String EXTENSION_RULE_XML = "rul_extension_rule.xml";

    /**
     * Pro strukturovaný datový typ a jeho rozšíření.
     */
    public static final String STRUCTURE_DEFINITION_XML = "rul_structure_definition.xml";
    public static final String STRUCTURE_EXTENSION_DEFINITION_XML = "rul_structure_extension_definition.xml";
    public static final String STRUCTURE_EXTENSION_XML = "rul_structure_extension.xml";
    public static final String STRUCTURE_TYPE_XML = "rul_structure_type.xml";

    /**
     * Typy částí
     */
    public static final String PART_TYPE_XML = "rul_part_type.xml";

    /**
     * typy outputů
     */
    public static final String OUTPUT_TYPE_XML = "rul_output_type.xml";

    /**
     * Osoby... TODO
     */
    public static final String SETTING_XML = "ui_setting.xml";

    /**
     * typy externích identifikátorů
     */
    public static final String EXTERNAL_ID_TYPE_XML = "ap_external_id_type.xml";

    /**
     * stavy připomínek
     */
    public static final String ISSUE_STATE_XML = "wf_issue_state.xml";

    /**
     * druhy připomínek
     */
    public static final String ISSUE_TYPE_XML = "wf_issue_type.xml";

    /**
     * Složka templatů
     */
    public final static String ZIP_DIR_TEMPLATES = "templates";

    /**
     * název složky pro vyhledání pravidel
     */
    public static final String ZIP_DIR_RULE_SET = "rul_rule_set";

    /**
     * adresář pro hromadné akce v zip
     */
    static public final String ZIP_DIR_ACTIONS = "bulk_actions";

    /**
     * adresář pro výstupní filtry v zip
     */
    static public final String ZIP_DIR_OUTPUT_FILTERS = "output_filters";

    /**
     * adresář pro exportní filtry v zip
     */
    static public final String ZIP_DIR_EXPORT_FILTERS = "export_filters";

    /**
     * adresář pro pravidla v zip
     */
    static public final String ZIP_DIR_RULES = "rules";

    /**
     * adresář pro groovy v zip
     */
    static public final String ZIP_DIR_SCRIPTS = "scripts";

    private static final String AVAILABLE_ITEMS = "AVAILABLE_ITEMS";
    private static final String VALIDATION = "VALIDATION";

    /**
     *  soubor s názvem a verzí balíčku
     */
    private static final String PACKAGE_XML = "package.xml";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private PackageRepository packageRepository;

    @Autowired
    private RuleSetRepository ruleSetRepository;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private ItemTypeSpecAssignRepository itemTypeSpecAssignRepository;

    @Autowired
    private ItemAptypeRepository itemAptypeRepository;

    @Autowired
    private ActionRepository packageActionsRepository;

    @Autowired
    private ArrangementRuleRepository arrangementRuleRepository;

    @Autowired
    private BulkActionConfigManager bulkActionConfigManager;

    @Autowired
    private PolicyTypeRepository policyTypeRepository;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private OutputTypeRepository outputTypeRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private ActionRecommendedRepository actionRecommendedRepository;

    @Autowired
    private ItemTypeActionRepository itemTypeActionRepository;

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    @Autowired
    private OutputRepository outputRepository;

    @Autowired
    private OutputTemplateRepository outputTemplateRepository;

    @Autowired
    private ApStateRepository apStateRepository;

    @Autowired
    private ApTypeRepository apTypeRepository;

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private PackageDependencyRepository packageDependencyRepository;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private ArrangementExtensionRepository arrangementExtensionRepository;

    @Autowired
    private ExtensionRuleRepository extensionRuleRepository;

    @Autowired
    private ApExternalIdTypeRepository externalIdTypeRepository;

    @Autowired
    private StructuredTypeRepository structureTypeRepository;

    @Autowired
    private StructureDefinitionRepository structureDefinitionRepository;

    @Autowired
    private StructuredTypeExtensionRepository structureExtensionRepository;

    @Autowired
    private StructureExtensionDefinitionRepository structureExtensionDefinitionRepository;

    @Autowired
    private StructObjService structureService;

    @Autowired
    private StructObjValueService structObjValueService;

    @Autowired
    private ApAccessPointRepository accessPointRepository;

    @Autowired
    private OutputResultRepository outputResultRepository;

    @Autowired
    private PartTypeRepository partTypeRepository;

    @Autowired
    private NodeCacheService nodeCacheService;

    @Autowired
    private WfIssueTypeRepository issueTypeRepository;

    @Autowired
    private WfIssueStateRepository issueStateRepository;

    @Autowired
    private IndexWorkProcessor indexWorkProcessor;

    @Autowired
    private AsyncRequestService asyncRequestService;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private OutputFilterRepository outputFilterRepository;

    @Autowired
    private ExportFilterRepository exportFilterRepository;

    @Autowired
    private UserService userService;

    @Autowired
    @Qualifier("transactionManager")
    protected PlatformTransactionManager txManager;

    IndexConfigurationReader configurationReader = SpringContext.getBean(IndexConfigurationReader.class);

    private Set<Integer> accessPoints;

    /**
     * Provede import balíčku.
     *
     * @param file soubor balíčku
     *             <p>
     *             Note: only one package can be imported at a time
     */
    synchronized public void importPackage(final File file) {
    	// check authorization
        TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
        transactionTemplate.executeWithoutResult(ts -> {
            AuthorizationRequest authRequest = AuthorizationRequest.hasPermission(UsrPermission.Permission.ADMIN);
        	userService.authorizeRequest(authRequest);
        });
        // stop services - outside transaction
        preImportPackage();

        TransactionTemplate transactionTemplate2 = new TransactionTemplate(txManager);
        transactionTemplate2.executeWithoutResult(ts -> {
            importPackageInternal(file, true);
        });
    }

    public void importPackageInternal(final File file, boolean startTasks) {

        // read package and do basic checks
        PackageContext pkgCtx = new PackageContext(resourcePathResolver);

        File oldPackageDir = null;
        File packageDir = null;

        try {
            pkgCtx.init(file);

            processRulPackage(pkgCtx);
            packageDir = pkgCtx.preparePackageDir();

            // do import
            importPackageInternal(pkgCtx);

            // get old package dir as last step - delete only when success
            oldPackageDir = pkgCtx.getOldPackageDir();
        } catch (Exception e) {
            logger.error("Failed to import package", e);

            // drop new package dir
            if (packageDir != null) {
                FileSystemUtils.deleteRecursively(packageDir);
            }

            if (e instanceof AbstractException) {
                throw (AbstractException) e;
            } else {
                throw new SystemException(e);
            }
        } finally {
            if (pkgCtx != null) {

                // start services after import
                postImportPackage(pkgCtx, startTasks);

                pkgCtx.close();
                pkgCtx = null;
                accessPoints = null;
            }
        }

        if (oldPackageDir != null && !oldPackageDir.equals(packageDir)) {
            FileSystemUtils.deleteRecursively(oldPackageDir);
        }

    }

    public void preImportPackage() {
        logger.info("Stoping services before package update");

        // zastavit indexovani
        stopAsyncTasks();

        // odebrání používaných groovy scritpů
        cacheService.resetCache(CacheInvalidateEvent.Type.GROOVY);
    }

    public void startAsyncTasks() {

        logger.debug("Starting async threads...");

        structObjValueService.startGenerator();

        // spustit indexovani
        indexWorkProcessor.resumeIndexing();

        asyncRequestService.start();

        logger.info("All async threads started.");
    }

    public void stopAsyncTasks() {
        logger.debug("Stopping async threads...");

        asyncRequestService.stop();

        // zastavit indexovani
        indexWorkProcessor.suspendIndexing();

        structObjValueService.stopGenerator();

        logger.info("All async threads stopped.");
    }

    private void postImportPackage(PackageContext pkgCtx, boolean startTasks) {
        if (pkgCtx.isSyncNodeCache()) {
            nodeCacheService.syncCache();
        }

        logger.info("Package was updated. Code: {}, Version: {}", pkgCtx.getPackageInfo().getCode(),
                pkgCtx.getPackageInfo().getVersion());

        try {

            // add request to regenerate structObjs
            Set<String> codes = pkgCtx.getRegenerateStructureTypes();
            List<RulStructuredType> revalidateStructureTypes = new ArrayList<>(codes.size());
            for (String code : codes) {
                RulStructuredType structType = this.structureTypeRepository.findByCode(code);
                revalidateStructureTypes.add(structType);
            }

            structObjValueService.addToValidateByTypes(revalidateStructureTypes);


        } finally {
            if (startTasks) {
                startAsyncTasks();
            }
        }

        logger.info("Services were restarted after package update");
    }

    public void importPackageInternal(final PackageContext pkgCtx) throws IOException {

        importApTypes(pkgCtx);

        processRuleSets(pkgCtx);

        List<RulStructuredType> rulStructuredTypes = processStructureTypes(pkgCtx);
        processStructureDefinitions(pkgCtx/*, rulStructuredTypes*/);

        processPartTypes(pkgCtx);

        for (RuleUpdateContext ruc : pkgCtx.getRuleUpdateContexts()) {
            processPolicyTypes(ruc);
            List<RulOutputType> rulOutputTypes = processOutputTypes(ruc);
            List<RulArrangementRule> rulArrangementRuleList = processArrangementRules(ruc);

            List<RulArrangementExtension> rulArrangementExtensions = processArrangementExtensions(ruc);
            List<RulExtensionRule> rulExtensionRuleList = processExtensionRules(ruc, rulArrangementExtensions);

            checkUniqueFilename(rulArrangementRuleList, rulExtensionRuleList, rulOutputTypes);

            StructTypeExtensionUpdater steu = new StructTypeExtensionUpdater(this.structureExtensionRepository,
                    this.structureExtensionDefinitionRepository,
                    this.componentRepository,
                    this.structureService,
                    this);
            steu.run(pkgCtx);
            processPackageActions(ruc);
            processPackageOutputFilters(ruc);
            processPackageExportFilters(ruc);
        }
        // import item types
        processItemTypes(pkgCtx);
        // append all items from repository
        List<RulItemType> rulDescItemTypes = itemTypeRepository.findAll();

        RulPackage rulPackage = pkgCtx.getPackage();
        List<UISettings> uiSettings = new ArrayList<>();
        for (RuleUpdateContext ruc : pkgCtx.getRuleUpdateContexts()) {
            String ruleDirPath = ruc.getKeyDirPath();
            RulRuleSet rulRuleSet = ruc.getRulSet();

            // run phase 2
            ruc.runActionsPhase2();

            Settings settings = pkgCtx.convertXmlStreamToObject(Settings.class, ruleDirPath + SETTING_XML);
            List<UISettings> ruleSettings = createUISettings(settings, rulPackage, rulRuleSet, rulDescItemTypes);
            uiSettings.addAll(ruleSettings);
        }

        deleteRuleSets(pkgCtx);

        // NASTAVENÍ -----------------------------------------------------------------------------------------------

        Settings settings = pkgCtx.convertXmlStreamToObject(Settings.class, SETTING_XML);
        List<UISettings> globalSettings = createUISettings(settings, rulPackage, null, rulDescItemTypes);
        uiSettings.addAll(globalSettings);

        processSettings(uiSettings, rulPackage);

        // END NASTAVENÍ -------------------------------------------------------------------------------------------

        // AP ------------------------------------------------------------------------------------------------------

        ExternalIdTypes externalIdTypes = pkgCtx.convertXmlStreamToObject(ExternalIdTypes.class,
                EXTERNAL_ID_TYPE_XML);
        processExternalIdTypes(externalIdTypes, rulPackage);

        // END AP --------------------------------------------------------------------------------------------------

        IssueTypes issueTypes = pkgCtx.convertXmlStreamToObject(IssueTypes.class, ISSUE_TYPE_XML);
        processIssueTypes(issueTypes, rulPackage);

        IssueStates issueStates = pkgCtx.convertXmlStreamToObject(IssueStates.class, ISSUE_STATE_XML);
        processIssueStates(issueStates, rulPackage);

        asyncRequestService.enqueueAp(accessPoints);

        entityManager.flush();

        staticDataService.reloadOnCommit();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                staticDataService.refreshForCurrentThread();

                cacheService.resetCache(CacheInvalidateEvent.Type.ALL);

                // Try to reload all actions
                // Note: static data have to be reloaded before bulk actions
                //       can be loaded
                bulkActionConfigManager.load();
            }
        });

        eventNotificationService.publishEvent(new ActionEvent(EventType.PACKAGE));

    }

    /**
     * Automatické načítání balíčků v adresáři /dpkg
     *
     * @param path
     */
    @Transactional
    public void autoImportPackages(Path dpkgPath) {
        if (!Files.exists(dpkgPath)) {
            return;
        }
        logger.info("Checking folder {} for packages...", dpkgPath.toString());

        // get current packages from DB
        List<RulPackage> packagesDb = getPackages();
        Map<String, PackageInfoWrapper> latestVersionMap = packagesDb.stream().map(p -> getPackageInfo(p))
                .collect(Collectors.toMap(PackageInfo::getCode, p -> new PackageInfoWrapper(p, null)));


        List<PackageInfoWrapper> packagesToImport = configurationReader.getPackagesToImport();

        // vyhledani poslednich verzi balicku
        for (PackageInfoWrapper infoWrapper : packagesToImport) {
            latestVersionMap.put(infoWrapper.getCode(), infoWrapper);
        }


        // řazení balíčků podle závislostí mezi sebou
        PackageUtils.Graph<String> g = new PackageUtils.Graph<>(latestVersionMap.size());
        latestVersionMap.values().forEach(p -> {
            if (p.getDependencies() != null) {
                p.getDependencies().forEach(d -> g.addEdge(p.getCode(), d.getCode()));
            }
        });
        List<String> sortedPkg = g.topologicalSort();
        logger.debug("Sorted packages: {}", sortedPkg);

        // import balíčku
        for (String codePkg : sortedPkg) {
            PackageInfoWrapper pkgZip = latestVersionMap.get(codePkg);
            // Nenalezen balicek na disku pro dany typ
            if (pkgZip == null) {
                logger.debug("No package with code: {}", codePkg);
                continue;
            }

            Path packagePath = pkgZip.getPath();
            // Kontrola, zda existuje soubor nebo je jiz v DB
            if (packagePath == null) {
                continue;
            }

            logger.info("Reading package from file: {}", pkgZip.getPath().toString());

            try {
                TransactionTemplate transactionTemplate2 = new TransactionTemplate(txManager);
                transactionTemplate2.executeWithoutResult(ts -> {
                    importPackageInternal(pkgZip.getPath().toFile(), false);
                });
            } catch (Exception e) {
                logger.error("Failed to import package file: {}", pkgZip, e);
            }
        }
    }

    /**
     * Získání objektu PackageInfoWrapper ze souboru PACKAGE_XML z archivu
     *
     * @param path
     * @return
     * @throws IOException
     */
    private PackageInfoWrapper getPackageInfo(Path path) throws IOException {
        try (ZipFile zipFile = new ZipFile(path.toFile())) {
            ZipEntry zipEntry = zipFile.getEntry(PACKAGE_XML);
            if (zipEntry == null) {
                // package info not found
                return null;
            }
            try (InputStream is = zipFile.getInputStream(zipEntry)) {
                ByteArrayInputStream bais = new ByteArrayInputStream(IOUtils.toByteArray(is));
                PackageInfo pkgZip = PackageUtils.convertXmlStreamToObject(PackageInfo.class, bais);

                return new PackageInfoWrapper(pkgZip, path);
            }
        }
    }

    private void importApTypes(PackageContext pkgCtx) throws IOException {
        APTypeUpdater apTypeUpdater = new APTypeUpdater(
                apStateRepository,
                apTypeRepository,
                accessPointRepository,
                staticDataService.getData()
        );
        apTypeUpdater.run(pkgCtx);
    }

    private void updateComponentHash(final PackageContext pkgCtx, final RulComponent component, final File dir,
                                     final String zipDir)
            throws IOException {
        String filename = component.getFilename();
        String hash = component.getHash();
        File file = pkgCtx.saveFile(dir, zipDir, filename);
        String newHash = PackageUtils.sha256File(file);
        if (!StringUtils.equalsIgnoreCase(newHash, hash)) {
            component.setHash(newHash);
        }
        componentRepository.save(component);
    }

    /**
     * Provede synchronizaci typů externích identifikátorů.
     *
     * @param externalIdTypes identifikátory v balíčku
     * @param rulPackage      importovaný balíček
     */
    private void processExternalIdTypes(final ExternalIdTypes externalIdTypes, final RulPackage rulPackage) {
        List<ApExternalIdType> apExternalIdTypes = externalIdTypeRepository.findByRulPackage(rulPackage);

        List<ApExternalIdType> apExternalIdTypesNew = new ArrayList<>();

        if (externalIdTypes != null && !CollectionUtils.isEmpty(externalIdTypes.getExternalIdTypes())) {
            for (ExternalIdType externalIdType : externalIdTypes.getExternalIdTypes()) {
                ApExternalIdType apExternalIdType = findEntity(apExternalIdTypes, externalIdType.getCode(), ApExternalIdType::getCode);
                if (apExternalIdType == null) {
                    apExternalIdType = new ApExternalIdType();
                }
                convertApExternalIdTypes(rulPackage, externalIdType, apExternalIdType);
                apExternalIdTypesNew.add(apExternalIdType);
            }
        }

        apExternalIdTypesNew = externalIdTypeRepository.saveAll(apExternalIdTypesNew);

        List<ApExternalIdType> apExternalIdTypesDelete = new ArrayList<>(apExternalIdTypes);
        apExternalIdTypesDelete.removeAll(apExternalIdTypesNew);

        externalIdTypeRepository.deleteAll(apExternalIdTypesDelete);
    }

    /**
     * Konverze VO na DAO.
     *
     * @param rulPackage       importovaný balíček
     * @param externalIdType   VO typ
     * @param apExternalIdType DAO typ
     */
    private void convertApExternalIdTypes(final RulPackage rulPackage,
                                          final ExternalIdType externalIdType,
                                          final ApExternalIdType apExternalIdType) {
        apExternalIdType.setCode(externalIdType.getCode());
        apExternalIdType.setName(externalIdType.getName());
        apExternalIdType.setRulPackage(rulPackage);
    }

    private List<RulStructureDefinition> processStructureDefinitions(final PackageContext puc/*,
                                                                                             final List<RulStructuredType> rulStructureTypes*/) {
        // read current structured types in DB
        List<RulStructuredType> rulStructureTypes = this.structureTypeRepository.findAll();

        StructureDefinitions structureDefinitions = puc.convertXmlStreamToObject(StructureDefinitions.class,
                STRUCTURE_DEFINITION_XML);

        List<RulStructureDefinition> rulStructureDefinitions = rulStructureTypes.size() == 0 ? Collections.emptyList() :
                structureDefinitionRepository.findByRulPackageAndStructuredTypeIn(puc.getPackage(), rulStructureTypes);
        List<RulStructureDefinition> rulStructureDefinitionsNew = new ArrayList<>();

        if (structureDefinitions != null && !CollectionUtils.isEmpty(structureDefinitions.getStructureDefinitions())) {
            for (StructureDefinition structureDefinition : structureDefinitions.getStructureDefinitions()) {

                RulStructureDefinition item = rulStructureDefinitions.stream()
                        .filter((r) -> r.getComponent().getFilename().equals(structureDefinition.getFilename()))
                        .filter((r) -> r.getStructuredType().getCode().equals(structureDefinition.getStructureType()))
                        .findFirst()
                        .orElse(null);

                if (item == null) {
                    item = new RulStructureDefinition();
                }

                convertRulStructureDefinition(puc.getPackage(), structureDefinition, item, rulStructureTypes);

                if (structureDefinition.getCompatibilityRulPackage() != null) {
                    if (puc.getOldPackageVersion() == null ||
                            structureDefinition.getCompatibilityRulPackage() > puc.getOldPackageVersion()) {
                        enqueueAccessPoints(item);
                    }
                }

                rulStructureDefinitionsNew.add(item);
            }
        }

        rulStructureDefinitionsNew = structureDefinitionRepository.saveAll(rulStructureDefinitionsNew);

        List<RulStructureDefinition> rulStructureDefinitionDelete = new ArrayList<>(rulStructureDefinitions);
        rulStructureDefinitionDelete.removeAll(rulStructureDefinitionsNew);

        List<RulComponent> rulComponentsDelete = rulStructureDefinitionDelete.stream().map(RulStructureDefinition::getComponent).collect(Collectors.toList());
        structureDefinitionRepository.deleteAll(rulStructureDefinitionDelete);
        componentRepository.deleteAll(rulComponentsDelete);

        Set<RulStructuredType> revalidateStructureTypes = new HashSet<>();
        try {
            for (RulStructureDefinition definition : rulStructureDefinitionDelete) {
                // if deleted -> has to be revalidated
                if (definition.getDefType() == RulStructureDefinition.DefType.SERIALIZED_VALUE) {
                    revalidateStructureTypes.add(definition.getStructuredType());
                }
            }

            for (RulStructureDefinition definition : rulStructureDefinitionsNew) {
                File file = puc.saveFile(puc.getDir(definition),
                        getZipDir(definition),
                        definition.getComponent().getFilename());
                if (definition.getDefType() == RulStructureDefinition.DefType.SERIALIZED_VALUE) {
                    String newHash = PackageUtils.sha256File(file);
                    String oldHash = definition.getComponent().getHash();
                    if (!StringUtils.equalsIgnoreCase(newHash, oldHash)) {
                        definition.getComponent().setHash(newHash);
                        componentRepository.save(definition.getComponent());
                        revalidateStructureTypes.add(definition.getStructuredType());
                    }
                }
            }
        } catch (IOException e) {
            throw new SystemException(e);
        }

        return rulStructureDefinitionsNew;
    }

    public String getZipDir(final RulStructureDefinition definition) {
        switch (definition.getDefType()) {
            case ATTRIBUTE_TYPES:
                return ZIP_DIR_RULES;
            case PARSE_VALUE:
            case SERIALIZED_VALUE:
                return ZIP_DIR_SCRIPTS;
            default:
                throw new NotImplementedException("Def type: " + definition.getDefType());
        }
    }

    private void convertRulStructureDefinition(final RulPackage rulPackage,
                                               final StructureDefinition structureDefinition,
                                               final RulStructureDefinition item,
                                               final List<RulStructuredType> rulStructureTypes) {
        item.setDefType(structureDefinition.getDefType());
        item.setPriority(structureDefinition.getPriority());
        item.setRulPackage(rulPackage);
        item.setStructuredType(rulStructureTypes.stream()
                .filter(x -> x.getCode().equals(structureDefinition.getStructureType()))
                .findFirst()
                .orElse(null));
        item.setCompatibilityRulPackage(structureDefinition.getCompatibilityRulPackage());

        String filename = structureDefinition.getFilename();
        if (filename != null) {
            RulComponent component = item.getComponent();
            if (component == null) {
                component = new RulComponent();
            }
            component.setFilename(filename);
            componentRepository.save(component);
            item.setComponent(component);
        } else {
            RulComponent component = item.getComponent();
            item.setComponent(null);
            if (component != null) {
                structureDefinitionRepository.save(item);
                componentRepository.delete(component);
            }
        }
    }

    private List<RulStructuredType> processStructureTypes(final PackageContext puc) {
        // read from XML
        StructureTypes structureTypes = PackageUtils.convertXmlStreamToObject(StructureTypes.class,
                puc.getByteStream(STRUCTURE_TYPE_XML));

        // get current types
        List<RulStructuredType> currStructTypes = structureTypeRepository.findByRulPackage(puc.getPackage());
        List<RulStructuredType> newStructTypes = new ArrayList<>();

        if (structureTypes != null && !CollectionUtils.isEmpty(structureTypes.getStructureTypes())) {
            for (StructureType structureType : structureTypes.getStructureTypes()) {
                // find existing or create new type
                RulStructuredType item = currStructTypes.stream().filter(
                        (r) -> r.getCode()
                                .equals(structureType.getCode()))
                        .findFirst()
                        .orElse(new RulStructuredType());

                convertRulStructureType(puc.getPackage(), structureType, item);
                newStructTypes.add(item);
                // check compatibility
                if (structureType.getValidValueFromVersion() != null) {
                    int validFromVersion = Integer.parseInt(structureType.getValidValueFromVersion());
                    if (puc.getOldPackageVersion() != null
                            && validFromVersion > puc.getOldPackageVersion().intValue()) {
                        puc.addRegenerateStructureType(structureType.getCode());
                    }
                }
            }
        }

        newStructTypes = structureTypeRepository.saveAll(newStructTypes);

        List<RulStructuredType> rulRuleDelete = new ArrayList<>(currStructTypes);
        rulRuleDelete.removeAll(newStructTypes);
        structureTypeRepository.deleteAll(rulRuleDelete);

        currStructTypes.addAll(newStructTypes);
        puc.setStructureTypes(currStructTypes);
        return currStructTypes;
    }

    private void convertRulStructureType(final RulPackage rulPackage,
                                         final StructureType structureType,
                                         final RulStructuredType item) {
        item.setRulPackage(rulPackage);
        item.setCode(structureType.getCode());
        item.setName(structureType.getName());
        item.setAnonymous(structureType.getAnonymous());
    }

    private void processPartTypes(final PackageContext packageContext) {
        // read from XML
        PartTypes partTypes = PackageUtils.convertXmlStreamToObject(PartTypes.class,
                packageContext.getByteStream(PART_TYPE_XML));

        // get current types
        List<RulPartType> currPartTypes = partTypeRepository.findByRulPackage(packageContext.getPackage());
        List<RulPartType> newPartTypes = new ArrayList<>();

        if (partTypes != null && !CollectionUtils.isEmpty(partTypes.getPartTypes())) {
            for (PartType partType : partTypes.getPartTypes()) {
                // find existing or create new type
                RulPartType item = currPartTypes.stream().filter(
                        (r) -> r.getCode().equals(partType.getCode()))
                        .findFirst()
                        .orElse(new RulPartType());

                convertRulPartType(packageContext.getPackage(), partType, item);
                newPartTypes.add(item);
            }
            processPartTypesChildPart(partTypes.getPartTypes(), newPartTypes);
        }

        newPartTypes = partTypeRepository.saveAll(newPartTypes);

        List<RulPartType> rulRuleDelete = new ArrayList<>(currPartTypes);
        rulRuleDelete.removeAll(newPartTypes);
        partTypeRepository.deleteAll(rulRuleDelete);
    }

    private void processPartTypesChildPart(List<PartType> partTypes, List<RulPartType> newPartTypes) {
        if (CollectionUtils.isNotEmpty(partTypes) && CollectionUtils.isNotEmpty(newPartTypes)) {
            for (PartType partType : partTypes) {
                if (StringUtils.isNotEmpty(partType.getChildPart())) {
                    RulPartType rulPartType = findRulPartTypeByCode(newPartTypes, partType.getCode());
                    if (rulPartType == null) {
                        throw new IllegalStateException("Nenalezen typ části s kódem: " + partType.getCode());
                    }

                    RulPartType childPartType = findRulPartTypeByCode(newPartTypes, partType.getChildPart());
                    if (childPartType == null) {
                        throw new IllegalStateException("Nenalezen typ podřízené části s kódem: " + partType.getChildPart());
                    }

                    rulPartType.setChildPart(childPartType);
                }
            }
        }
    }

    @Nullable
    private RulPartType findRulPartTypeByCode(final List<RulPartType> rulPartTypes, final String code) {
        if (CollectionUtils.isNotEmpty(rulPartTypes)) {
            for (RulPartType rulPartType : rulPartTypes) {
                if (rulPartType.getCode().equals(code)) {
                    return rulPartType;
                }
            }
        }
        return null;
    }

    private void convertRulPartType(final RulPackage rulPackage,
                                    final PartType partType,
                                    final RulPartType item) {
        item.setRulPackage(rulPackage);
        item.setCode(partType.getCode());
        item.setName(partType.getName());
        item.setRepeatable(partType.getRepeatable() == null || partType.getRepeatable());
    }

    /**
     * Kontroluje, že v rámci balíčů a pravidel neexistuje odkaz na identický soubor.
     *
     * @param rulArrangementRuleList základní pravidla
     * @param rulExtensionRuleList   řídící pravidla
     * @param rulOutputTypes         typy výstupů
     */
    private void checkUniqueFilename(final List<RulArrangementRule> rulArrangementRuleList,
                                     final List<RulExtensionRule> rulExtensionRuleList,
                                     final List<RulOutputType> rulOutputTypes) {
        Set<String> exists = new HashSet<>();
        for (RulArrangementRule rulArrangementRule : rulArrangementRuleList) {
            String filename = rulArrangementRule.getComponent().getFilename().toLowerCase();
            if (exists.contains(filename)) {
                throw new IllegalStateException("Duplicitní reference na název souboru pravidel: " + filename);
            }
            exists.add(filename);
        }
        for (RulExtensionRule rulExtensionRule : rulExtensionRuleList) {
            String filename = rulExtensionRule.getComponent().getFilename().toLowerCase();
            if (exists.contains(filename)) {
                throw new IllegalStateException("Duplicitní reference na název souboru pravidel: " + filename);
            }
            exists.add(filename);
        }
        for (RulOutputType rulOutputType : rulOutputTypes) {
            if (rulOutputType.getComponent() != null) {
                String filename = rulOutputType.getComponent().getFilename().toLowerCase();
                if (exists.contains(filename)) {
                    throw new IllegalStateException("Duplicitní reference na název souboru pravidel: " + filename);
                }
                exists.add(filename);
            }
        }
    }

    /**
     * Zpracování nastavení.
     *
     * @param newSettings not-null
     * @param rulPackage  not-null
     */
    private void processSettings(final List<UISettings> newSettings, final RulPackage rulPackage) {
        Validate.notNull(newSettings);
        Validate.notNull(rulPackage);

        List<UISettings> allSettings = settingsRepository.findAll();

        List<UISettings> currSettings = new LinkedList<>();
        List<UISettings> otherSettings = new ArrayList<>();

        for (UISettings sett : allSettings) {
            if (rulPackage.getPackageId().equals(sett.getPackageId())) {
                currSettings.add(sett);
            } else {
                otherSettings.add(sett);
            }
        }

        for (UISettings sett : newSettings) {
            // find same settings in other packages (throws exception when found)
            for (UISettings otherSett : otherSettings) {
                if (sett.isSameSettings(otherSett)) {
                    throw new SystemException("Settings already exists", PackageCode.OTHER_PACKAGE)
                            .set("UISettingsId", otherSett.getSettingsId())
                            .set("settingsType", otherSett.getSettingsType());
                }
            }

            // find same settings in this package (update current when found)
            for (Iterator<UISettings> it = currSettings.iterator(); it.hasNext(); ) {
                UISettings currSett = it.next();
                if (sett.isSameSettings(currSett)) {

                    // update current settings
                    sett.setSettingsId(currSett.getSettingsId());

                    it.remove();
                    break;
                }
            }

            settingsRepository.save(sett);
        }

        settingsRepository.deleteAll(currSettings);
    }

    private List<UISettings> createUISettings(final Settings settings,
                                              final RulPackage rulPackage,
                                              final RulRuleSet ruleSet,
                                              final List<RulItemType> rulItemTypes) {
        if (settings == null) {
            return Collections.emptyList();
        }
        Validate.notNull(settings.getSettings());

        List<UISettings> result = new ArrayList<>(settings.getSettings().size());

        for (Setting sett : settings.getSettings()) {
            UISettings uiSett = sett.createUISettings(rulPackage);

            Integer entityId = null;
            if (uiSett.getEntityType() == EntityType.RULE) {
                Validate.notNull(ruleSet, "Ruleset is null for settings: %1$s", sett);

                entityId = ruleSet.getRuleSetId();
            } else if (uiSett.getEntityType() == EntityType.ITEM_TYPE) {
                SettingFavoriteItemSpecs specs = (SettingFavoriteItemSpecs) sett;
                String specsCode = specs.getCode();

                RulItemType itemType = rulItemTypes.stream()
                        .filter(t -> t.getCode().equals(specsCode))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException("RulItemType s code=" + specsCode + " nenalezen", PackageCode.CODE_NOT_FOUND)
                                .set("code", specsCode).set("file", SETTING_XML));

                entityId = itemType.getItemTypeId();
            }

            uiSett.setEntityId(entityId);

            result.add(uiSett);
        }

        return result;
    }

    /**
     * Provede synchronizaci typů připomínek.
     *
     * @param issueTypes typy připomínek
     * @param rulPackage importovaný balíček
     */
    private void processIssueTypes(IssueTypes issueTypes, final RulPackage rulPackage) {

        List<WfIssueType> wfIssueTypes = issueTypeRepository.findByRulPackage(rulPackage);

        List<WfIssueType> wfIssueTypesNew = new ArrayList<>();

        if (issueTypes != null) {
            for (IssueType issueType : issueTypes.getIssueTypes()) {
                WfIssueType wfIssueType = findEntity(wfIssueTypes, issueType.getCode(), WfIssueType::getCode);
                if (wfIssueType == null) {
                    wfIssueType = new WfIssueType();
                }
                wfIssueType.setCode(issueType.getCode());
                wfIssueType.setName(issueType.getName());
                wfIssueType.setViewOrder(issueType.getViewOrder());
                wfIssueType.setRulPackage(rulPackage);
                wfIssueTypesNew.add(wfIssueType);
            }
        }

        wfIssueTypesNew = issueTypeRepository.saveAll(wfIssueTypesNew);

        List<WfIssueType> WfIssueTypeDelete = new ArrayList<>(wfIssueTypes);
        WfIssueTypeDelete.removeAll(wfIssueTypesNew);

        issueTypeRepository.deleteAll(WfIssueTypeDelete);
    }

    /**
     * Provede synchronizaci stavů připomínek.
     *
     * @param issueStates stavy připomínek
     * @param rulPackage  importovaný balíček
     */
    private void processIssueStates(IssueStates issueStates, final RulPackage rulPackage) {

        List<WfIssueState> wfIssueStates = issueStateRepository.findByRulPackage(rulPackage);

        List<WfIssueState> wfIssueStatesNew = new ArrayList<>();

        if (issueStates != null) {
            for (IssueState issueState : issueStates.getIssueStates()) {
                WfIssueState wfIssueState = findEntity(wfIssueStates, issueState.getCode(), WfIssueState::getCode);
                if (wfIssueState == null) {
                    wfIssueState = new WfIssueState();
                }
                wfIssueState.setCode(issueState.getCode());
                wfIssueState.setName(issueState.getName());
                wfIssueState.setStartState(issueState.isStartState());
                wfIssueState.setFinalState(issueState.isFinalState());
                wfIssueState.setRulPackage(rulPackage);
                wfIssueStatesNew.add(wfIssueState);
            }
        }

        wfIssueStatesNew = issueStateRepository.saveAll(wfIssueStatesNew);

        List<WfIssueState> WfIssueStateDelete = new ArrayList<>(wfIssueStates);
        WfIssueStateDelete.removeAll(wfIssueStatesNew);

        issueStateRepository.deleteAll(WfIssueStateDelete);
    }

    /**
     * Generická metoda pro vyhledání v seznamu entit podle definované metody.
     *
     * @param list     seznam prohledávaných entit
     * @param find     co hledán v entitě
     * @param function metoda, jakou hledám v entitě
     * @param <T>
     * @param <S>
     * @return nalezená entita
     */
    static <T, S> T findEntity(@NotNull final Collection<T> list,
                               @NotNull final S find,
                               @NotNull final Function<T, S> function) {
        for (T item : list) {
            if (Objects.equals(function.apply(item), find)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Generická metoda pro vyhledání v seznamu entit podle definované metody.
     *
     * @param list      seznam prohledávaných entit
     * @param findA     co hledán v entitě - první podmínka
     * @param findB     co hledán v entitě - druhá podmínka
     * @param functionA metoda, jakou hledám v entitě - první
     * @param functionB metoda, jakou hledám v entitě - druhá
     * @param <T>
     * @param <S1>
     * @param <S2>
     * @return nalezená entita
     */
    private <T, S1, S2> T findEntity(@NotNull final Collection<T> list,
                                     @NotNull final S1 findA,
                                     @NotNull final S2 findB,
                                     @NotNull final Function<T, S1> functionA,
                                     @NotNull final Function<T, S2> functionB) {
        for (T item : list) {
            if (Objects.equals(functionA.apply(item), findA)
                    && Objects.equals(functionB.apply(item), findB)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Zpracování policy.
     *
     * @param ruc rule update context
     */
    private void processPolicyTypes(RuleUpdateContext ruc) {
        PolicyTypes policyTypes = ruc.convertXmlStreamToObject(PolicyTypes.class,
                POLICY_TYPE_XML);
        final RulPackage rulPackage = ruc.getRulPackage();
        final RulRuleSet rulRuleSet = ruc.getRulSet();

        List<RulPolicyType> rulPolicyTypesTypes = policyTypeRepository.findByRulPackage(rulPackage);
        List<RulPolicyType> rulPolicyTypesNew = new ArrayList<>();

        if (policyTypes != null && !CollectionUtils.isEmpty(policyTypes.getPolicyTypes())) {
            for (PolicyType policyType : policyTypes.getPolicyTypes()) {
                List<RulPolicyType> findItems = rulPolicyTypesTypes.stream().filter(
                        (r) -> r.getCode().equals(policyType.getCode())).collect(
                        Collectors.toList());
                RulPolicyType item;
                if (findItems.size() > 0) {
                    item = findItems.get(0);
                } else {
                    item = new RulPolicyType();
                }

                convertRulPolicyTypes(rulPackage, policyType, item, rulRuleSet);
                rulPolicyTypesNew.add(item);
            }
        }

        rulPolicyTypesNew = policyTypeRepository.saveAll(rulPolicyTypesNew);

        List<RulPolicyType> rulPolicyTypesDelete = new ArrayList<>(rulPolicyTypesNew);
        rulPolicyTypesDelete.removeAll(rulPolicyTypesNew);
        policyTypeRepository.deleteAll(rulPolicyTypesDelete);
    }

    /**
     * Převod VO na DAO policy.
     *
     * @param rulPackage    balíček
     * @param policyType    VO policy
     * @param rulPolicyType DAO policy
     * @param rulRuleSet    pravidlo
     */
    private void convertRulPolicyTypes(final RulPackage rulPackage,
                                       final PolicyType policyType,
                                       final RulPolicyType rulPolicyType,
                                       final RulRuleSet rulRuleSet) {
        rulPolicyType.setCode(policyType.getCode());
        rulPolicyType.setName(policyType.getName());
        rulPolicyType.setRulPackage(rulPackage);
        rulPolicyType.setRuleSet(rulRuleSet);
    }

    /**
     * Převod VO na DAO packet.
     *
     * @param rulPackage    balíček
     * @param outputType    VO packet
     * @param rulOutputType DAO packet
     * @param rulRuleSet    pravidla
     */
    private void convertRulOutputType(final RulPackage rulPackage,
                                      final OutputType outputType,
                                      final RulOutputType rulOutputType,
                                      final RulRuleSet rulRuleSet) {
        rulOutputType.setPackage(rulPackage);
        rulOutputType.setCode(outputType.getCode());
        rulOutputType.setName(outputType.getName());
        rulOutputType.setRuleSet(rulRuleSet);

        String filename = outputType.getFilename();
        if (filename != null) {
            RulComponent component = rulOutputType.getComponent();
            if (component == null) {
                component = new RulComponent();
            }
            component.setFilename(filename);
            componentRepository.save(component);
            rulOutputType.setComponent(component);
        } else {
            RulComponent component = rulOutputType.getComponent();
            rulOutputType.setComponent(null);
            if (component != null) {
                outputTypeRepository.save(rulOutputType);
                componentRepository.delete(component);
            }
        }
    }

    /**
     * Zpracování řídících pravidel.
     *
     * @param ruc importovaných seznam pravidel
     * @return seznam pravidel
     */
    private List<RulArrangementRule> processArrangementRules(final RuleUpdateContext ruc) {
        ArrangementRules packageRules = ruc
                .convertXmlStreamToObject(ArrangementRules.class, ARRANGEMENT_RULE_XML);

        List<RulArrangementRule> rulPackageRules = arrangementRuleRepository.findByRulPackageAndRuleSet(ruc.getRulPackage(),
                ruc.getRulSet());
        List<RulArrangementRule> rulRuleNew = new ArrayList<>();

        if (packageRules != null && !CollectionUtils.isEmpty(packageRules.getArrangementRules())) {
            for (ArrangementRule packageRule : packageRules.getArrangementRules()) {
                List<RulArrangementRule> findItems = rulPackageRules.stream().filter(
                        (r) -> r.getComponent().getFilename().equals(packageRule.getFilename())).collect(
                        Collectors.toList());
                RulArrangementRule item;
                if (findItems.size() > 0) {
                    item = findItems.get(0);
                } else {
                    item = new RulArrangementRule();
                }

                convertRulArrangementRule(ruc.getRulPackage(), packageRule, item, ruc.getRulSet());
                rulRuleNew.add(item);
            }
        }

        rulRuleNew = arrangementRuleRepository.saveAll(rulRuleNew);

        List<RulArrangementRule> rulRuleDelete = new ArrayList<>(rulPackageRules);
        rulRuleDelete.removeAll(rulRuleNew);
        List<RulComponent> rulComponentsDelete = rulRuleDelete.stream().map(RulArrangementRule::getComponent).collect(Collectors.toList());
        arrangementRuleRepository.deleteAll(rulRuleDelete);
        componentRepository.deleteAll(rulComponentsDelete);

        try {
            for (RulArrangementRule rule : rulRuleNew) {
                ruc.getPackageUpdateContext().saveFile(ruc.getRulesDir(),
                        ZIP_DIR_RULE_SET + "/" + ruc.getRulSetCode() + "/" + ZIP_DIR_RULES,
                        rule.getComponent().getFilename());
            }
        } catch (IOException e) {
            throw new SystemException(e);
        }

        return rulRuleNew;

    }

    /**
     * Zpracování definice rozšíření pro řídící pravidla popisu.
     *
     * @param ruc importované definice rozšíření
     * @return seznam definicí
     */
    private List<RulArrangementExtension> processArrangementExtensions(RuleUpdateContext ruc) {
        ArrangementExtensions arrangementExtensions = ruc
                .convertXmlStreamToObject(ArrangementExtensions.class, ARRANGEMENT_EXTENSION_XML);
        RulRuleSet rulRuleSet = ruc.getRulSet();
        RulPackage rulPackage = ruc.getRulPackage();

        List<RulArrangementExtension> rulArrangementExtensions = arrangementExtensionRepository.findByRulPackageAndRuleSet(rulPackage, rulRuleSet);
        List<RulArrangementExtension> rulArrangementExtensionsNew = new ArrayList<>();

        if (arrangementExtensions != null && !CollectionUtils.isEmpty(arrangementExtensions.getArrangementExtensions())) {
            for (ArrangementExtension arrangementExtension : arrangementExtensions.getArrangementExtensions()) {
                RulArrangementExtension item = rulArrangementExtensions.stream()
                        .filter((r) -> r.getCode().equals(arrangementExtension.getCode()))
                        .findFirst()
                        .orElse(null);

                if (item == null) {
                    item = new RulArrangementExtension();
                }

                convertRulArrangementExtension(rulPackage, arrangementExtension, item, rulRuleSet);
                rulArrangementExtensionsNew.add(item);
            }
        }

        rulArrangementExtensionsNew = arrangementExtensionRepository.saveAll(rulArrangementExtensionsNew);

        List<RulArrangementExtension> rulArrangementExtensionDelete = new ArrayList<>(rulArrangementExtensions);
        rulArrangementExtensionDelete.removeAll(rulArrangementExtensionsNew);
        arrangementExtensionRepository.deleteAll(rulArrangementExtensionDelete);

        return rulArrangementExtensionsNew;
    }

    /**
     * Zpracování řídících pravidel archivního popisu, které definují dané rozšíření.
     *
     * @param ruc                      importované řídící pravidla
     * @param rulArrangementExtensions definice rozšíření
     */
    private List<RulExtensionRule> processExtensionRules(final RuleUpdateContext ruc,
                                                         final List<RulArrangementExtension> rulArrangementExtensions) {
        ExtensionRules extensionRules = ruc.convertXmlStreamToObject(ExtensionRules.class, EXTENSION_RULE_XML);

        List<RulExtensionRule> rulExtensionRules = rulArrangementExtensions.size() == 0 ? Collections.emptyList() :
                extensionRuleRepository.findByRulPackageAndArrangementExtensionIn(ruc.getRulPackage(),
                        rulArrangementExtensions);
        List<RulExtensionRule> rulExtensionRulesNew = new ArrayList<>();

        if (extensionRules != null && !CollectionUtils.isEmpty(extensionRules.getExtensionRules())) {
            for (ExtensionRule extensionRule : extensionRules.getExtensionRules()) {

                RulExtensionRule item = rulExtensionRules.stream()
                        .filter((r) -> r.getComponent().getFilename().equals(extensionRule.getFilename()))
                        .filter((r) -> r.getArrangementExtension().getCode().equals(extensionRule.getArrangementExtension()))
                        .findFirst()
                        .orElse(null);

                if (item == null) {
                    item = new RulExtensionRule();
                }

                convertRulExtensionRule(ruc.getRulPackage(), extensionRule, item, rulArrangementExtensions);

                if (extensionRule.getCompatibilityRulPackage() != null) {
                    if (ruc.getPackageUpdateContext().getOldPackageVersion() == null ||
                            extensionRule.getCompatibilityRulPackage() > ruc.getPackageUpdateContext().getOldPackageVersion()) {
                        enqueueAccessPoints(item);
                    }
                }

                rulExtensionRulesNew.add(item);
            }
        }

        rulExtensionRulesNew = extensionRuleRepository.saveAll(rulExtensionRulesNew);

        List<RulExtensionRule> rulExtensionRulesDelete = new ArrayList<>(rulExtensionRules);
        rulExtensionRulesDelete.removeAll(rulExtensionRulesNew);

        List<RulComponent> rulComponentsDelete = rulExtensionRulesDelete.stream().map(RulExtensionRule::getComponent).collect(Collectors.toList());
        extensionRuleRepository.deleteAll(rulExtensionRulesDelete);
        componentRepository.deleteAll(rulComponentsDelete);

        try {
            for (RulExtensionRule rule : rulExtensionRulesNew) {
                ruc.getPackageUpdateContext().saveFile(ruc.getRulesDir(),
                        ZIP_DIR_RULE_SET + "/" + ruc.getRulSetCode() + "/" + ZIP_DIR_RULES,
                        rule.getComponent().getFilename());
            }
        } catch (IOException e) {
            throw new SystemException(e);
        }

        return rulExtensionRulesNew;
    }

    private void convertRulExtensionRule(final RulPackage rulPackage,
                                         final ExtensionRule extensionRule,
                                         final RulExtensionRule rulExtensionRule,
                                         final List<RulArrangementExtension> rulArrangementExtensions) {
        rulExtensionRule.setPackage(rulPackage);
        rulExtensionRule.setRuleType(extensionRule.getRuleType());
        rulExtensionRule.setPriority(extensionRule.getPriority());
        rulExtensionRule.setArrangementExtension(rulArrangementExtensions.stream()
                .filter(x -> x.getCode().equals(extensionRule.getArrangementExtension()))
                .findFirst()
                .orElse(null));
        rulExtensionRule.setCompatibilityRulPackage(extensionRule.getCompatibilityRulPackage());
        rulExtensionRule.setCondition(extensionRule.getCondition());

        String filename = extensionRule.getFilename();
        if (filename != null) {
            RulComponent component = rulExtensionRule.getComponent();
            if (component == null) {
                component = new RulComponent();
            }
            component.setFilename(filename);
            componentRepository.save(component);
            rulExtensionRule.setComponent(component);
        } else {
            RulComponent component = rulExtensionRule.getComponent();
            rulExtensionRule.setComponent(null);
            if (component != null) {
                extensionRuleRepository.save(rulExtensionRule);
                componentRepository.delete(component);
            }
        }
    }


    /**
     * Převod VO na DAO pravidla.
     *
     * @param rulPackage         balíček
     * @param arrangementRule    VO pravidla
     * @param rulArrangementRule DAO pravidla
     * @param rulRuleSet         pravidlo
     */
    private void convertRulArrangementRule(final RulPackage rulPackage,
                                           final ArrangementRule arrangementRule,
                                           final RulArrangementRule rulArrangementRule,
                                           final RulRuleSet rulRuleSet) {

        rulArrangementRule.setPackage(rulPackage);
        rulArrangementRule.setPriority(arrangementRule.getPriority());
        rulArrangementRule.setRuleType(arrangementRule.getRuleType());
        rulArrangementRule.setRuleSet(rulRuleSet);

        String filename = arrangementRule.getFilename();
        if (filename != null) {
            RulComponent component = rulArrangementRule.getComponent();
            if (component == null) {
                component = new RulComponent();
            }
            component.setFilename(filename);
            componentRepository.save(component);
            rulArrangementRule.setComponent(component);
        } else {
            RulComponent component = rulArrangementRule.getComponent();
            rulArrangementRule.setComponent(null);
            if (component != null) {
                arrangementRuleRepository.save(rulArrangementRule);
                componentRepository.delete(component);
            }
        }
    }

    /**
     * Převod VO na DAO definice rozšíření.
     *
     * @param rulPackage              balíček
     * @param arrangementExtension    VO definice rozšíření
     * @param rulArrangementExtension DAO definice rozšíření
     * @param rulRuleSet              pravidlo
     */
    private void convertRulArrangementExtension(final RulPackage rulPackage,
                                                final ArrangementExtension arrangementExtension,
                                                final RulArrangementExtension rulArrangementExtension,
                                                final RulRuleSet rulRuleSet) {
        rulArrangementExtension.setCode(arrangementExtension.getCode());
        rulArrangementExtension.setName(arrangementExtension.getName());
        rulArrangementExtension.setRulPackage(rulPackage);
        rulArrangementExtension.setRuleSet(rulRuleSet);
    }

    private void processPackageActions(RuleUpdateContext ruc) {
        logger.info("Processing package actions, code: {}", ruc.getRulSetCode());

        RulPackage rulPackage = ruc.getRulPackage();

        ActionsXml actionsXml = ruc
                .convertXmlStreamToObject(ActionsXml.class, PACKAGE_ACTIONS_XML);

        List<RulAction> dbActions = packageActionsRepository.findByRulPackage(rulPackage);
        List<RulAction> rulPackageActionsNew = new ArrayList<>();

        if (actionsXml != null && !CollectionUtils.isEmpty(actionsXml.getPackageActions())) {
            // procházím všechny definice akcí z pabíčku
            for (ActionXml packageAction : actionsXml.getPackageActions()) {

                //vyhledám akci podle záznamů v DB, pokud existuje
                Optional<RulAction> findItems = dbActions.stream().filter(
                        (r) -> r.getFilename().equals(packageAction
                                .getFilename())).findFirst();
                // pokud existuje v DB, vyhledám návazné typy atributů a doporučené akce,
                // jinak založím prázdné seznamy
                RulAction dbAction = findItems.orElseGet(
                        () -> new RulAction()
                );

                // vytvořím/úpravím a uložím akci
                convertRulPackageAction(rulPackage, packageAction, dbAction, ruc.getRulSet());
                packageActionsRepository.save(dbAction);

                processActionItemTypes(ruc, packageAction.getActionItemTypes(), dbAction);

                processRecommendedActions(ruc, packageAction.getActionRecommendeds(), dbAction);

                rulPackageActionsNew.add(dbAction);
            }
        }

        // uložení nově vytvořených hromadných akcí
        rulPackageActionsNew = packageActionsRepository.saveAll(rulPackageActionsNew);

        // smazání nedefinovaných hromadných akcí včetně vazeb
        List<RulAction> rulPackageActionsDelete = new ArrayList<>(dbActions);
        rulPackageActionsDelete.removeAll(rulPackageActionsNew);

        for (RulAction rulAction : rulPackageActionsDelete) {
            itemTypeActionRepository.deleteByAction(rulAction);
            actionRecommendedRepository.deleteByAction(rulAction);
        }
        packageActionsRepository.deleteAll(rulPackageActionsDelete);

        // odstranění/vytvoření definičních souborů pro hromadné akce
        try {
            for (RulAction action : rulPackageActionsNew) {
                ruc.getPackageUpdateContext().saveFile(ruc.getActionsDir(),
                        ZIP_DIR_RULE_SET + "/" + ruc.getRulSetCode() + "/" + ZIP_DIR_ACTIONS, action.getFilename());
            }

        } catch (IOException e) {
            throw new SystemException(e);
        }
    }

    private void processRecommendedActions(RuleUpdateContext ruc, List<ActionRecommended> actionRecommendeds,
                                           RulAction dbAction) {
        List<RulActionRecommended> rulActionRecommendeds = actionRecommendedRepository.findByAction(dbAction);

        List<RulActionRecommended> rulActionRecomendedsNew = new ArrayList<>();
        if (!CollectionUtils.isEmpty(actionRecommendeds)) {
            // pokud existují v balíčku u akce typy výstupů, pro které jsou akce doporučené,
            // je potřeba je dohledat pokud existují v DB a následně upravit, nebo přidat/smaza
            for (ActionRecommended actionRecommended : actionRecommendeds) {
                RulActionRecommended rulActionRecommended = actionRecommendedRepository
                        .findOneByOutputTypeCodeAndAction(actionRecommended.getOutputType(), dbAction);
                RulOutputType rulOutputType = outputTypeRepository.findOneByCode(actionRecommended.getOutputType());

                if (rulOutputType == null) {
                    throw new BusinessException("RulOutputType s code=" + actionRecommended.getOutputType()
                            + " nenalezen", PackageCode.CODE_NOT_FOUND).set("code", actionRecommended.getOutputType())
                            .set("file", OUTPUT_TYPE_XML);
                }

                // pokud vazba na doporučenou akci ještě neexistuje v DB
                if (rulActionRecommended == null) {
                    rulActionRecommended = new RulActionRecommended();
                }

                rulActionRecommended.setOutputType(rulOutputType);
                rulActionRecommended.setAction(dbAction);
                rulActionRecomendedsNew.add(rulActionRecommended);
            }

            // uložení seznamu upravených/přidaných vazeb na doporučené akce
            actionRecommendedRepository.saveAll(rulActionRecomendedsNew);

        }
        // vyhkedat a odstranit již nenavázané doporučené akce
        List<RulActionRecommended> rulActionRecommendedsDelete = new ArrayList<>(rulActionRecommendeds);
        rulActionRecommendedsDelete.removeAll(rulActionRecomendedsNew);
        actionRecommendedRepository.deleteAll(rulActionRecommendedsDelete);
    }

    private void processActionItemTypes(RuleUpdateContext ruc, List<ActionItemType> xmlActionItemTypes,
                                        RulAction dbAction) {
        logger.info("Processing action related item types, action: {}", dbAction.getCode());
        // get current actions
        List<RulItemTypeAction> rulTypeActions = itemTypeActionRepository.findByAction(dbAction);

        List<RulItemTypeAction> rulTypeActionsNew = new ArrayList<>();
        if (!CollectionUtils.isEmpty(xmlActionItemTypes)) {
            // pokud existují v balíčku u akce typy atributů, které se počítají,
            // je potřeba je dohledat pokud existují v DB a následně upravit,
            // nebo přidat/smazat

            for (ActionItemType actionItemType : xmlActionItemTypes) {
                // Kontrola existence item type
                RulItemTypeAction rulItemTypeAction = null;
                RulItemType rulItemType = itemTypeRepository.findOneByCode(actionItemType.getItemType());
                if (rulItemType != null) {
                    rulItemTypeAction = itemTypeActionRepository
                            .findOneByItemTypeCodeAndAction(actionItemType.getItemType(),
                                    dbAction);
                }
                if (rulItemTypeAction == null) {
                    // if item not found -> has to be created in phase 2
                    StoreItemTypeAction stia = new StoreItemTypeAction(itemTypeRepository,
                            itemTypeActionRepository,
                            actionItemType.getItemType(),
                            dbAction);
                    ruc.addActionPhase2(stia);
                } else {
                    // do not drop ok items
                    rulTypeActionsNew.add(rulItemTypeAction);
                }

            }
        }
        // vyhledat a odstranit již nenavázané typy atributů z DB
        List<RulItemTypeAction> rulTypeActionsDelete = new ArrayList<>(rulTypeActions);
        rulTypeActionsDelete.removeAll(rulTypeActionsNew);
        if (rulTypeActionsDelete.size() > 0) {
            logger.info("Deleting action related item types, count: {}", rulTypeActionsDelete.size());
            // deleting actions
            itemTypeActionRepository.deleteAll(rulTypeActionsDelete);
        }
    }

    private void processPackageOutputFilters(RuleUpdateContext ruc) {
        logger.info("Processing package output filters, code: {}", ruc.getRulSetCode());

        RulPackage rulPackage = ruc.getRulPackage();

        OutputFiltersXml outputFiltersXml = ruc
                .convertXmlStreamToObject(OutputFiltersXml.class, PACKAGE_OUTPUT_FILTERS_XML);

        List<RulOutputFilter> dbOutputFilters = outputFilterRepository.findByRulPackage(rulPackage);
        List<RulOutputFilter> rulPackageOutputFiltersNew = new ArrayList<>();

        if (outputFiltersXml != null && !CollectionUtils.isEmpty(outputFiltersXml.getPackageOutputFilters())) {
            // procházím všechny definice výstupních filtrů
            for (OutputFilterXml packageOutputFilter : outputFiltersXml.getPackageOutputFilters()) {

                //vyhledám výstupní filtr podle záznamů v DB, pokud existuje
                Optional<RulOutputFilter> findItems = dbOutputFilters.stream().filter(
                        (r) -> r.getCode().equals(packageOutputFilter
                                .getCode())).findFirst();
                // jinak založím prázdné seznamy
                RulOutputFilter dbOutputFilter = findItems.orElseGet(
                        RulOutputFilter::new
                );

                // vytvořím/úpravím a uložím
                convertRulPackageOutputFilter(rulPackage, packageOutputFilter, dbOutputFilter, ruc.getRulSet());

                rulPackageOutputFiltersNew.add(dbOutputFilter);
            }
        }

        // uložení nově vytvořených výstupních filtrů
        rulPackageOutputFiltersNew = outputFilterRepository.saveAll(rulPackageOutputFiltersNew);

        // smazání nedefinovaných výstupních filtrů
        List<RulOutputFilter> rulPackageOutputFiltersDelete = new ArrayList<>(dbOutputFilters);
        rulPackageOutputFiltersDelete.removeAll(rulPackageOutputFiltersNew);
        outputFilterRepository.deleteAll(rulPackageOutputFiltersDelete);

        // odstranění/vytvoření definičních souborů pro výstupní filtry
        try {
            for (RulOutputFilter outputFilter : rulPackageOutputFiltersNew) {
                ruc.getPackageUpdateContext().saveFile(ruc.getOutputFiltersDir(),
                        ZIP_DIR_RULE_SET + "/" + ruc.getRulSetCode() + "/" + ZIP_DIR_OUTPUT_FILTERS, outputFilter.getFilename());
            }

        } catch (IOException e) {
            throw new SystemException(e);
        }
    }

    private void processPackageExportFilters(RuleUpdateContext ruc) {
        logger.info("Processing package export filters, code: {}", ruc.getRulSetCode());

        RulPackage rulPackage = ruc.getRulPackage();

        ExportFiltersXml exportFiltersXml = ruc
                .convertXmlStreamToObject(ExportFiltersXml.class, PACKAGE_EXPORT_FILTERS_XML);

        List<RulExportFilter> dbExportFilters = exportFilterRepository.findByRulPackage(rulPackage);
        List<RulExportFilter> rulPackageExportFiltersNew = new ArrayList<>();

        if (exportFiltersXml != null && !CollectionUtils.isEmpty(exportFiltersXml.getPackageExportFilters())) {
            // procházím všechny definice exportních filtrů
            for (ExportFilterXml packageExportFilter : exportFiltersXml.getPackageExportFilters()) {

                //vyhledám exportní filtr podle záznamů v DB, pokud existuje
                Optional<RulExportFilter> findItems = dbExportFilters.stream().filter(
                        (r) -> r.getCode().equals(packageExportFilter
                                .getCode())).findFirst();
                // jinak založím prázdné seznamy
                RulExportFilter dbExportFilter = findItems.orElseGet(
                        RulExportFilter::new
                );

                // vytvořím/úpravím a uložím
                convertRulPackageExportFilter(rulPackage, packageExportFilter, dbExportFilter, ruc.getRulSet());

                rulPackageExportFiltersNew.add(dbExportFilter);
            }
        }

        // uložení nově vytvořených exportních filtrů
        rulPackageExportFiltersNew = exportFilterRepository.saveAll(rulPackageExportFiltersNew);

        // smazání nedefinovaných exportních filtrů
        List<RulExportFilter> rulPackageExportFiltersDelete = new ArrayList<>(dbExportFilters);
        rulPackageExportFiltersDelete.removeAll(rulPackageExportFiltersNew);
        exportFilterRepository.deleteAll(rulPackageExportFiltersDelete);

        // odstranění/vytvoření definičních souborů pro exportní filtry
        try {
            for (RulExportFilter exportFilter : rulPackageExportFiltersNew) {
                ruc.getPackageUpdateContext().saveFile(ruc.getExportFiltersDir(),
                        ZIP_DIR_RULE_SET + "/" + ruc.getRulSetCode() + "/" + ZIP_DIR_EXPORT_FILTERS, exportFilter.getFilename());
            }

        } catch (IOException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Smazání (reálně přesun) souboru.
     *
     * @param dir      adresář
     * @param filename název souboru
     */
    private void deleteFile(final File dir, final String filename) throws IOException {

        File file = new File(dir.getPath() + File.separator + filename);

        if (file.exists()) {
            File fileMove = new File(dir.getPath() + File.separator + filename + ".bck");
            Files.move(file.toPath(), fileMove.toPath());
        }

    }

    /**
     * Převod VO na DAO hromadné akce.
     *
     * @param rulPackage       balíček
     * @param packageAction    VO hromadné akce
     * @param rulPackageAction DAO hromadné akce
     * @param rulRuleSet       pravidla
     */
    private void convertRulPackageAction(final RulPackage rulPackage,
                                         final ActionXml packageAction,
                                         final RulAction rulPackageAction,
                                         final RulRuleSet rulRuleSet) {
        rulPackageAction.setPackage(rulPackage);
        rulPackageAction.setFilename(packageAction.getFilename());
        rulPackageAction.setRuleSet(rulRuleSet);
    }

    /**
     * Převod VO na DAO výstupního filtru.
     *
     * @param rulPackage       balíček
     * @param packageOutputFilter    VO výstupního filtru
     * @param rulOutputFilter  DAO výstupního filtru
     * @param rulRuleSet       pravidla
     */
    private void convertRulPackageOutputFilter(final RulPackage rulPackage,
                                         final OutputFilterXml packageOutputFilter,
                                         final RulOutputFilter rulOutputFilter,
                                         final RulRuleSet rulRuleSet) {
        rulOutputFilter.setRulPackage(rulPackage);
        rulOutputFilter.setCode(packageOutputFilter.getCode());
        rulOutputFilter.setName(packageOutputFilter.getName());
        rulOutputFilter.setFilename(packageOutputFilter.getFilename());
        rulOutputFilter.setRuleSet(rulRuleSet);
    }

    /**
     * Převod VO na DAO exportního filtru.
     *
     * @param rulPackage       balíček
     * @param packageExportFilter    VO exportního filtru
     * @param rulExportFilter  DAO exportního filtru
     * @param rulRuleSet       pravidla
     */
    private void convertRulPackageExportFilter(final RulPackage rulPackage,
                                               final ExportFilterXml packageExportFilter,
                                               final RulExportFilter rulExportFilter,
                                               final RulRuleSet rulRuleSet) {
        rulExportFilter.setRulPackage(rulPackage);
        rulExportFilter.setCode(packageExportFilter.getCode());
        rulExportFilter.setName(packageExportFilter.getName());
        rulExportFilter.setFilename(packageExportFilter.getFilename());
        rulExportFilter.setRuleSet(rulRuleSet);
    }

    /**
     * Zpracování typů atributů.
     *
     * @param pkgCtx balíček
     */
    private void processItemTypes(final PackageContext pkgCtx) {
        ItemSpecs itemSpecs = pkgCtx.convertXmlStreamToObject(ItemSpecs.class, ITEM_SPEC_XML);
        ItemTypes itemTypes = pkgCtx.convertXmlStreamToObject(ItemTypes.class, ITEM_TYPE_XML);

        ItemTypeUpdater updater = applicationContext.getBean(ItemTypeUpdater.class);

        updater.update(itemTypes, itemSpecs, pkgCtx);
        // check if node cache should be sync
        if (updater.getNumDroppedCachedNode() > 0) {
            pkgCtx.setSyncNodeCache(true);
        }
    }

    /**
     * Zpracování typů atributů.
     *
     * @param ruc context
     * @return výsledný seznam atributů v db
     */
    private List<RulOutputType> processOutputTypes(final RuleUpdateContext ruc) {
        OutputTypes outputTypes = ruc.convertXmlStreamToObject(OutputTypes.class,
                OUTPUT_TYPE_XML);

        List<RulOutputType> rulOutputTypes = outputTypeRepository.findByRulPackageAndRuleSet(ruc.getRulPackage(),
                ruc.getRulSet());
        List<RulOutputType> rulOutputTypesNew = new ArrayList<>();

        if (outputTypes != null && !CollectionUtils.isEmpty(outputTypes.getOutputTypes())) {
            for (OutputType outputType : outputTypes.getOutputTypes()) {
                List<RulOutputType> findItems = rulOutputTypes.stream()
                        .filter((r) -> r.getCode().equals(outputType.getCode())).collect(Collectors.toList());
                RulOutputType item;
                if (findItems.size() > 0) {
                    item = findItems.get(0);
                } else {
                    item = new RulOutputType();
                }

                convertRulOutputType(ruc.getRulPackage(), outputType, item, ruc.getRulSet());
                rulOutputTypesNew.add(item);
            }
        }

        rulOutputTypesNew = outputTypeRepository.saveAll(rulOutputTypesNew);

        // update templates
        TemplateUpdater templateUpdater = new TemplateUpdater(this.templateRepository, outputTemplateRepository,
                this.outputResultRepository,
                rulOutputTypesNew);
        templateUpdater.run(ruc);

        List<RulOutputType> rulOutputTypesDelete = new ArrayList<>(rulOutputTypes);
        rulOutputTypesDelete.removeAll(rulOutputTypesNew);

        if (!rulOutputTypesDelete.isEmpty()) {
            List<ArrOutput> byOutputTypes = outputRepository
                    .findByOutputTypes(rulOutputTypesDelete);
            if (!byOutputTypes.isEmpty()) {
                throw new IllegalStateException(
                        "Existuje výstup(y) navázáný na typ výstupu, který je v novém balíčku smazán.");
            }

            List<RulComponent> rulComponentsDelete = rulOutputTypesDelete.stream().map(RulOutputType::getComponent)
                    .filter(Objects::nonNull).collect(Collectors.toList());
            outputTypeRepository.deleteAll(rulOutputTypesDelete);
            componentRepository.deleteAll(rulComponentsDelete);
        }

        try {
            for (RulOutputType outputType : rulOutputTypesNew) {
                RulComponent component = outputType.getComponent();
                if (component != null && component.getFilename() != null) {
                    ruc.getPackageUpdateContext().saveFile(ruc.getRulesDir(),
                            ZIP_DIR_RULE_SET + "/" + ruc.getRulSetCode() + "/"
                                    + ZIP_DIR_RULES,
                            component.getFilename());
                }
            }
        } catch (IOException e) {
            throw new SystemException(e);
        }

        return rulOutputTypesNew;
    }


    /**
     * Zpracování pravidel.
     *
     * @param pkgCtx package context
     */
    private void processRuleSets(
            final PackageContext pkgCtx) {
        RuleSets xmlRulesets = pkgCtx.convertXmlStreamToObject(RuleSets.class, RULE_SET_XML);
        RulPackage rulPackage = pkgCtx.getPackage();

        List<RulRuleSet> rulRuleSets = ruleSetRepository.findByRulPackage(rulPackage);
        List<RulRuleSet> rulRuleSetsNew = new ArrayList<>();

        RulRuleSet entityRuleSet = null;


        if (xmlRulesets != null && !CollectionUtils.isEmpty(xmlRulesets.getRuleSets())) {
            for (RuleSetXml ruleSet : xmlRulesets.getRuleSets()) {
                // find ruleset in DB
                Optional<RulRuleSet> foundItem = rulRuleSets.stream().filter((r) -> r.getCode().equals(ruleSet
                        .getCode()))
                        .findFirst();
                RulRuleSet item = foundItem.orElseGet(() -> new RulRuleSet());

                convertRuleSet(rulPackage, ruleSet, item);
                rulRuleSetsNew.add(item);
                if (item.getRuleType() == RulRuleSet.RuleType.ENTITY) {
                    entityRuleSet = item;
                }

                RuleUpdateContext ruc = new RuleUpdateContext(RuleState.UPDATE, pkgCtx,
                        item, this.resourcePathResolver);
                pkgCtx.addRuleUpdateContext(ruc);
            }
        }

        // Uložení pravidel
        rulRuleSetsNew = ruleSetRepository.saveAll(rulRuleSetsNew);

        //nastavení pravidel pro entity u oblastí, které žádné nemají
        if (entityRuleSet != null) {
            List<ApScope> scopes = scopeRepository.findScopeByRuleSetIdIsNull();
            if (CollectionUtils.isNotEmpty(scopes)) {
                for (ApScope scope : scopes) {
                    scope.setRulRuleSet(entityRuleSet);
                }
                scopeRepository.saveAll(scopes);
            }
        }

        // Naplnění pravidel ke smazání, které již nejsou v xml
        for (RulRuleSet dbRuleset : rulRuleSets) {
            Optional<RulRuleSet> found = rulRuleSetsNew.stream().filter(n -> n.getCode().equals(dbRuleset.getCode()))
                    .findFirst();
            if (!found.isPresent()) {
                // rules not found -> have to be deleted
                RuleUpdateContext ruc = new RuleUpdateContext(RuleState.DELETE, pkgCtx,
                        dbRuleset, this.resourcePathResolver);
                pkgCtx.addRuleUpdateContext(ruc);
            }
        }

        // append rulesets with dir only
        Map<String, String> rulePaths = PackageUtils.findRulePaths(ZIP_DIR_RULE_SET, pkgCtx.getByteStreamKeys());
        for (String code : rulePaths.keySet()) {
            RuleUpdateContext ruc = pkgCtx.getRuleUpdateContextByCode(code);
            if (ruc == null) {
                // find ruleset in db
                RulRuleSet dbRuleset = ruleSetRepository.findByCode(code);
                Validate.notNull(dbRuleset, "Ruleset not exists, code: {}", code);
                // rules not found -> have to be added
                ruc = new RuleUpdateContext(RuleState.ADDON, pkgCtx,
                        dbRuleset, this.resourcePathResolver);
                pkgCtx.addRuleUpdateContext(ruc);
            }
        }
        try {
            for (RulRuleSet rulRuleSet : rulRuleSetsNew) {
                if (rulRuleSet.getItemTypeComponent() != null) {
                    RuleUpdateContext ruc = pkgCtx.getRuleUpdateContextByCode(rulRuleSet.getCode());
                    ruc.getPackageUpdateContext().saveFile(ruc.getRulesDir(),
                            ZIP_DIR_RULE_SET + "/" + ruc.getRulSetCode() + "/" + ZIP_DIR_RULES,
                            rulRuleSet.getItemTypeComponent().getFilename());
                }
            }
        } catch (IOException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Smazání pravidel.
     *
     * @param pkgCtx package context
     */
    private void deleteRuleSets(final PackageContext pkgCtx) {
        for (RuleUpdateContext ruc : pkgCtx.getRuleUpdateContexts()) {
            RuleState ruleState = ruc.getRuleState();
            if (ruleState == RuleState.DELETE) {
                RulComponent component = ruc.getRulSet().getItemTypeComponent();
                ruleSetRepository.delete(ruc.getRulSet());
                if (component != null) {
                    componentRepository.delete(component);
                }
            }
        }
    }

    /**
     * Převod VO na DAO pravidla.
     *
     * @param rulPackage balíček
     * @param ruleSet    VO pravidla
     * @param rulRuleSet DAO pravidla
     */
    private void convertRuleSet(final RulPackage rulPackage, final RuleSetXml ruleSet, final RulRuleSet rulRuleSet) {
        rulRuleSet.setCode(ruleSet.getCode());
        rulRuleSet.setName(ruleSet.getName());
        rulRuleSet.setRuleType(RulRuleSet.RuleType.fromValue(ruleSet.getRuleType()));
        rulRuleSet.setPackage(rulPackage);

        String filename = ruleSet.getRuleItemTypeFilter();
        if (filename != null) {
            RulComponent component = rulRuleSet.getItemTypeComponent();
            if (component == null) {
                component = new RulComponent();
            }
            component.setFilename(filename);
            componentRepository.save(component);
            rulRuleSet.setItemTypeComponent(component);
        } else {
            RulComponent component = rulRuleSet.getItemTypeComponent();
            rulRuleSet.setItemTypeComponent(null);
            if (component != null) {
                ruleSetRepository.save(rulRuleSet);
                componentRepository.delete(component);
            }
        }
    }

    /**
     * Zpracování importovaného balíčku.
     *
     * @param puc PackageContext
     */
    private void processRulPackage(PackageContext puc) {

        PackageInfo packageInfo = puc.getPackageInfo();

        RulPackage rulPackage = packageRepository.findByCode(packageInfo.getCode());

        if (rulPackage == null) {
            rulPackage = new RulPackage();
        } else {
            if (BooleanUtils.isNotTrue(getTesting()) && rulPackage.getVersion().equals(packageInfo.getVersion())) {
                throw new BusinessException("Verze (" + packageInfo.getVersion() + ") balíčku (" + rulPackage.getCode() + ") byla již aplikována", PackageCode.VERSION_APPLIED).set("code", rulPackage.getCode()).set("version", packageInfo.getVersion())
                        .set("version", rulPackage.getVersion());
            }
            puc.setOldPackageVersion(rulPackage.getVersion());
        }

        rulPackage.setCode(packageInfo.getCode());
        rulPackage.setName(packageInfo.getName());
        rulPackage.setDescription(packageInfo.getDescription());
        rulPackage.setVersion(packageInfo.getVersion());

        rulPackage = packageRepository.save(rulPackage);

        puc.setPackage(rulPackage);

        processRulPackageDependencies(packageInfo, rulPackage);

        detectCyclicDependencies();
    }

    /**
     * Zkontroluje závislosti, zda-li neobsahují cyklus.
     */
    private void detectCyclicDependencies() {
        packageDependencyRepository.flush();
        List<RulPackageDependency> packageDependencies = packageDependencyRepository.findAll();

        Map<Integer, Set<Integer>> dependencies = new HashMap<>();
        for (RulPackageDependency packageDependency : packageDependencies) {
            Set<Integer> packageIds = dependencies.computeIfAbsent(packageDependency.getPackageId(), k -> new HashSet<>());
            packageIds.add(packageDependency.getDependsOnPackageId());
        }

        for (Integer id : dependencies.keySet()) {
            detectCyclic(id, id, dependencies);
        }
    }

    /**
     * Rekurzivní prohledávání stromu a hledání cyklu.
     *
     * @param nextId       prohledávaný uzel
     * @param findId       hledaný konfliktní uzel
     * @param dependencies mapa stromu
     */
    private void detectCyclic(final Integer nextId, final Integer findId, final Map<Integer, Set<Integer>> dependencies) {
        Set<Integer> nextIds = dependencies.get(nextId);
        if (nextIds == null) {
            return;
        }
        if (nextIds.contains(findId)) {
            throw new BusinessException("Detekován cyklus v závislostech balíčků", PackageCode.CIRCULAR_DEPENDENCY);
        }
        for (Integer newNextId : nextIds) {
            detectCyclic(newNextId, findId, dependencies);
        }
    }

    /**
     * Zpracování a kontrola závislostí mezi balíčky.
     *
     * @param packageInfo VO importovaného balíčku
     * @param rulPackage  importovaný balíček
     */
    private void processRulPackageDependencies(final PackageInfo packageInfo, final RulPackage rulPackage) {

        // odeberu současné vazby
        packageDependencyRepository.deleteByRulPackage(rulPackage);

        // packageCode / minVersion
        Map<String, Integer> packageCodeVersion = new HashMap<>();
        if (CollectionUtils.isNotEmpty(packageInfo.getDependencies())) {
            for (PackageDependency dependency : packageInfo.getDependencies()) {
                if (dependency.getCode().equalsIgnoreCase(packageInfo.getCode())) {
                    throw new BusinessException("Nelze vytvořit závislost na definovaný balíček", PackageCode.CIRCULAR_DEPENDENCY);
                }
                packageCodeVersion.put(dependency.getCode(), dependency.getMinVersion());
            }
        }

        Collection<String> requiredDependencyCodes = packageCodeVersion.keySet();
        List<RulPackage> requiredDependencies = packageRepository.findByCodeIn(requiredDependencyCodes);
        if (requiredDependencies.size() != requiredDependencyCodes.size()) {
            Set<String> requiredDependencyFoundCodes = requiredDependencies.stream().map(RulPackage::getCode).collect(Collectors.toSet());
            requiredDependencyCodes.removeAll(requiredDependencyFoundCodes);
            throw new BusinessException("Balíčky nenalezeny: " + requiredDependencyCodes, PackageCode.FOREIGN_PACKAGES_NOT_EXIST).set("codes", requiredDependencyCodes);
        }

        List<RulPackageDependency> newDependencies = new ArrayList<>();
        for (RulPackage requiredDependency : requiredDependencies) {
            Integer minVersion = packageCodeVersion.get(requiredDependency.getCode());
            if (requiredDependency.getVersion() < minVersion) {
                throw new BusinessException("Není splněna minimální verze balíčku " + requiredDependency.getCode() + ": " + minVersion, PackageCode.MIN_DEPENDENCY)
                        .set("code", requiredDependency.getCode())
                        .set("version", minVersion);
            }
            RulPackageDependency packageDependency = new RulPackageDependency();
            packageDependency.setMinVersion(minVersion);
            packageDependency.setRulPackage(rulPackage);
            packageDependency.setDependsOnPackage(requiredDependency);
            newDependencies.add(packageDependency);
        }
        packageDependencyRepository.saveAll(newDependencies);
    }

    /**
     * Smazání importovaného balíčku podle kódu.
     * <p>
     * Note: only one package can be imported at a time
     *
     * @param code kód balíčku
     */
    @Transactional
    @AuthMethod(permission = {UsrPermission.Permission.ADMIN})
    synchronized public void deletePackage(final String code) {
        RulPackage rulPackage = packageRepository.findByCode(code);

        if (rulPackage == null) {
            throw new ObjectNotFoundException("Balíček s kódem " + code + " neexistuje", BaseCode.ID_NOT_EXIST);
        }

        // kontrola na existující zavíslostí z jiných balíčků
        List<RulPackageDependency> targetPackages = packageDependencyRepository.findByDependsOnPackage(rulPackage);
        if (targetPackages.size() > 0) {
            throw new BusinessException("Balíček nelze odebrat, protože je používán jiným balíčkem", PackageCode.FOREIGN_DEPENDENCY)
                    .set("foreignPackageCodes", targetPackages.stream().map(pd -> pd.getRulPackage().getCode()).collect(Collectors.toList()));
        }
        packageDependencyRepository.deleteByRulPackage(rulPackage);

        List<RulItemSpec> rulDescItemSpecs = itemSpecRepository.findByRulPackage(rulPackage);
        itemTypeSpecAssignRepository.deleteByItemSpecIn(rulDescItemSpecs);
        for (RulItemSpec rulDescItemSpec : rulDescItemSpecs) {
            itemAptypeRepository.deleteByItemSpec(rulDescItemSpec);
        }
        itemSpecRepository.deleteAll(rulDescItemSpecs);

        List<RulRuleSet> ruleSets = ruleSetRepository.findByRulPackage(rulPackage);
        List<RulArrangementRule> arrangementRules = arrangementRuleRepository.findByRulPackage(rulPackage);
        List<RulStructureExtensionDefinition> structureExtensionDefinitions = structureExtensionDefinitionRepository.findByRulPackage(rulPackage);
        List<RulStructureDefinition> structureDefinitions = structureDefinitionRepository.findByRulPackage(rulPackage);
        List<RulAction> actions = packageActionsRepository.findByRulPackage(rulPackage);
        List<RulOutputType> outputTypes = outputTypeRepository.findByRulPackage(rulPackage);
        List<RulItemType> rulDescItemTypes = itemTypeRepository.findByRulPackage(rulPackage);
        List<RulOutputFilter> outputFilters = outputFilterRepository.findByRulPackage(rulPackage);
        List<RulExportFilter> exportFilters = exportFilterRepository.findByRulPackage(rulPackage);

        packageActionsRepository.findByRulPackage(rulPackage).forEach(this::deleteActionLink);


        for (RulItemType rulDescItemType : rulDescItemTypes) {
            itemAptypeRepository.deleteByItemType(rulDescItemType);
        }
        itemTypeRepository.deleteByRulPackage(rulPackage);

        structureExtensionDefinitionRepository.deleteByRulPackage(rulPackage);
        structureExtensionRepository.deleteByRulPackage(rulPackage);
        structureDefinitionRepository.deleteByRulPackage(rulPackage);
        structureTypeRepository.deleteByRulPackage(rulPackage);
        partTypeRepository.deleteByRulPackage(rulPackage);
        packageActionsRepository.deleteByRulPackage(rulPackage);
        outputFilterRepository.deleteByRulPackage(rulPackage);
        exportFilterRepository.deleteByRulPackage(rulPackage);
        arrangementRuleRepository.deleteByRulPackage(rulPackage);
        policyTypeRepository.deleteByRulPackage(rulPackage);
        templateRepository.deleteByRulPackage(rulPackage);
        outputTypeRepository.deleteByRulPackage(rulPackage);
        extensionRuleRepository.deleteByRulPackage(rulPackage);
        arrangementExtensionRepository.deleteByRulPackage(rulPackage);
        ruleSetRepository.deleteAll(ruleSets);
        apTypeRepository.preDeleteByRulPackage(rulPackage);
        apTypeRepository.deleteByRulPackage(rulPackage);
        settingsRepository.deleteByRulPackage(rulPackage);
        issueStateRepository.deleteByRulPackage(rulPackage);
        issueTypeRepository.deleteByRulPackage(rulPackage);
        packageRepository.delete(rulPackage);

        entityManager.flush();

        for (RulRuleSet ruleSet : ruleSets) {
            File dirGroovies = resourcePathResolver.getGroovyDir(rulPackage.getPackageId(), ruleSet.getRuleSetId())
                    .toFile();
            File dirActions = resourcePathResolver.getFunctionsDir(rulPackage.getPackageId(), ruleSet.getRuleSetId()).toFile();
            File dirRules = resourcePathResolver.getDroolsDir(rulPackage.getPackageId(), ruleSet.getRuleSetId()).toFile();
            File dirOutputFilters = resourcePathResolver.getOutputFiltersDir(rulPackage.getPackageId(), ruleSet.getRuleSetId()).toFile();
            File dirExportFilters = resourcePathResolver.getExportFiltersDir(rulPackage.getPackageId(), ruleSet.getRuleSetId()).toFile();

            try {

                if (ruleSet.getItemTypeComponent() != null) {
                    deleteFile(dirRules, ruleSet.getItemTypeComponent().getFilename());
                    componentRepository.delete(ruleSet.getItemTypeComponent());
                }

                for (RulArrangementRule rulArrangementRule : arrangementRules) {
                    deleteFile(dirRules, rulArrangementRule.getComponent().getFilename());
                    componentRepository.delete(rulArrangementRule.getComponent());
                }

                for (RulStructureExtensionDefinition structureExtensionDefinition : structureExtensionDefinitions) {
                    File dir;
                    switch (structureExtensionDefinition.getDefType()) {
                        case ATTRIBUTE_TYPES:
                            dir = dirRules;
                            break;
                        case PARSE_VALUE:
                        case SERIALIZED_VALUE:
                            dir = dirGroovies;
                            break;
                        default:
                            throw new NotImplementedException("Neimplementovaný typ: " + structureExtensionDefinition.getDefType());
                    }
                    deleteFile(dir, structureExtensionDefinition.getComponent().getFilename());
                    componentRepository.delete(structureExtensionDefinition.getComponent());
                }

                for (RulStructureDefinition structureExtensionDefinition : structureDefinitions) {
                    File dir;
                    switch (structureExtensionDefinition.getDefType()) {
                        case ATTRIBUTE_TYPES:
                            dir = dirRules;
                            break;
                        case PARSE_VALUE:
                        case SERIALIZED_VALUE:
                            dir = dirGroovies;
                            break;
                        default:
                            throw new NotImplementedException("Neimplementovaný typ: " + structureExtensionDefinition.getDefType());
                    }
                    deleteFile(dir, structureExtensionDefinition.getComponent().getFilename());
                    componentRepository.delete(structureExtensionDefinition.getComponent());
                }

                for (RulAction rulPackageAction : actions) {
                    deleteFile(dirActions, rulPackageAction.getFilename());
                }

                for (RulOutputType outputType : outputTypes) {
                    RulComponent component = outputType.getComponent();
                    if (component != null && component.getFilename() != null) {
                        deleteFile(dirRules, component.getFilename());
                    }
                }

                for (RulOutputFilter rulOutputFilter : outputFilters) {
                    deleteFile(dirOutputFilters, rulOutputFilter.getFilename());
                }

                for (RulExportFilter rulExportFilter : exportFilters) {
                    deleteFile(dirExportFilters, rulExportFilter.getFilename());
                }

                entityManager.flush();

                bulkActionConfigManager.load();
            } catch (IOException e) {
                throw new SystemException("Nastala chyba během obnovy souborů po selhání importu balíčku", e);
            }
        }
    }

    /**
     * Smazání návazných entit.
     *
     * @param action hromadná akce
     */
    private void deleteActionLink(final RulAction action) {
        itemTypeActionRepository.deleteByAction(action);
        actionRecommendedRepository.deleteByAction(action);
    }

    /**
     * Vrací seznam importovaných balíčků.
     *
     * @return seznam balíčků
     */
    public List<RulPackage> getPackages() {
        return packageRepository.findAll();
    }

    /**
     * Vrací seznam importovaných balíčků.
     *
     * @return seznam balíčků
     */
    public List<RulPackageDependency> getPackagesDependencies() {
        return packageDependencyRepository.findAll();
    }

    /**
     * Provede export balíčku s konfigurací.
     * <p>
     * Note: only one package can be imported at a time
     *
     * @param code kód balíčku
     * @return výsledný soubor
     * @throws IOException
     */
    @Transactional(readOnly = true)
    synchronized public Path exportPackage(final String code) throws IOException {
        RulPackage rulPackage = packageRepository.findByCode(code);

        if (rulPackage == null) {
            throw new ObjectNotFoundException("Balíček s kódem " + code + " neexistuje", PackageCode.PACKAGE_NOT_EXIST).set("code", code);
        }

        try (AutoDeletingTempFile tempFile = AutoDeletingTempFile.createTempFile("ElzaPackage-" + code + "-",
                "-package.zip")) {
            exportPackage(rulPackage, tempFile.getPath());
            return tempFile.release();
        }

    }

    private void exportPackage(RulPackage rulPackage, Path path) throws IOException {

        try (FileOutputStream fos = new FileOutputStream(path.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos);) {

            exportPackageInfo(rulPackage, zos);
            exportRuleSet(rulPackage, zos);
            exportItemSpecs(rulPackage, zos);
            exportItemTypes(rulPackage, zos);
            exportPackageActions(rulPackage, zos);
            exportPackageOutputFilters(rulPackage, zos);
            exportPackageExportFilters(rulPackage, zos);
            exportArrangementRules(rulPackage, zos);
            exportArrangementExtensions(rulPackage, zos);
            exportExtensionRules(rulPackage, zos);
            exportOutputTypes(rulPackage, zos);
            exportTemplates(rulPackage, zos);
            exportRegisterTypes(rulPackage, zos);
            exportSettings(rulPackage, zos);
            exportExternalIdTypes(rulPackage, zos);
            exportIssueTypes(rulPackage, zos);
            exportIssueStates(rulPackage, zos);
            exportPartTypes(rulPackage, zos);
        }
    }

    /**
     * Exportování definic řídících pravidel.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportArrangementExtensions(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        List<RulArrangementExtension> rulPackageExtensions = arrangementExtensionRepository.findByRulPackage(rulPackage);

        if (rulPackageExtensions.size() == 0) {
            return;
        }

        Map<RulRuleSet, List<RulArrangementExtension>> ruleSetRuleMap = rulPackageExtensions.stream()
                .collect(Collectors.groupingBy(RulArrangementExtension::getRuleSet));

        for (Map.Entry<RulRuleSet, List<RulArrangementExtension>> entry : ruleSetRuleMap.entrySet()) {
            ArrangementExtensions arrangementExtensions = new ArrangementExtensions();
            List<RulArrangementExtension> rulArrangementExtensionList = entry.getValue();
            List<ArrangementExtension> arrangementExtensionsList = new ArrayList<>(rulArrangementExtensionList.size());
            arrangementExtensions.setArrangementExtensions(arrangementExtensionsList);
            String ruleSetCode = entry.getKey().getCode();
            for (RulArrangementExtension rulArrangementExtension : rulArrangementExtensionList) {
                ArrangementExtension arrangementExtension = new ArrangementExtension();
                convertArrangementExtension(rulArrangementExtension, arrangementExtension);
                arrangementExtensionsList.add(arrangementExtension);
            }
            addObjectToZipFile(arrangementExtensions, zos, ZIP_DIR_RULE_SET + "/" + ruleSetCode + "/" + ARRANGEMENT_EXTENSION_XML);
        }
    }

    /**
     * Exportování řídících pravidel.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportExtensionRules(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        List<RulExtensionRule> rulPackageRules = extensionRuleRepository.findByRulPackage(rulPackage);

        if (rulPackageRules.size() == 0) {
            return;
        }

        Map<RulRuleSet, List<RulExtensionRule>> ruleSetRuleMap = rulPackageRules.stream()
                .collect(Collectors.groupingBy(er -> er.getArrangementExtension().getRuleSet()));

        for (Map.Entry<RulRuleSet, List<RulExtensionRule>> entry : ruleSetRuleMap.entrySet()) {
            ExtensionRules extensionRules = new ExtensionRules();
            List<RulExtensionRule> ruleList = entry.getValue();
            List<ExtensionRule> extensionRuleList = new ArrayList<>(ruleList.size());
            extensionRules.setExtensionRules(extensionRuleList);
            RulRuleSet ruleSet = entry.getKey();
            String ruleSetCode = ruleSet.getCode();
            for (RulExtensionRule rulExtensionRule : ruleList) {
                ExtensionRule extensionRule = new ExtensionRule();
                convertExtensionRule(rulExtensionRule, extensionRule);
                extensionRuleList.add(extensionRule);
                addToZipFile(ZIP_DIR_RULE_SET + "/" + ruleSetCode + "/" + ZIP_DIR_RULES + "/" + rulExtensionRule.getComponent().getFilename(),
                        resourcePathResolver.getDroolsDir(rulPackage, ruleSet)
                                .resolve(rulExtensionRule.getComponent().getFilename()).toFile(),
                        zos);
            }

            addObjectToZipFile(extensionRules, zos, ZIP_DIR_RULE_SET + "/" + ruleSetCode + "/" + EXTENSION_RULE_XML);
        }
    }

    static class UISettingsExport {
        final List<UISettings> settings = new ArrayList<>();

        final Integer rulesetId;

        public UISettingsExport(final Integer rulesetId) {
            this.rulesetId = rulesetId;
        }

        public void add(UISettings uiSetts) {
            this.settings.add(uiSetts);

        }

        public List<UISettings> getSettings() {
            return settings;
        }

        public Integer getRuleSetId() {
            return rulesetId;
        }
    }


    /**
     * Přidání nastavení do zip souboru.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     * @throws IOException
     */
    private void exportSettings(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        List<UISettings> uiSettingsCol = settingsRepository.findByRulPackage(rulPackage);
        if (uiSettingsCol.size() == 0) {
            return;
        }

        HashMap<Integer, UISettingsExport> settingMap = new HashMap<>();
        // prepare export settings per ruleset
        for (UISettings uiSettings : uiSettingsCol) {
            Integer rulesetId = null;
            if (uiSettings.getEntityType() == EntityType.RULE) {
                rulesetId = uiSettings.getEntityId();
            }
            UISettingsExport expSettings = settingMap.computeIfAbsent(rulesetId, c -> new UISettingsExport(c));
            expSettings.add(uiSettings);
        }

        // run export
        for (UISettingsExport c : settingMap.values()) {
            exportSettingsForRuleset(c, zos);
        }
    }

    private void exportSettingsForRuleset(UISettingsExport uiSettingsForRuleset,
                                          final ZipOutputStream zos) throws IOException {
        StaticDataProvider sdp = this.staticDataService.getData();
        RuleSet ruleSet = null;
        if (uiSettingsForRuleset.getRuleSetId() != null) {
            ruleSet = sdp.getRuleSetById(uiSettingsForRuleset.getRuleSetId());
        }

        String fileName = ruleSet == null ? SETTING_XML : ZIP_DIR_RULE_SET + "/" + ruleSet.getCode() + "/" + SETTING_XML;

        Settings settings = new Settings();
        List<Setting> settingsList = new ArrayList<>();
        settings.setSettings(settingsList);

        uiSettingsForRuleset.getSettings().forEach(uiSettings -> {
            // export single settings
            Setting setting = settingsService.convertSetting(uiSettings);
            settingsList.add(setting);
        });

        addObjectToZipFile(settings, zos, fileName);
    }

    @FunctionalInterface
    interface Setter<One, Two> {
        void apply(One one, Two two);
    }

    @FunctionalInterface
    interface Convertor<One, Two> {
        void apply(One one, Two two);
    }

    @FunctionalInterface
    interface ContextFunction<T, U, R> {
        R apply(T t, U u);
    }

    /**
     * Generická metoda pro export entity do balíčku.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     * @param repository repozitory ukládané entity
     * @param clazzs     třída seznamu VO
     * @param clazz      třída VO
     * @param setter     setter metoda pro naplnění VO
     * @param convertor  metoda pro konverzi DO na VO
     * @param factory    factory metoda - používá se u abstraktních tříd
     * @param filter     funkce pro filtrování položek
     * @param fileName   název souboru
     * @param context    entita pro context
     */
    private <R extends Packaging<E>, E, TS, T, U> void export(final RulPackage rulPackage,
                                                              final ZipOutputStream zos,
                                                              final R repository,
                                                              final Class<TS> clazzs,
                                                              final Class<T> clazz,
                                                              final Setter<List<T>, TS> setter,
                                                              final Convertor<E, T> convertor,
                                                              final ContextFunction<E, U, T> factory,
                                                              final Predicate<E> filter,
                                                              final String fileName,
                                                              final U context) {
        try {
            TS entities = clazzs.newInstance();
            List<E> dbEntities = repository.findByRulPackage(rulPackage);
            if (dbEntities.size() == 0) {
                return;
            }
            List<T> entityList = new ArrayList<>(dbEntities.size());
            setter.apply(entityList, entities);

            for (E entity : dbEntities) {
                T instance;
                if (factory == null) {
                    instance = clazz.newInstance();
                } else {
                    instance = factory.apply(entity, context);
                }
                convertor.apply(entity, instance);
                if (filter == null || filter.test(entity)) {
                    entityList.add(instance);
                }
            }

            addObjectToZipFile(entities, zos, fileName);
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    private void exportRegisterTypes(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        APTypes registerTypes = new APTypes();
        List<ApType> apTypes = apTypeRepository.findByRulPackage(rulPackage);
        if (apTypes.size() == 0) {
            return;
        }
        List<APTypeXml> registerTypeList = new ArrayList<>(apTypes.size());
        registerTypes.setRegisterTypes(registerTypeList);

        for (ApType apType : apTypes) {
            APTypeXml registerType = new APTypeXml();
            convertRegisterType(apType, registerType);
            registerTypeList.add(registerType);
        }

        addObjectToZipFile(registerTypes, zos, APTypeUpdater.AP_TYPE_XML);
    }

    private void convertRegisterType(final ApType apType, final APTypeXml registerType) {
        registerType.setName(apType.getName());
        registerType.setCode(apType.getCode());
        registerType.setReadOnly(apType.isReadOnly());
        registerType.setParentType(apType.getParentApType() == null ? null : apType.getParentApType().getCode());
    }

    private void exportOutputTypes(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        List<RulOutputType> rulRuleSets = outputTypeRepository.findByRulPackage(rulPackage);
        if (rulRuleSets.size() == 0) {
            return;
        }

        Map<RulRuleSet, List<RulOutputType>> ruleSetOutputTypeMap = rulRuleSets.stream()
                .collect(Collectors.groupingBy(RulOutputType::getRuleSet));

        for (Map.Entry<RulRuleSet, List<RulOutputType>> entry : ruleSetOutputTypeMap.entrySet()) {
            OutputTypes outputTypes = new OutputTypes();
            List<RulOutputType> rulRuleSetList = entry.getValue();
            List<OutputType> ruleSetList = new ArrayList<>(rulRuleSetList.size());
            outputTypes.setOutputTypes(ruleSetList);

            for (RulOutputType rulOutputType : rulRuleSetList) {
                OutputType outputType = new OutputType();
                convertOutputType(rulOutputType, outputType);
                ruleSetList.add(outputType);
            }

            addObjectToZipFile(outputTypes, zos, ZIP_DIR_RULE_SET + "/" + entry.getKey().getCode() + "/" + OUTPUT_TYPE_XML);
        }
    }

    private void exportTemplates(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        List<RulTemplate> rulTemplates = templateRepository.findByRulPackageAndNotDeleted(rulPackage);
        if (rulTemplates.size() == 0) {
            return;
        }

        Map<RulRuleSet, List<RulTemplate>> ruleSetTemplateMap = rulTemplates.stream()
                .collect(Collectors.groupingBy(t -> t.getOutputType().getRuleSet()));

        for (Map.Entry<RulRuleSet, List<RulTemplate>> entry : ruleSetTemplateMap.entrySet()) {
            Templates outputTypes = new Templates();
            List<RulTemplate> rulTemplatesList = entry.getValue();
            List<TemplateXml> templateList = new ArrayList<>(rulTemplatesList.size());
            outputTypes.setTemplates(templateList);
            String ruleSetCode = entry.getKey().getCode();
            for (RulTemplate rulTemplate : rulTemplatesList) {
                TemplateXml outputType = new TemplateXml();
                convertTemplate(rulTemplate, outputType);
                templateList.add(outputType);
                File dir = resourcePathResolver.getTemplateDir(rulTemplate).toFile();
                for (File dirFile : dir.listFiles()) {
                    addToZipFile(ZIP_DIR_RULE_SET + "/" + ruleSetCode + "/" + ZIP_DIR_TEMPLATES + "/" + rulTemplate.getDirectory() + "/" + dirFile.getName(), dirFile, zos);
                }
            }

            addObjectToZipFile(outputTypes, zos,
                    ZIP_DIR_RULE_SET + "/" + ruleSetCode + "/" + TemplateUpdater.TEMPLATE_XML);
        }
    }

    /**
     * Exportování pravidel.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportRuleSet(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        RuleSets ruleSets = new RuleSets();
        List<RulRuleSet> rulRuleSets = ruleSetRepository.findByRulPackage(rulPackage);
        if (rulRuleSets.size() == 0) {
            return;
        }
        List<RuleSetXml> ruleSetList = new ArrayList<>(rulRuleSets.size());
        ruleSets.setRuleSets(ruleSetList);

        for (RulRuleSet rulRuleSet : rulRuleSets) {
            RuleSetXml ruleSet = new RuleSetXml();
            covertRuleSet(rulRuleSet, ruleSet);
            ruleSetList.add(ruleSet);

            if (rulRuleSet.getItemTypeComponent() != null) {
                File ruleFile = resourcePathResolver.getDroolFile(rulRuleSet).toFile();
                addToZipFile(ZIP_DIR_RULE_SET + "/" + rulRuleSet.getCode() + "/" + ZIP_DIR_RULES + "/"
                        + rulRuleSet.getItemTypeComponent().getFilename(), ruleFile, zos);
            }
        }

        addObjectToZipFile(ruleSets, zos, RULE_SET_XML);
    }

    /**
     * Exportování typů externích identifikátorů.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportExternalIdTypes(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        ExternalIdTypes externalIdTypes = new ExternalIdTypes();
        List<ApExternalIdType> apExternalIdTypes = externalIdTypeRepository.findByRulPackage(rulPackage);
        if (apExternalIdTypes.size() == 0) {
            return;
        }
        List<ExternalIdType> externalIdTypeList = new ArrayList<>(apExternalIdTypes.size());
        externalIdTypes.setExternalIdTypes(externalIdTypeList);

        for (ApExternalIdType apExternalIdType : apExternalIdTypes) {
            ExternalIdType externalIdType = new ExternalIdType();
            convertExternalIdType(apExternalIdType, externalIdType);
            externalIdTypeList.add(externalIdType);
        }

        addObjectToZipFile(externalIdTypes, zos, EXTERNAL_ID_TYPE_XML);
    }

    /**
     * Převod DAO na VO pro typy externích identifikátorů.
     *
     * @param apExternalIdType DAO typ
     * @param externalIdType   VO typ
     */
    private void convertExternalIdType(final ApExternalIdType apExternalIdType, final ExternalIdType externalIdType) {
        externalIdType.setCode(apExternalIdType.getCode());
        externalIdType.setName(apExternalIdType.getName());
    }

    /**
     * Create package info from DB object
     *
     * @param rulPackage
     * @return
     */
    PackageInfo getPackageInfo(final RulPackage rulPackage) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setCode(rulPackage.getCode());
        packageInfo.setName(rulPackage.getName());
        packageInfo.setDescription(rulPackage.getDescription());
        packageInfo.setVersion(rulPackage.getVersion());

        List<RulPackageDependency> dependencies = packageDependencyRepository.findByRulPackage(rulPackage);
        packageInfo.setDependencies(dependencies.stream()
                .map(d -> {
                    PackageDependency pd = new PackageDependency();
                    pd.setCode(d.getDependsOnPackage().getCode());
                    pd.setMinVersion(d.getMinVersion());
                    return pd;
                }).collect(Collectors.toList()));

        return packageInfo;
    }

    /**
     * Exportování informace o balíčku
     *
     * @param rulPackage
     *            balíček
     * @param zos
     *            stream zip souboru
     */
    private void exportPackageInfo(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        PackageInfo packageInfo = getPackageInfo(rulPackage);

        addObjectToZipFile(packageInfo, zos, PackageContext.PACKAGE_XML);
    }

    /**
     * Exportování základních pravidel pro archivní popis.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportArrangementRules(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        List<RulArrangementRule> rulPackageRules = arrangementRuleRepository.findByRulPackage(rulPackage);

        if (rulPackageRules.size() == 0) {
            return;
        }

        Map<RulRuleSet, List<RulArrangementRule>> ruleSetRuleMap = rulPackageRules.stream()
                .collect(Collectors.groupingBy(RulArrangementRule::getRuleSet));

        for (Map.Entry<RulRuleSet, List<RulArrangementRule>> entry : ruleSetRuleMap.entrySet()) {
            ArrangementRules packageRules = new ArrangementRules();
            List<RulArrangementRule> ruleList = entry.getValue();
            List<ArrangementRule> packageRuleList = new ArrayList<>(ruleList.size());
            packageRules.setArrangementRules(packageRuleList);
            String ruleSetCode = entry.getKey().getCode();
            for (RulArrangementRule rulArrangementRule : ruleList) {
                ArrangementRule packageRule = new ArrangementRule();
                convertArrangementRule(rulArrangementRule, packageRule);
                packageRuleList.add(packageRule);

                File ruleFile = resourcePathResolver.getDroolFile(rulArrangementRule).toFile();
                addToZipFile(ZIP_DIR_RULE_SET + "/" + ruleSetCode + "/" + ZIP_DIR_RULES + "/"
                        + rulArrangementRule.getComponent().getFilename(), ruleFile, zos);

            }

            addObjectToZipFile(packageRules, zos, ZIP_DIR_RULE_SET + "/" + ruleSetCode + "/" + ARRANGEMENT_RULE_XML);
        }
    }

    /**
     * Exportování hromadných akcí.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportPackageActions(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        List<RulAction> rulPackageActions = packageActionsRepository.findByRulPackage(rulPackage);
        if (rulPackageActions.size() == 0) {
            return;
        }

        Map<RulRuleSet, List<RulAction>> ruleSetActionMap = rulPackageActions.stream()
                .collect(Collectors.groupingBy(RulAction::getRuleSet));

        for (Map.Entry<RulRuleSet, List<RulAction>> entry : ruleSetActionMap.entrySet()) {
            ActionsXml packageActions = new ActionsXml();
            List<RulAction> actionList = entry.getValue();
            List<ActionXml> packageActionList = new ArrayList<>(actionList.size());
            packageActions.setPackageActions(packageActionList);
            String ruleSetCode = entry.getKey().getCode();
            for (RulAction rulPackageAction : actionList) {
                ActionXml packageAction = new ActionXml();
                convertPackageAction(rulPackageAction, packageAction);
                packageActionList.add(packageAction);

                File functionFile = resourcePathResolver.getFunctionFile(rulPackageAction).toFile();
                addToZipFile(ZIP_DIR_RULE_SET + "/" + ruleSetCode + "/" + ZIP_DIR_ACTIONS + "/" + rulPackageAction.getFilename(),
                        functionFile, zos);
            }

            addObjectToZipFile(packageActions, zos, ZIP_DIR_RULE_SET + "/" + ruleSetCode + "/" + PACKAGE_ACTIONS_XML);
        }
    }

    /**
     * Exportování výstupních filtrů.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportPackageOutputFilters(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        List<RulOutputFilter> rulOutputFilters = outputFilterRepository.findByRulPackage(rulPackage);
        if (rulOutputFilters.size() == 0) {
            return;
        }

        Map<RulRuleSet, List<RulOutputFilter>> ruleSetOutputFilterMap = rulOutputFilters.stream()
                .collect(Collectors.groupingBy(RulOutputFilter::getRuleSet));

        for (Map.Entry<RulRuleSet, List<RulOutputFilter>> entry : ruleSetOutputFilterMap.entrySet()) {
            OutputFiltersXml packageOutputFilters = new OutputFiltersXml();
            List<RulOutputFilter> outputFilterList = entry.getValue();
            List<OutputFilterXml> packageOutputFilterList = new ArrayList<>(outputFilterList.size());
            packageOutputFilters.setPackageOutputFilters(packageOutputFilterList);
            String ruleSetCode = entry.getKey().getCode();
            for (RulOutputFilter rulPackageOutputFilter : outputFilterList) {
                OutputFilterXml packageOutputFilter = new OutputFilterXml();
                convertPackageOutputFilter(rulPackageOutputFilter, packageOutputFilter);
                packageOutputFilterList.add(packageOutputFilter);

                File outputFilterFile = resourcePathResolver.getOutputFilterFile(rulPackageOutputFilter).toFile();
                addToZipFile(ZIP_DIR_RULE_SET + "/" + ruleSetCode + "/" + ZIP_DIR_OUTPUT_FILTERS + "/" + rulPackageOutputFilter.getFilename(),
                        outputFilterFile, zos);
            }

            addObjectToZipFile(packageOutputFilters, zos, ZIP_DIR_RULE_SET + "/" + ruleSetCode + "/" + PACKAGE_OUTPUT_FILTERS_XML);
        }
    }

    /**
     * Exportování exportních filtrů.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportPackageExportFilters(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        List<RulExportFilter> rulExportFilters = exportFilterRepository.findByRulPackage(rulPackage);
        if (rulExportFilters.size() == 0) {
            return;
        }

        Map<RulRuleSet, List<RulExportFilter>> ruleSetExportFilterMap = rulExportFilters.stream()
                .collect(Collectors.groupingBy(RulExportFilter::getRuleSet));

        for (Map.Entry<RulRuleSet, List<RulExportFilter>> entry : ruleSetExportFilterMap.entrySet()) {
            ExportFiltersXml packageExportFilters = new ExportFiltersXml();
            List<RulExportFilter> exportFilterList = entry.getValue();
            List<ExportFilterXml> packageExportFilterList = new ArrayList<>(exportFilterList.size());
            packageExportFilters.setPackageExportFilters(packageExportFilterList);
            String ruleSetCode = entry.getKey().getCode();
            for (RulExportFilter rulPackageExportFilter : exportFilterList) {
                ExportFilterXml packageExportFilter = new ExportFilterXml();
                convertPackageExportFilter(rulPackageExportFilter, packageExportFilter);
                packageExportFilterList.add(packageExportFilter);

                File exportFilterFile = resourcePathResolver.getExportFilterFile(rulPackageExportFilter).toFile();
                addToZipFile(ZIP_DIR_RULE_SET + "/" + ruleSetCode + "/" + ZIP_DIR_EXPORT_FILTERS + "/" + rulPackageExportFilter.getFilename(),
                        exportFilterFile, zos);
            }

            addObjectToZipFile(packageExportFilters, zos, ZIP_DIR_RULE_SET + "/" + ruleSetCode + "/" + PACKAGE_EXPORT_FILTERS_XML);
        }
    }

    /**
     * Exportování typů atributů.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportItemTypes(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        List<RulItemType> rulDescItemTypes = itemTypeRepository.findByRulPackageOrderByViewOrderAsc(rulPackage);
        if (rulDescItemTypes.size() == 0) {
            return;
        }

        ItemTypes itemTypes = new ItemTypes();
        List<ItemType> itemTypeList = new ArrayList<>(rulDescItemTypes.size());
        itemTypes.setItemTypes(itemTypeList);

        for (RulItemType rulDescItemType : rulDescItemTypes) {
            ItemType itemType = ItemType.fromEntity(rulDescItemType, itemAptypeRepository);
            itemTypeList.add(itemType);
        }

        addObjectToZipFile(itemTypes, zos, ITEM_TYPE_XML);
    }

    /**
     * Exportování specifikací atributů.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportItemSpecs(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        List<RulItemSpec> rulDescItemSpecs = itemSpecRepository.findByRulPackageFetchItemType(rulPackage);
        if (CollectionUtils.isEmpty(rulDescItemSpecs)) {
            return;
        }

        List<RulItemTypeSpecAssign> typeAssigned = itemTypeSpecAssignRepository.findByItemSpecIn(rulDescItemSpecs);
        Map<Integer, List<String>> typeAssignedBySpecId = typeAssigned.stream()
                .collect(Collectors.groupingBy(tsa -> tsa.getItemSpec().getItemSpecId(),
                                               Collectors.mapping(tsa -> tsa.getItemType().getCode(),
                                                                  Collectors.toList())));

        ItemSpecs itemSpecs = new ItemSpecs();
        List<ItemSpec> itemSpecList = new ArrayList<>(rulDescItemSpecs.size());
        itemSpecs.setItemSpecs(itemSpecList);

        for (RulItemSpec rulDescItemSpec : rulDescItemSpecs) {
            List<String> assignedTypes = typeAssignedBySpecId.get(rulDescItemSpec.getItemSpecId());

            ItemSpec itemSpec = ItemSpec.fromEntity(rulDescItemSpec, assignedTypes, itemAptypeRepository);
            itemSpecList.add(itemSpec);
        }

        addObjectToZipFile(itemSpecs, zos, ITEM_SPEC_XML);
    }

    /**
     * Exportování druhů připomínek
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportIssueTypes(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {

        List<WfIssueType> wfIssueTypes = issueTypeRepository.findByRulPackage(rulPackage);

        if (!wfIssueTypes.isEmpty()) {

            List<IssueType> issueTypeList = new ArrayList<>(wfIssueTypes.size());
            for (WfIssueType wfIssueType : wfIssueTypes) {
                IssueType issueType = new IssueType();
                issueType.setCode(wfIssueType.getCode());
                issueType.setName(wfIssueType.getName());
                issueType.setViewOrder(wfIssueType.getViewOrder());
                issueTypeList.add(issueType);
            }

            IssueTypes issueTypes = new IssueTypes();
            issueTypes.setIssueTypes(issueTypeList);

            addObjectToZipFile(issueTypes, zos, ISSUE_TYPE_XML);
        }
    }

    /**
     * Exportování stavů připomínek
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportIssueStates(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {

        List<WfIssueState> wfIssueStates = issueStateRepository.findByRulPackage(rulPackage);

        if (!wfIssueStates.isEmpty()) {

            List<IssueState> issueStateList = new ArrayList<>(wfIssueStates.size());
            for (WfIssueState wfIssueState : wfIssueStates) {
                IssueState issueState = new IssueState();
                issueState.setCode(wfIssueState.getCode());
                issueState.setName(wfIssueState.getName());
                issueState.setStartState(wfIssueState.isStartState());
                issueState.setFinalState(wfIssueState.isFinalState());
                issueStateList.add(issueState);
            }

            IssueStates issueStates = new IssueStates();
            issueStates.setIssueStates(issueStateList);

            addObjectToZipFile(issueStates, zos, ISSUE_STATE_XML);
        }
    }

    /**
     * Převod DAO na VO pravidla.
     *
     * @param rulRuleSet DAO pravidla
     * @param ruleSet    VO pravidla
     */
    private void covertRuleSet(final RulRuleSet rulRuleSet, final RuleSetXml ruleSet) {
        ruleSet.setCode(rulRuleSet.getCode());
        ruleSet.setName(rulRuleSet.getName());
        ruleSet.setRuleType(rulRuleSet.getRuleType().value());

        if (rulRuleSet.getItemTypeComponent() != null) {
            ruleSet.setRuleItemTypeFilter(rulRuleSet.getItemTypeComponent().getFilename());
        }
    }

    /**
     * Převod DAO na VO typu outputu.
     *
     * @param rulOutputType DAO packet
     * @param outputType    VO packet
     */
    private void convertOutputType(final RulOutputType rulOutputType, final OutputType outputType) {
        outputType.setCode(rulOutputType.getCode());
        outputType.setName(rulOutputType.getName());
        outputType.setFilename(rulOutputType.getComponent() == null ? null : rulOutputType.getComponent().getFilename());
    }

    /**
     * Převod DAO na VO template.
     *
     * @param rulOutputType DAO packet
     * @param outputType    VO packet
     */
    private void convertTemplate(final RulTemplate rulOutputType, final TemplateXml outputType) {
        outputType.setCode(rulOutputType.getCode());
        outputType.setName(rulOutputType.getName());
        outputType.setDirectory(rulOutputType.getDirectory());
        outputType.setEngine(rulOutputType.getEngine().toString());
        outputType.setOutputType(rulOutputType.getOutputType().getCode());
        outputType.setMimeType(rulOutputType.getMimeType());
        outputType.setExtension(rulOutputType.getExtension());
    }


    /**
     * Převod DAO na VO pravidla.
     *
     * @param rulArrangementRule DAO pravidla
     * @param packageRule        VO pravidla
     */
    private void convertArrangementRule(final RulArrangementRule rulArrangementRule, final ArrangementRule packageRule) {
        packageRule.setFilename(rulArrangementRule.getComponent().getFilename());
        packageRule.setPriority(rulArrangementRule.getPriority());
        packageRule.setRuleType(rulArrangementRule.getRuleType());
    }

    /**
     * Převod DAO na VO rozšíření.
     *
     * @param rulArrangementExtension DAO rozšíření
     * @param arrangementExtension    VO rozšíření
     */
    private void convertArrangementExtension(final RulArrangementExtension rulArrangementExtension,
                                             final ArrangementExtension arrangementExtension) {
        arrangementExtension.setCode(rulArrangementExtension.getCode());
        arrangementExtension.setName(rulArrangementExtension.getName());
    }

    /**
     * Převod DAO na VO řídících pravidel.
     *
     * @param rulExtensionRule DAO řídících pravidel
     * @param extensionRule    VO řídících pravidel
     */
    private void convertExtensionRule(final RulExtensionRule rulExtensionRule,
                                      final ExtensionRule extensionRule) {
        extensionRule.setFilename(rulExtensionRule.getComponent().getFilename());
        extensionRule.setPriority(rulExtensionRule.getPriority());
        extensionRule.setRuleType(rulExtensionRule.getRuleType());
        extensionRule.setArrangementExtension(rulExtensionRule.getArrangementExtension().getCode());
        extensionRule.setCompatibilityRulPackage(rulExtensionRule.getCompatibilityRulPackage());
    }

    /**
     * Převod DAO na VO hromadné akce.
     *
     * @param rulPackageAction DAO hromadné akce
     * @param packageAction    VO hromadné akce
     */
    private void convertPackageAction(final RulAction rulPackageAction, final ActionXml packageAction) {
        packageAction.setFilename(rulPackageAction.getFilename());

        List<ActionItemType> actionItemTypeList = new ArrayList<>();
        packageAction.setActionItemTypes(actionItemTypeList);

        for (RulItemTypeAction rulItemTypeAction : itemTypeActionRepository.findByAction(rulPackageAction)) {
            ActionItemType actionItemType = new ActionItemType();
            actionItemType.setItemType(rulItemTypeAction.getItemType().getCode());
            actionItemTypeList.add(actionItemType);
        }

        List<ActionRecommended> actionRecommendedList = new ArrayList<>();
        packageAction.setActionRecommendeds(actionRecommendedList);

        for (RulActionRecommended rulActionRecommended : actionRecommendedRepository.findByAction(rulPackageAction)) {
            ActionRecommended actionRecommended = new ActionRecommended();
            actionRecommended.setOutputType(rulActionRecommended.getOutputType().getCode());
            actionRecommendedList.add(actionRecommended);
        }
    }

    /**
     * Převod DAO na VO výstupního filtru.
     *
     * @param rulPackageOutputFilter DAO výstupního filtru
     * @param packageOutputFilter    VO výstupního filtru
     */
    private void convertPackageOutputFilter(final RulOutputFilter rulPackageOutputFilter, final OutputFilterXml packageOutputFilter) {
        packageOutputFilter.setCode(rulPackageOutputFilter.getCode());
        packageOutputFilter.setName(rulPackageOutputFilter.getName());
        packageOutputFilter.setFilename(rulPackageOutputFilter.getFilename());
    }

    /**
     * Převod DAO na VO exportního filtru.
     *
     * @param rulPackageExportFilter DAO exportního filtru
     * @param packageExportFilter    VO exportního filtru
     */
    private void convertPackageExportFilter(final RulExportFilter rulPackageExportFilter, final ExportFilterXml packageExportFilter) {
        packageExportFilter.setCode(rulPackageExportFilter.getCode());
        packageExportFilter.setName(rulPackageExportFilter.getName());
        packageExportFilter.setFilename(rulPackageExportFilter.getFilename());
    }

    /**
     * Exportování typů částí přístupových bodů
     *
     * @param rulPackage balíček
     * @param zos stream zip souboru
     * @throws IOException
     */
    private void exportPartTypes(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        List<RulPartType> rulPartTypes = partTypeRepository.findByRulPackage(rulPackage);

        if (CollectionUtils.isNotEmpty(rulPartTypes)) {
            PartTypes partTypes = new PartTypes();
            List<PartType> partTypeList = new ArrayList<>(rulPartTypes.size());
            partTypes.setPartTypes(partTypeList);

            for (RulPartType rulPartType : rulPartTypes) {
                PartType partType = new PartType();
                convertPartType(rulPartType, partType);
                partTypeList.add(partType);
            }
            processRulPartTypesChildPart(rulPartTypes, partTypeList);

            addObjectToZipFile(partTypes, zos, PART_TYPE_XML);
        }
    }

    /**
     * Převod DAO na VO typů částí přístupových bodů
     *
     * @param rulPartType DAO typu části přístupových bodů
     * @param partType VO typu části přístupových bodů
     */
    private void convertPartType(RulPartType rulPartType, PartType partType) {
        partType.setCode(rulPartType.getCode());
        partType.setName(rulPartType.getName());
        partType.setRepeatable(rulPartType.getRepeatable());
    }

    private void processRulPartTypesChildPart(List<RulPartType> rulPartTypes, List<PartType> partTypes) {
        if (CollectionUtils.isNotEmpty(rulPartTypes) && CollectionUtils.isNotEmpty(partTypes)) {
            for (RulPartType rulPartType : rulPartTypes) {
                if (rulPartType.getChildPart() != null) {
                    PartType partType = findPartTypeByCode(partTypes, rulPartType.getCode());
                    if (partType == null) {
                        throw new IllegalStateException("Nenalezen typ části s kódem: " + rulPartType.getCode());
                    }

                    PartType childPartType = findPartTypeByCode(partTypes, rulPartType.getChildPart().getCode());
                    if (childPartType == null) {
                        throw new IllegalStateException("Nenalezen typ podřízené části s kódem: " + rulPartType.getChildPart().getCode());
                    }

                    partType.setChildPart(childPartType.getCode());
                }
            }
        }
    }

    @Nullable
    private PartType findPartTypeByCode(final List<PartType> partTypes, final String code) {
        if (CollectionUtils.isNotEmpty(partTypes)) {
            for (PartType partType : partTypes) {
                if (partType.getCode().equals(code)) {
                    return partType;
                }
            }
        }
        return null;
    }

    /**
     * Převod objekt souboru (XML) do XML souboru.
     *
     * @param data objekt souboru (XML)
     * @return převedený dočasný soubor
     */
    private <T> File convertObjectToXmlFile(final T data) {
        try {
            File file = File.createTempFile(data.getClass().getSimpleName() + "-", ".xml");
            JAXBContext jaxbContext = JAXBContext.newInstance(data.getClass());
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(data, file);
            return file;
        } catch (Exception e) {
            throw new IllegalStateException("Problém při konverzi xml objektu do xml souboru", e);
        }
    }

    /**
     * Přidání souboru do zip souboru.
     *
     * @param fileName název souboru v zip
     * @param file     zdrojový soubor
     * @param zos      stream zip souboru
     */
    private void addToZipFile(final String fileName, final File file, final ZipOutputStream zos) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            zos.closeEntry();
        }
    }

    /**
     * Přidání objekt souboru (XML) do zip souboru.
     *
     * @param data     objekt souboru (XML)
     * @param zos      stream zip souboru
     * @param fileName název souboru v zip
     */
    private <T> void addObjectToZipFile(final T data, final ZipOutputStream zos, final String fileName)
            throws IOException {
        File xmlFile = convertObjectToXmlFile(data);
        try {
            addToZipFile(fileName, xmlFile, zos);
        } finally {
            xmlFile.delete();
        }
    }

    private void enqueueAccessPoints(RulStructureDefinition rulStructureDefinition) {
        StaticDataProvider sdp = staticDataService.getData();
        RulPartType partType = sdp.getPartTypeByCode(rulStructureDefinition.getStructuredType().getCode());
        if (partType != null) {
            String apTypeCode = null;
            enqueueAccessPoints(apTypeCode);
        }
    }

    public void enqueueAccessPoints(RulStructureExtensionDefinition rulStructureExtensionDefinition) {
        String apTypeCode = null;
        String structureExtensionCode = rulStructureExtensionDefinition.getStructuredTypeExtension().getCode();
        String[] strArray = StringUtils.split(structureExtensionCode, "/");
        if (strArray != null && strArray.length == 2) {
            apTypeCode = strArray[0];
        }

        enqueueAccessPoints(apTypeCode);
    }

    private void enqueueAccessPoints(RulExtensionRule rulExtensionRule) {
        String apTypeCode = null;
        String arrangementExtensionCode = rulExtensionRule.getArrangementExtension().getCode();
        String[] strArray = StringUtils.split(arrangementExtensionCode, "/");
        if (strArray != null && strArray.length > 0) {
            String rulType = strArray[0];
            if ((rulType.equals(AVAILABLE_ITEMS) && strArray.length == 3) ||
                    (rulType.equals(VALIDATION) && strArray.length == 2)) {
                apTypeCode = strArray[1];
            }
        }
        enqueueAccessPoints(apTypeCode);
    }

    private void enqueueAccessPoints(final String apTypeCode) {
        StaticDataProvider sdp = staticDataService.getData();
        ApType apType = null;
        if (StringUtils.isNotEmpty(apTypeCode)) {
            apType = sdp.getApTypeByCode(apTypeCode);
        }

        if (accessPoints == null) {
            accessPoints = new HashSet<>();
        }

        if(apType == null) {
            List<Integer> accessPointList = accessPointRepository.findActiveAccessPointIds();
            if (CollectionUtils.isNotEmpty(accessPointList)) {
                accessPoints.addAll(accessPointList);
            }
        } else {
            List<ApType> apTypeList = findTreeApTypes(apType.getApTypeId());
            if (CollectionUtils.isNotEmpty(apTypeList)) {
                List<Integer> accessPointList = accessPointRepository.findActiveAccessPointIdsByApTypes(apTypeList);
                if (CollectionUtils.isNotEmpty(accessPointList)) {
                    accessPoints.addAll(accessPointList);
                }
            }
        }
    }

    private List<ApType> findTreeApTypes(final Integer id) {
        List<ApType> apTypes = apTypeRepository.findAll();
        return findTreeApTypes(apTypes, id);
    }

    private List<ApType> findTreeApTypes(final List<ApType> apTypes, final Integer id) {
        ApType parent = getApTypeById(apTypes, id);
        Set<ApType> result = new HashSet<>();
        if (parent != null) {
            result.add(parent);
            for (ApType item : apTypes) {
                if (parent.equals(item.getParentApType())) {
                    result.addAll(findTreeApTypes(apTypes, item.getApTypeId()));
                }
            }
        }
        return new ArrayList<>(result);
    }

    @Nullable
    private ApType getApTypeById(final List<ApType> apTypes, final Integer id) {
        for (ApType apType : apTypes) {
            if (apType.getApTypeId().equals(id)) {
                return apType;
            }
        }
        return null;
    }


    public Boolean getTesting() {
        return testing;
    }

    public void setTesting(final Boolean testing) {
        this.testing = testing;
    }


}

