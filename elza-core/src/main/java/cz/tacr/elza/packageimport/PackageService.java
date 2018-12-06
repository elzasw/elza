package cz.tacr.elza.packageimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
import java.util.zip.ZipOutputStream;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotNull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.FileSystemUtils;

import cz.tacr.elza.api.UseUnitdateEnum;
import cz.tacr.elza.api.enums.ParRelationClassTypeRepeatabilityEnum;
import cz.tacr.elza.api.enums.UIPartyGroupTypeEnum;
import cz.tacr.elza.bulkaction.BulkActionConfigManager;
import cz.tacr.elza.common.AutoDeletingTempFile;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.domain.ApExternalIdType;
import cz.tacr.elza.domain.ApRule;
import cz.tacr.elza.domain.ApRuleSystem;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParPartyTypeComplementType;
import cz.tacr.elza.domain.ParPartyTypeRelation;
import cz.tacr.elza.domain.ParRegistryRole;
import cz.tacr.elza.domain.ParRelationClassType;
import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.ParRelationType;
import cz.tacr.elza.domain.ParRelationTypeRoleType;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.domain.RulActionRecommended;
import cz.tacr.elza.domain.RulArrangementExtension;
import cz.tacr.elza.domain.RulArrangementRule;
import cz.tacr.elza.domain.RulComponent;
import cz.tacr.elza.domain.RulExtensionRule;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeAction;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPackageDependency;
import cz.tacr.elza.domain.RulPolicyType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.domain.UIPartyGroup;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UISettings.EntityType;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.interpi.service.InterpiService;
import cz.tacr.elza.packageimport.RuleUpdateContext.RuleState;
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
import cz.tacr.elza.packageimport.xml.ComplementType;
import cz.tacr.elza.packageimport.xml.ComplementTypes;
import cz.tacr.elza.packageimport.xml.ExtensionRule;
import cz.tacr.elza.packageimport.xml.ExtensionRules;
import cz.tacr.elza.packageimport.xml.ExternalIdType;
import cz.tacr.elza.packageimport.xml.ExternalIdTypes;
import cz.tacr.elza.packageimport.xml.ItemSpec;
import cz.tacr.elza.packageimport.xml.ItemSpecs;
import cz.tacr.elza.packageimport.xml.ItemType;
import cz.tacr.elza.packageimport.xml.ItemTypes;
import cz.tacr.elza.packageimport.xml.OutputType;
import cz.tacr.elza.packageimport.xml.OutputTypes;
import cz.tacr.elza.packageimport.xml.PackageDependency;
import cz.tacr.elza.packageimport.xml.PackageInfo;
import cz.tacr.elza.packageimport.xml.PartyGroup;
import cz.tacr.elza.packageimport.xml.PartyGroups;
import cz.tacr.elza.packageimport.xml.PartyNameFormType;
import cz.tacr.elza.packageimport.xml.PartyNameFormTypes;
import cz.tacr.elza.packageimport.xml.PartyTypeComplementType;
import cz.tacr.elza.packageimport.xml.PartyTypeComplementTypes;
import cz.tacr.elza.packageimport.xml.PartyTypeRelation;
import cz.tacr.elza.packageimport.xml.PartyTypeRelations;
import cz.tacr.elza.packageimport.xml.PolicyType;
import cz.tacr.elza.packageimport.xml.PolicyTypes;
import cz.tacr.elza.packageimport.xml.RegistryRole;
import cz.tacr.elza.packageimport.xml.RegistryRoles;
import cz.tacr.elza.packageimport.xml.RelationClassType;
import cz.tacr.elza.packageimport.xml.RelationClassTypes;
import cz.tacr.elza.packageimport.xml.RelationRoleType;
import cz.tacr.elza.packageimport.xml.RelationRoleTypes;
import cz.tacr.elza.packageimport.xml.RelationType;
import cz.tacr.elza.packageimport.xml.RelationTypeRoleType;
import cz.tacr.elza.packageimport.xml.RelationTypeRoleTypes;
import cz.tacr.elza.packageimport.xml.RelationTypes;
import cz.tacr.elza.packageimport.xml.Rule;
import cz.tacr.elza.packageimport.xml.RuleSetXml;
import cz.tacr.elza.packageimport.xml.RuleSets;
import cz.tacr.elza.packageimport.xml.RuleSystem;
import cz.tacr.elza.packageimport.xml.RuleSystems;
import cz.tacr.elza.packageimport.xml.Setting;
import cz.tacr.elza.packageimport.xml.SettingBase;
import cz.tacr.elza.packageimport.xml.SettingFavoriteItemSpecs;
import cz.tacr.elza.packageimport.xml.SettingFundViews;
import cz.tacr.elza.packageimport.xml.SettingGridView;
import cz.tacr.elza.packageimport.xml.SettingRecord;
import cz.tacr.elza.packageimport.xml.SettingStructureTypes;
import cz.tacr.elza.packageimport.xml.SettingTypeGroups;
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
import cz.tacr.elza.repository.ApRuleRepository;
import cz.tacr.elza.repository.ApRuleSystemRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.ArrangementExtensionRepository;
import cz.tacr.elza.repository.ArrangementRuleRepository;
import cz.tacr.elza.repository.ComplementTypeRepository;
import cz.tacr.elza.repository.ComponentRepository;
import cz.tacr.elza.repository.ExtensionRuleRepository;
import cz.tacr.elza.repository.ItemSpecRegisterRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeActionRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.OutputDefinitionRepository;
import cz.tacr.elza.repository.OutputResultRepository;
import cz.tacr.elza.repository.OutputTypeRepository;
import cz.tacr.elza.repository.PackageDependencyRepository;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.repository.Packaging;
import cz.tacr.elza.repository.PartyNameFormTypeRepository;
import cz.tacr.elza.repository.PartyRelationClassTypeRepository;
import cz.tacr.elza.repository.PartyTypeComplementTypeRepository;
import cz.tacr.elza.repository.PartyTypeRelationRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.PolicyTypeRepository;
import cz.tacr.elza.repository.RegistryRoleRepository;
import cz.tacr.elza.repository.RelationRoleTypeRepository;
import cz.tacr.elza.repository.RelationTypeRepository;
import cz.tacr.elza.repository.RelationTypeRoleTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.SettingsRepository;
import cz.tacr.elza.repository.StructureDefinitionRepository;
import cz.tacr.elza.repository.StructureExtensionDefinitionRepository;
import cz.tacr.elza.repository.StructuredTypeExtensionRepository;
import cz.tacr.elza.repository.StructuredTypeRepository;
import cz.tacr.elza.repository.TemplateRepository;
import cz.tacr.elza.repository.UIPartyGroupRepository;
import cz.tacr.elza.service.CacheService;
import cz.tacr.elza.service.StructObjService;
import cz.tacr.elza.service.StructObjValueService;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.event.CacheInvalidateEvent;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.ActionEvent;
import cz.tacr.elza.service.eventnotification.events.EventType;


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
     * typy outputů
     */
    public static final String OUTPUT_TYPE_XML = "rul_output_type.xml";

    /**
     * Osoby... TODO
     */
    public static final String RELATION_ROLE_TYPE_XML = "par_relation_role_type.xml";
    public static final String PARTY_NAME_FORM_TYPE_XML = "par_party_name_form_type.xml";
    public static final String RELATION_CLASS_TYPE_XML = "par_relation_class_type.xml";
    public static final String COMPLEMENT_TYPE_XML = "par_complement_type.xml";
    public static final String PARTY_GROUP_XML = "ui_party_group.xml";
    public static final String PARTY_TYPE_COMPLEMENT_TYPE_XML = "par_party_type_complement_type.xml";
    public static final String PARTY_TYPE_RELATION_XML = "par_party_type_relation.xml";
    public static final String RELATION_TYPE_XML = "par_relation_type.xml";
    public static final String RELATION_TYPE_ROLE_TYPE_XML = "par_relation_type_role_type.xml";
    public static final String REGISTRY_ROLE_XML = "par_registry_role.xml";
    public static final String SETTING_XML = "ui_setting.xml";

    /**
     * typy externích identifikátorů
     */
    public static final String EXTERNAL_ID_TYPE_XML = "ap_external_id_type.xml";

    /**
     * typy fragmentů
     */
    public static final String FRAGMENT_TYPE_XML = "ap_fragment_type.xml";

    /**
     * pravidla popisu ap
     */
    public static final String RULE_SYSTEM_XML = "ap_rule_system.xml";

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
     * adresář pro pravidla v zip
     */
    static public final String ZIP_DIR_RULES = "rules";

    /**
     * adresář pro groovy v zip
     */
    static public final String ZIP_DIR_SCRIPTS = "scripts";

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
    private ItemSpecRegisterRepository itemSpecRegisterRepository;

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
    private OutputDefinitionRepository outputDefinitionRepository;

    @Autowired
    private PartyTypeRepository partyTypeRepository;

    @Autowired
    private RelationRoleTypeRepository relationRoleTypeRepository;

    @Autowired
    private PartyNameFormTypeRepository partyNameFormTypeRepository;

    @Autowired
    private PartyRelationClassTypeRepository partyRelationClassTypeRepository;

    @Autowired
    private ComplementTypeRepository complementTypeRepository;

    @Autowired
    private UIPartyGroupRepository uiPartyGroupRepository;

    @Autowired
    private PartyTypeComplementTypeRepository partyTypeComplementTypeRepository;

    @Autowired
    private RelationTypeRepository relationTypeRepository;

    @Autowired
    private PartyTypeRelationRepository partyTypeRelationRepository;

    @Autowired
    private RelationTypeRoleTypeRepository relationTypeRoleTypeRepository;

    @Autowired
    private ApTypeRepository apTypeRepository;

    @Autowired
    private RegistryRoleRepository registryRoleRepository;

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private InterpiService interpiService;

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
    private ApRuleSystemRepository ruleSystemRepository;

    @Autowired
    private ApRuleRepository ruleRepository;

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
    private NodeCacheService nodeCacheService;

    /**
     * Provede import balíčku.
     *
     * @param file
     *            soubor balíčku
     * 
     *            Note: only one package can be imported at a time
     */
    @Transactional
	@AuthMethod(permission = { UsrPermission.Permission.ADMIN })
    synchronized public void importPackage(final File file) {
        importPackageInternal(file);
    }

    @Transactional
    public void importPackageInternal(final File file) {

        // read package and do basic checks
        PackageContext pkgCtx = new PackageContext(resourcePathResolver);

        File oldPackageDir = null;
        File packageDir = null;
        try {
            pkgCtx.init(file);

            // stop services and prepare update
            preImportPackage();

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
        }
        finally {
            if (pkgCtx != null) {
                // start services after import
                postImportPackage(pkgCtx);

                pkgCtx.close();
                pkgCtx = null;
            }
        }

        if (oldPackageDir != null) {
            FileSystemUtils.deleteRecursively(oldPackageDir);
        }

    }

    private void preImportPackage() {
        logger.info("Stoping services before package update");

        structObjValueService.stopGenerator();
        // odebrání používaných groovy scritpů
        cacheService.resetCache(CacheInvalidateEvent.Type.GROOVY);
    }

    private void postImportPackage(PackageContext pkgCtx) {
        if (pkgCtx.isSyncNodeCache()) {
            nodeCacheService.syncCache();
        }

        logger.info("Package was updated. Code: {}, Version: {}", pkgCtx.getPackageInfo().getCode(),
                    pkgCtx.getPackageInfo().getVersion());

        // add request to regenerate structObjs
        Set<String> codes = pkgCtx.getRegenerateStructureTypes();
        List<RulStructuredType> revalidateStructureTypes = new ArrayList<>(codes.size());
        for (String code : codes) {
            RulStructuredType structType = this.structureTypeRepository.findByCode(code);
            revalidateStructureTypes.add(structType);
        }

        structObjValueService.addToValidateByTypes(revalidateStructureTypes);

        structObjValueService.startGenerator();

        logger.info("Services were restarted after package update");
    }

    public void importPackageInternal(final PackageContext pkgCtx) throws IOException {

        // OSOBY ---------------------------------------------------------------------------------------------------
        importPersonTypes(pkgCtx);
        // END OSOBY -----------------------------------------------------------------------------------------------

        processRuleSets(pkgCtx);

        List<RulStructuredType> rulStructuredTypes = processStructureTypes(pkgCtx);
        processStructureDefinitions(pkgCtx, rulStructuredTypes);

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
                    this.structureService);
            steu.run(pkgCtx);
            processPackageActions(ruc);
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
        List<UISettings> globalSettings = createUISettings(settings, rulPackage, null, null);
        uiSettings.addAll(globalSettings);

        processSettings(uiSettings, rulPackage);

        // END NASTAVENÍ -------------------------------------------------------------------------------------------

        // AP ------------------------------------------------------------------------------------------------------

        ExternalIdTypes externalIdTypes = pkgCtx.convertXmlStreamToObject(ExternalIdTypes.class,
                                                                          EXTERNAL_ID_TYPE_XML);
        processExternalIdTypes(externalIdTypes, rulPackage);

        // END AP --------------------------------------------------------------------------------------------------

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

    private void importPersonTypes(PackageContext pkgCtx) throws IOException {
        RulPackage rulPackage = pkgCtx.getPackage();

        List<ApRuleSystem> apRuleSystems = processRuleSystems(pkgCtx);
        List<ParPartyType> parPartyTypes = partyTypeRepository.findAll();

        RelationRoleTypes relationRoleTypes = pkgCtx.convertXmlStreamToObject(RelationRoleTypes.class,
                                                                              RELATION_ROLE_TYPE_XML);
        List<ParRelationRoleType> parRelationRoleTypes = processRelationRoleTypes(relationRoleTypes, rulPackage);

        PartyNameFormTypes partyNameFormTypes = pkgCtx.convertXmlStreamToObject(PartyNameFormTypes.class,
                                                                                PARTY_NAME_FORM_TYPE_XML);
        processPartyNameFormTypes(partyNameFormTypes, rulPackage);

        RelationClassTypes relationClassTypes = pkgCtx.convertXmlStreamToObject(RelationClassTypes.class,
                                                                                RELATION_CLASS_TYPE_XML);
        List<ParRelationClassType> parRelationClassTypes = processRelationClassTypes(relationClassTypes, rulPackage);

        ComplementTypes complementTypes = pkgCtx.convertXmlStreamToObject(ComplementTypes.class,
                                                                          COMPLEMENT_TYPE_XML);
        List<ParComplementType> parComplementTypes = processComplementTypes(complementTypes, rulPackage);

        PartyGroups partyGroups = pkgCtx.convertXmlStreamToObject(PartyGroups.class, PARTY_GROUP_XML);
        processPartyGroups(partyGroups, rulPackage, parPartyTypes);

        PartyTypeComplementTypes partyTypeComplementTypes = pkgCtx
                .convertXmlStreamToObject(PartyTypeComplementTypes.class, PARTY_TYPE_COMPLEMENT_TYPE_XML);
        processPartyTypeComplementTypes(partyTypeComplementTypes, rulPackage, parComplementTypes, parPartyTypes);

        RelationTypes relationTypes = pkgCtx.convertXmlStreamToObject(RelationTypes.class,
                                                                      RELATION_TYPE_XML);
        List<ParRelationType> parRelationTypes = processRelationTypes(relationTypes, rulPackage, parRelationClassTypes);

        PartyTypeRelations partyTypeRelations = pkgCtx.convertXmlStreamToObject(PartyTypeRelations.class,
                                                                                PARTY_TYPE_RELATION_XML);
        processPartyTypeRelations(partyTypeRelations, rulPackage, parRelationTypes, parPartyTypes);

        RelationTypeRoleTypes relationTypeRoleTypes = pkgCtx
                .convertXmlStreamToObject(RelationTypeRoleTypes.class, RELATION_TYPE_ROLE_TYPE_XML);
        processRelationTypeRoleTypes(relationTypeRoleTypes, rulPackage, parRelationRoleTypes, parRelationTypes);

        APTypeUpdater apTypeUpdater = new APTypeUpdater(apTypeRepository, registryRoleRepository,
                this.accessPointRepository, parPartyTypes, apRuleSystems, staticDataService.getData());
        apTypeUpdater.run(pkgCtx);
        List<ApType> apTypes = apTypeUpdater.getApTypes();

        RegistryRoles registryRoles = pkgCtx.convertXmlStreamToObject(RegistryRoles.class,
                                                                      REGISTRY_ROLE_XML);
        processRegistryRoles(registryRoles, rulPackage, parRelationRoleTypes, apTypes);
    }

    private List<ApRuleSystem> processRuleSystems(final PackageContext pkgCtx) throws IOException {
        RuleSystems ruleSystems = PackageUtils.convertXmlStreamToObject(RuleSystems.class,
                                                                        pkgCtx.getByteStream(RULE_SYSTEM_XML));

        List<ApRuleSystem> apRuleSystems = ruleSystemRepository.findByRulPackage(pkgCtx.getPackage());
        Map<Integer, List<ApRule>> typeRules = apRuleSystems.isEmpty()
                ? Collections.emptyMap()
                : ruleRepository.findByRuleSystemIn(apRuleSystems).stream()
                .collect(Collectors.groupingBy(ApRule::getRuleSystemId));

        List<ApRuleSystem> apRuleSystemsNew = new ArrayList<>();
        List<ApRule> apRulesNew = new ArrayList<>();

        if (ruleSystems != null && !CollectionUtils.isEmpty(ruleSystems.getRuleSystems())) {
            for (RuleSystem ruleSystem : ruleSystems.getRuleSystems()) {
                ApRuleSystem item = findEntity(apRuleSystems, ruleSystem.getCode(), ApRuleSystem::getCode);
                if (item == null) {
                    item = new ApRuleSystem();
                }
                convertApRuleSystem(pkgCtx.getPackage(), ruleSystem, item);
                ruleSystemRepository.save(item);
                List<ApRule> apRules = typeRules.get(item.getRuleSystemId());
                apRules = mergeApRules(apRules == null ? new ArrayList<>() : apRules, ruleSystem.getRules(), item,
                                       pkgCtx);
                apRulesNew.addAll(apRules);
                apRuleSystemsNew.add(item);
            }
        }

        List<ApRuleSystem> apRuleSystemsDelete = new ArrayList<>(apRuleSystems);
        apRuleSystemsDelete.removeAll(apRuleSystemsNew);

        List<RulComponent> componentsDelete = new ArrayList<>();
        List<ApRule> rulesDelete = new ArrayList<>();
        for (ApRuleSystem apRuleSystem : apRuleSystemsDelete) {
            List<ApRule> apRules = typeRules.get(apRuleSystem.getRuleSystemId());
            for (ApRule apRule : apRules) {
                RulComponent component = apRule.getComponent();
                componentsDelete.add(component);
                deleteFile(pkgCtx.getDir(apRule), component.getFilename());
            }
            rulesDelete.addAll(apRules);
        }
        ruleRepository.delete(rulesDelete);
        componentRepository.delete(componentsDelete);
        ruleSystemRepository.delete(apRuleSystemsDelete);

        try {
            for (ApRule apRule : apRulesNew) {
                RulComponent component = apRule.getComponent();
                updateComponentHash(pkgCtx, component, pkgCtx.getDir(apRule), getZipDir(apRule));
            }
        } catch (IOException e) {
            throw new SystemException(e);
        }

        apRuleSystems.addAll(apRuleSystemsNew);
        return apRuleSystems;
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

    private List<ApRule> mergeApRules(final List<ApRule> apRules, final List<Rule> rules,
                                      final ApRuleSystem apRuleSystem, final PackageContext pkgCtx)
            throws IOException {
        Validate.notEmpty(rules);
        Validate.notNull(apRules);
        List<ApRule> apRulesNew = new ArrayList<>();
        for (Rule rule : rules) {
            ApRule apRule = findEntity(apRules, rule.getRuleType(), ApRule::getRuleType);
            if (apRule == null) {
                apRule = new ApRule();
                RulComponent component = new RulComponent();
                component.setFilename(rule.getFilename());
                apRule.setComponent(component);
            } else {
                RulComponent component = apRule.getComponent();
                component.setFilename(rule.getFilename());
            }
            apRule.setRuleSystem(apRuleSystem);
            apRule.setRuleType(rule.getRuleType());
            apRulesNew.add(apRule);
        }

        List<ApRule> apRulesDelete = new ArrayList<>(apRules);
        apRulesDelete.removeAll(apRulesNew);
        for (ApRule apRule : apRulesDelete) {
            deleteFile(pkgCtx.getDir(apRule), apRule.getComponent().getFilename());
        }
        List<RulComponent> componentsDelete = apRulesDelete.stream().map(ApRule::getComponent).collect(Collectors.toList());

        ruleRepository.delete(apRulesDelete);
        componentRepository.delete(componentsDelete);

        List<RulComponent> components = apRulesNew.stream().map(ApRule::getComponent).collect(Collectors.toList());
        componentRepository.save(components);

        return ruleRepository.save(apRulesNew);
    }

    private void convertApRuleSystem(final RulPackage rulPackage, final RuleSystem ruleSystem, final ApRuleSystem apRuleSystem) {
        apRuleSystem.setCode(ruleSystem.getCode());
        apRuleSystem.setRulPackage(rulPackage);
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

        apExternalIdTypesNew = externalIdTypeRepository.save(apExternalIdTypesNew);

        List<ApExternalIdType> apExternalIdTypesDelete = new ArrayList<>(apExternalIdTypes);
        apExternalIdTypesDelete.removeAll(apExternalIdTypesNew);

        externalIdTypeRepository.delete(apExternalIdTypesDelete);
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

    private List<RulStructureDefinition> processStructureDefinitions(final PackageContext puc,
                                                                     final List<RulStructuredType> rulStructureTypes) {
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
                rulStructureDefinitionsNew.add(item);
            }
        }

        rulStructureDefinitionsNew = structureDefinitionRepository.save(rulStructureDefinitionsNew);

        List<RulStructureDefinition> rulStructureDefinitionDelete = new ArrayList<>(rulStructureDefinitions);
        rulStructureDefinitionDelete.removeAll(rulStructureDefinitionsNew);

        List<RulComponent> rulComponentsDelete = rulStructureDefinitionDelete.stream().map(RulStructureDefinition::getComponent).collect(Collectors.toList());
        structureDefinitionRepository.delete(rulStructureDefinitionDelete);
        componentRepository.delete(rulComponentsDelete);

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
            case SERIALIZED_VALUE:
                return ZIP_DIR_SCRIPTS;
            default:
                throw new NotImplementedException("Def type: " + definition.getDefType());
        }
    }

    public String getZipDir(final ApRule rule) {
        switch (rule.getRuleType()) {
            case BODY_ITEMS:
            case NAME_ITEMS:
                return ZIP_DIR_RULES;
            case TEXT_GENERATOR:
            case MIGRATE:
                return ZIP_DIR_SCRIPTS;
            default:
                throw new NotImplementedException("Rule type: " + rule.getRuleType());
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

        newStructTypes = structureTypeRepository.save(newStructTypes);

        List<RulStructuredType> rulRuleDelete = new ArrayList<>(currStructTypes);
        rulRuleDelete.removeAll(newStructTypes);
        structureTypeRepository.delete(rulRuleDelete);

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
     * @param rulPackage not-null
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
            for (Iterator<UISettings> it = currSettings.iterator(); it.hasNext();) {
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

        settingsRepository.delete(currSettings);
    }

    private List<UISettings> createUISettings(final Settings settings,
                                              final RulPackage rulPackage,
                                              final RulRuleSet ruleSet,
                                              final List<RulItemType> rulItemTypes) {
        if (settings == null) {
            return Collections.emptyList();
        }

        List<UISettings> result = new ArrayList<>(settings.getSettings().size());

        for (Setting sett : settings.getSettings()) {
            UISettings uiSett = sett.createUISettings(rulPackage);

            if (uiSett.getEntityType() == EntityType.RULE) {
                uiSett.setEntityId(ruleSet.getRuleSetId());

            } else if (uiSett.getEntityType() == EntityType.ITEM_TYPE) {
                SettingFavoriteItemSpecs specs = (SettingFavoriteItemSpecs) sett;
                String specsCode = specs.getCode();

                RulItemType itemType = rulItemTypes.stream()
                        .filter(t -> t.getCode().equals(specsCode))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException("RulItemType s code=" + specsCode + " nenalezen", PackageCode.CODE_NOT_FOUND)
                                .set("code", specsCode).set("file", SETTING_XML));

                uiSett.setEntityId(itemType.getItemTypeId());
            }

            result.add(uiSett);
        }

        return result;
    }

    /**
     * Zpracování vztahů typu třídy.
     *
     * @param registryRoles        VO vztahy typu třídy
     * @param rulPackage           balíček
     * @param parRelationRoleTypes seznam rolí entit ve vztahu
     * @param apTypes     seznam typů rejstříků
     */
    private void processRegistryRoles(final RegistryRoles registryRoles,
                                      final RulPackage rulPackage,
                                      final List<ParRelationRoleType> parRelationRoleTypes,
                                      final List<ApType> apTypes) {
        List<ParRegistryRole> parRegistryRoles = registryRoleRepository.findByRulPackage(rulPackage);
        List<ParRegistryRole> parRegistryRolesNew = new ArrayList<>();

        if (registryRoles != null && !CollectionUtils.isEmpty(registryRoles.getRegistryRoles())) {
            for (RegistryRole registryRole : registryRoles.getRegistryRoles()) {
                ParRegistryRole parRegistryRole = findEntity(parRegistryRoles,
                        registryRole.getRegisterType(), registryRole.getRoleType(),
                        i -> i.getApType().getCode(), i -> i.getRoleType().getCode());
                if (parRegistryRole == null) {
                    parRegistryRole = new ParRegistryRole();
                }
                convertParRegistryRoles(rulPackage, registryRole, parRegistryRole, apTypes, parRelationRoleTypes);
                parRegistryRolesNew.add(parRegistryRole);
            }
        }

        parRegistryRolesNew = registryRoleRepository.save(parRegistryRolesNew);

        List<ParRegistryRole> parRegistryRolesDelete = new ArrayList<>(parRegistryRoles);
        parRegistryRolesDelete.removeAll(parRegistryRolesNew);
        registryRoleRepository.delete(parRegistryRolesDelete);
    }

    /**
     * Konverze VO -> DO.
     *
     * @param rulPackage            balíček
     * @param registryRole          typ vztahu - VO
     * @param parRegistryRole       typ vztahu - DO
     * @param apTypes      seznam typů rejstříků
     * @param parRelationRoleTypes  seznam rolí entit ve vztahu
     */
    private void convertParRegistryRoles(final RulPackage rulPackage,
                                         final RegistryRole registryRole,
                                         final ParRegistryRole parRegistryRole,
                                         final List<ApType> apTypes,
                                         final List<ParRelationRoleType> parRelationRoleTypes) {
        parRegistryRole.setRulPackage(rulPackage);
        ParRelationRoleType parRelationRoleType = findEntity(parRelationRoleTypes, registryRole.getRoleType(), ParRelationRoleType::getCode);
        if (parRelationRoleType == null) {
            throw new BusinessException("ParRelationRoleType s code=" + registryRole.getRoleType() + " nenalezen", PackageCode.CODE_NOT_FOUND).set("code", registryRole.getRoleType()).set("file", REGISTRY_ROLE_XML);
        }
        parRegistryRole.setRoleType(parRelationRoleType);
        ApType apType = findEntity(apTypes, registryRole.getRegisterType(), ApType::getCode);
        if (apType == null) {
            throw new BusinessException("ApType s code=" + registryRole.getRoleType() + " nenalezen", PackageCode.CODE_NOT_FOUND).set("code", registryRole.getRoleType()).set("file", REGISTRY_ROLE_XML);
        }
        parRegistryRole.setApType(apType);
    }


    /**
     * Zpracování entity.
     */
    private void processRelationTypeRoleTypes(@Nullable final RelationTypeRoleTypes relationTypeRoleTypes,
                                              @NotNull final RulPackage rulPackage,
                                              @NotNull final List<ParRelationRoleType> parRelationRoleTypes,
                                              @NotNull final List<ParRelationType> parRelationTypes) {

        List<ParRelationTypeRoleType> parRelationTypeRoleTypes = relationTypeRoleTypeRepository.findByRulPackage(rulPackage);
        List<ParRelationTypeRoleType> parRelationTypeRoleTypesNew = new ArrayList<>();

        if (relationTypeRoleTypes != null)
        {
        	List<RelationTypeRoleType> types = relationTypeRoleTypes.getRelationTypeRoleTypes();
        	if(types!=null)
        	{
        		// set to check if input items is not multiple times in collection
        		Set<Pair<String,String>> uniqueRelations = new HashSet<>();

        		for (RelationTypeRoleType relation : relationTypeRoleTypes.getRelationTypeRoleTypes()) {

        			// Check if exists
        			Pair<String, String> uniqueRelation = Pair.of(relation.getRelationType(), relation.getRoleType());
        			if(!uniqueRelations.add(uniqueRelation)) {
         				throw new BusinessException("Multiple relation with same types.", PackageCode.PARSE_ERROR)
         						.set("relationType", relation.getRelationType())
         						.set("roleType", relation.getRoleType())
         						.set("file", RELATION_TYPE_ROLE_TYPE_XML);
        			}

        			// Find in DB
        			ParRelationTypeRoleType parRelationTypeRoleType = findEntity(parRelationTypeRoleTypes,
        					relation.getRelationType(), relation.getRoleType(),
        					i -> i.getRelationType().getCode(), i -> i.getRoleType().getCode());
        			if (parRelationTypeRoleType == null) {
        				parRelationTypeRoleType = new ParRelationTypeRoleType();
        			}
        			convertParRelationTypeRoleTypes(rulPackage, relation, parRelationTypeRoleType, parRelationTypes, parRelationRoleTypes);
        			parRelationTypeRoleTypesNew.add(parRelationTypeRoleType);
        		}
        	}
        }

        parRelationTypeRoleTypesNew = relationTypeRoleTypeRepository.save(parRelationTypeRoleTypesNew);

        List<ParRelationTypeRoleType> parRelationTypeRoleTypesDelete = new ArrayList<>(parRelationTypeRoleTypes);
        parRelationTypeRoleTypesDelete.removeAll(parRelationTypeRoleTypesNew);
        relationTypeRoleTypeRepository.delete(parRelationTypeRoleTypesDelete);

        if (!parRelationTypeRoleTypesNew.isEmpty() || !parRelationTypeRoleTypesDelete.isEmpty()) {
            interpiService.deleteInvalidMappings();
        }
    }

    /**
     * Konverze VO -> DO.
     */
    private void convertParRelationTypeRoleTypes(final RulPackage rulPackage,
                                                 final RelationTypeRoleType relationTypeRoleType,
                                                 final ParRelationTypeRoleType parRelationTypeRoleType,
                                                 final List<ParRelationType> parRelationTypes,
                                                 final List<ParRelationRoleType> parRelationRoleTypes) {
        parRelationTypeRoleType.setRulPackage(rulPackage);
        parRelationTypeRoleType.setRepeatable(relationTypeRoleType.getRepeatable());
        //parRelationTypeRoleType.setViewOrder(relationTypeRoleType.getViewOrder());
        ParRelationType parRelationType = findEntity(parRelationTypes, relationTypeRoleType.getRelationType(), ParRelationType::getCode);
        if (parRelationType == null) {
            throw new BusinessException("ParRelationType s code=" + relationTypeRoleType.getRelationType() + " nenalezen", PackageCode.CODE_NOT_FOUND).set("code", relationTypeRoleType.getRelationType()).set("file", RELATION_TYPE_ROLE_TYPE_XML);
        }
        parRelationTypeRoleType.setRelationType(parRelationType);
        ParRelationRoleType parRelationRoleType = findEntity(parRelationRoleTypes, relationTypeRoleType.getRoleType(), ParRelationRoleType::getCode);
        if (parRelationRoleType == null) {
            throw new BusinessException("ParRelationRoleType s code=" + relationTypeRoleType.getRoleType() + " nenalezen", PackageCode.CODE_NOT_FOUND).set("code", relationTypeRoleType.getRoleType()).set("file", RELATION_TYPE_ROLE_TYPE_XML);
        }
        parRelationTypeRoleType.setRoleType(parRelationRoleType);
    }

    /**
     * Zpracování typů vztahu osob.
     */
    private void processPartyTypeRelations(@Nullable final PartyTypeRelations partyTypeRelations,
                                           @NotNull final RulPackage rulPackage,
                                           @NotNull final List<ParRelationType> parRelationTypes,
                                           @NotNull final List<ParPartyType> parPartyTypes) {
        List<ParPartyTypeRelation> parPartyTypeRelations = partyTypeRelationRepository.findByRulPackage(rulPackage);
        List<ParPartyTypeRelation> parPartyTypeRelationsNew = new ArrayList<>();

        if (partyTypeRelations != null && !CollectionUtils.isEmpty(partyTypeRelations.getPartyTypeRelations())) {
            for (PartyTypeRelation partyTypeRelation : partyTypeRelations.getPartyTypeRelations()) {
                ParPartyTypeRelation parPartyTypeRelation = findEntity(parPartyTypeRelations,
                        partyTypeRelation.getRelationType(), partyTypeRelation.getPartyType(),
                        i -> i.getRelationType().getCode(), i -> i.getPartyType().getCode());
                if (parPartyTypeRelation == null) {
                    parPartyTypeRelation = new ParPartyTypeRelation();
                }
                convertParPartyTypeRelations(rulPackage, partyTypeRelation, parPartyTypeRelation, parRelationTypes, parPartyTypes);
                parPartyTypeRelationsNew.add(parPartyTypeRelation);
            }
        }

        parPartyTypeRelationsNew = partyTypeRelationRepository.save(parPartyTypeRelationsNew);
        List<ParPartyTypeRelation> parPartyTypeRelationsDelete = new ArrayList<>(parPartyTypeRelations);
        parPartyTypeRelationsDelete.removeAll(parPartyTypeRelationsNew);
        partyTypeRelationRepository.delete(parPartyTypeRelationsDelete);
    }

    /**
     * Konverze VO -> DO.
     */
    private void convertParPartyTypeRelations(final RulPackage rulPackage,
                                              final PartyTypeRelation partyTypeRelation,
                                              final ParPartyTypeRelation parPartyTypeRelation,
                                              final List<ParRelationType> parRelationTypes,
                                              final List<ParPartyType> parPartyTypes) {
        parPartyTypeRelation.setRulPackage(rulPackage);
        parPartyTypeRelation.setRepeatable(partyTypeRelation.getRepeatable());
        parPartyTypeRelation.setViewOrder(partyTypeRelation.getViewOrder());
        parPartyTypeRelation.setName(partyTypeRelation.getName());
        ParRelationType parRelationType = findEntity(parRelationTypes, partyTypeRelation.getRelationType(), ParRelationType::getCode);
        if (parRelationType == null) {
            throw new BusinessException("ParRelationType s code=" + partyTypeRelation.getRelationType() + " nenalezen", PackageCode.CODE_NOT_FOUND).set("code", partyTypeRelation.getRelationType()).set("file", PARTY_TYPE_RELATION_XML);
        }
        parPartyTypeRelation.setRelationType(parRelationType);
        ParPartyType parPartyType = findEntity(parPartyTypes, partyTypeRelation.getPartyType(), ParPartyType::getCode);
        if (parPartyType == null) {
            throw new BusinessException("ParPartyType s code=" + partyTypeRelation.getPartyType() + " nenalezen", PackageCode.CODE_NOT_FOUND).set("code", partyTypeRelation.getPartyType()).set("file", PARTY_TYPE_RELATION_XML);
        }
        parPartyTypeRelation.setPartyType(parPartyType);
    }

    /**
     * Zpracování typů vztahů.
     */
    private List<ParRelationType> processRelationTypes(@Nullable final RelationTypes relationTypes,
                                                       @NotNull final RulPackage rulPackage,
                                                       @NotNull final List<ParRelationClassType> parRelationClassTypes) {
        List<ParRelationType> parRelationTypes = relationTypeRepository.findByRulPackage(rulPackage);
        List<ParRelationType> parRelationTypesNew = new ArrayList<>();

        if (relationTypes != null && !CollectionUtils.isEmpty(relationTypes.getRelationTypes())) {
            for (RelationType relationType : relationTypes.getRelationTypes()) {
                ParRelationType parRelationType = findEntity(parRelationTypes, relationType.getCode(), ParRelationType::getCode);
                if (parRelationType == null) {
                    parRelationType = new ParRelationType();
                }
                convertParRelationTypes(rulPackage, relationType, parRelationType, parRelationClassTypes);
                parRelationTypesNew.add(parRelationType);
            }
        }

        parRelationTypesNew = relationTypeRepository.save(parRelationTypesNew);

        List<ParRelationType> parRelationTypesDelete = new ArrayList<>(parRelationTypes);
        parRelationTypesDelete.removeAll(parRelationTypesNew);

        parRelationTypesDelete.forEach(partyTypeRelationRepository::deleteByRelationType);
        parRelationTypesDelete.forEach(relationTypeRoleTypeRepository::deleteByRelationType);

        relationTypeRepository.delete(parRelationTypesDelete);

        return parRelationTypesNew;
    }

    /**
     * Konverze VO -> DO.
     */
    private void convertParRelationTypes(final RulPackage rulPackage,
                                         final RelationType relationType,
                                         final ParRelationType parRelationType,
                                         final List<ParRelationClassType> parRelationClassTypes) {
        parRelationType.setRulPackage(rulPackage);
        parRelationType.setCode(relationType.getCode());
        parRelationType.setName(relationType.getName());
        parRelationType.setUseUnitdate(UseUnitdateEnum.valueOf(relationType.getUseUnitdate()));
        ParRelationClassType parRelationClassType = findEntity(parRelationClassTypes, relationType.getRelatioClassType(), ParRelationClassType::getCode);
        if (parRelationClassType == null) {
            throw new BusinessException("ParRelationClassType s code=" + relationType.getRelatioClassType() + " nenalezen", PackageCode.CODE_NOT_FOUND).set("code", relationType.getRelatioClassType()).set("file", RELATION_TYPE_XML);
        }
        parRelationType.setRelationClassType(parRelationClassType);
    }

    /**
     * Zpracování vazby M:N mezi typem osoby a typem doplňku jména.
     */
    private void processPartyTypeComplementTypes(@Nullable final PartyTypeComplementTypes partyTypeComplementTypes,
                                                 @NotNull final RulPackage rulPackage,
                                                 @NotNull final List<ParComplementType> parComplementTypes,
                                                 @NotNull final List<ParPartyType> parPartyTypes) {
        List<ParPartyTypeComplementType> parPartyTypeComplementTypes = partyTypeComplementTypeRepository.findByRulPackage(rulPackage);
        List<ParPartyTypeComplementType> parPartyTypeComplementTypesNew = new ArrayList<>();

        if (partyTypeComplementTypes != null && !CollectionUtils.isEmpty(partyTypeComplementTypes.getPartyTypeComplementTypes())) {
            for (PartyTypeComplementType partyTypeComplementType : partyTypeComplementTypes.getPartyTypeComplementTypes()) {
                ParPartyTypeComplementType parPartyTypeComplementType = findEntity(parPartyTypeComplementTypes,
                        partyTypeComplementType.getComplementType(), partyTypeComplementType.getPartyType(),
                        i -> i.getComplementType().getCode(), i -> i.getPartyType().getCode());
                if (parPartyTypeComplementType == null) {
                    parPartyTypeComplementType = new ParPartyTypeComplementType();
                }
                convertParPartyTypeComplementTypes(rulPackage, partyTypeComplementType, parPartyTypeComplementType, parComplementTypes, parPartyTypes);
                parPartyTypeComplementTypesNew.add(parPartyTypeComplementType);
            }
        }

        parPartyTypeComplementTypesNew = partyTypeComplementTypeRepository.save(parPartyTypeComplementTypesNew);

        List<ParPartyTypeComplementType> parPartyTypeComplementTypesDelete = new ArrayList<>(parPartyTypeComplementTypes);
        parPartyTypeComplementTypesDelete.removeAll(parPartyTypeComplementTypesNew);
        partyTypeComplementTypeRepository.delete(parPartyTypeComplementTypesDelete);
    }

    /**
     * Konverze VO -> DO.
     */
    private void convertParPartyTypeComplementTypes(final RulPackage rulPackage,
                                                    final PartyTypeComplementType partyTypeComplementType,
                                                    final ParPartyTypeComplementType parPartyTypeComplementType,
                                                    final List<ParComplementType> parComplementTypes,
                                                    final List<ParPartyType> parPartyTypes) {
        parPartyTypeComplementType.setRulPackage(rulPackage);
        ParComplementType parComplementType = findEntity(parComplementTypes, partyTypeComplementType.getComplementType(), ParComplementType::getCode);
        if (parComplementType == null) {
            throw new BusinessException("ParComplementType s code=" + partyTypeComplementType.getComplementType() + " nenalezen", PackageCode.CODE_NOT_FOUND).set("code", partyTypeComplementType.getComplementType()).set("file", PARTY_TYPE_COMPLEMENT_TYPE_XML);
        }
        parPartyTypeComplementType.setComplementType(parComplementType);
        ParPartyType parPartyType = findEntity(parPartyTypes, partyTypeComplementType.getPartyType(), ParPartyType::getCode);
        if (parPartyType == null) {
            throw new BusinessException("ParPartyType s code=" + partyTypeComplementType.getPartyType() + " nenalezen", PackageCode.CODE_NOT_FOUND).set("code", partyTypeComplementType.getPartyType()).set("file", PARTY_TYPE_COMPLEMENT_TYPE_XML);
        }
        parPartyTypeComplementType.setPartyType(parPartyType);
        parPartyTypeComplementType.setRepeatable(partyTypeComplementType.getRepeatable());
    }

    /**
     * Zpracování nastavení zobrazení formuláře pro osoby.
     */
    private void processPartyGroups(@Nullable final PartyGroups partyGroups,
                                    @NotNull final RulPackage rulPackage,
                                    @NotNull final List<ParPartyType> parPartyTypes) {
        List<UIPartyGroup> uiPartyGroups = uiPartyGroupRepository.findByRulPackage(rulPackage);
        List<UIPartyGroup> uiPartyGroupsNew = new ArrayList<>();

        if (partyGroups != null && !CollectionUtils.isEmpty(partyGroups.getPartyGroups())) {
            for (PartyGroup partyGroup : partyGroups.getPartyGroups()) {
                UIPartyGroup uiPartyGroup = findEntity(uiPartyGroups, partyGroup.getCode(), UIPartyGroup::getCode);
                if (uiPartyGroup == null) {
                    uiPartyGroup = new UIPartyGroup();
                }
                convertUIPartyGroups(rulPackage, partyGroup, uiPartyGroup, parPartyTypes);
                uiPartyGroupsNew.add(uiPartyGroup);
            }
        }

        uiPartyGroupsNew = uiPartyGroupRepository.save(uiPartyGroupsNew);

        List<UIPartyGroup> uiPartyGroupsDelete = new ArrayList<>(uiPartyGroups);
        uiPartyGroupsDelete.removeAll(uiPartyGroupsNew);
        uiPartyGroupRepository.delete(uiPartyGroupsDelete);
    }

    /**
     * Konverze VO -> DO.
     */
    private void convertUIPartyGroups(final RulPackage rulPackage,
                                      final PartyGroup partyGroup,
                                      final UIPartyGroup parComplementType,
                                      final List<ParPartyType> parPartyTypes) {
        parComplementType.setRulPackage(rulPackage);
        parComplementType.setCode(partyGroup.getCode());
        parComplementType.setName(partyGroup.getName());
        parComplementType.setViewOrder(partyGroup.getViewOrder());
        parComplementType.setType(UIPartyGroupTypeEnum.valueOf(partyGroup.getType()));
        parComplementType.setContentDefinition(partyGroup.getContentDefinitionsString());
        if (partyGroup.getPartyType() != null) {
            ParPartyType parPartyType = findEntity(parPartyTypes, partyGroup.getPartyType(), ParPartyType::getCode);
            if (parPartyType == null) {
                throw new BusinessException("ParPartyType s code=" + partyGroup.getPartyType() + " nenalezen", PackageCode.CODE_NOT_FOUND).set("code", partyGroup.getPartyType());
            }
            parComplementType.setPartyType(parPartyType);
        }
    }

    /**
     * Zpracování typů doplňků jmen osob.
     */
    private List<ParComplementType> processComplementTypes(@Nullable final ComplementTypes complementTypes,
                                                           @NotNull final RulPackage rulPackage) {
        List<ParComplementType> parComplementTypes = complementTypeRepository.findByRulPackage(rulPackage);
        List<ParComplementType> parComplementTypesNew = new ArrayList<>();

        if (complementTypes != null && !CollectionUtils.isEmpty(complementTypes.getComplementTypes())) {
            for (ComplementType complementType : complementTypes.getComplementTypes()) {
                ParComplementType parComplementType = findEntity(parComplementTypes, complementType.getCode(), ParComplementType::getCode);
                if (parComplementType == null) {
                    parComplementType = new ParComplementType();
                }
                convertParComplementTypes(rulPackage, complementType, parComplementType);
                parComplementTypesNew.add(parComplementType);
            }
        }

        parComplementTypesNew = complementTypeRepository.save(parComplementTypesNew);

        List<ParComplementType> parComplementTypesDelete = new ArrayList<>(parComplementTypes);
        parComplementTypesDelete.removeAll(parComplementTypesNew);

        parComplementTypesDelete.forEach(partyTypeComplementTypeRepository::deleteByComplementType);

        complementTypeRepository.delete(parComplementTypesDelete);

        return parComplementTypesNew;
    }

    /**
     * Konverze VO -> DO.
     */
    private void convertParComplementTypes(final RulPackage rulPackage,
                                           final ComplementType complementType,
                                           final ParComplementType parComplementType) {
        parComplementType.setRulPackage(rulPackage);
        parComplementType.setCode(complementType.getCode());
        parComplementType.setName(complementType.getName());
        parComplementType.setViewOrder(complementType.getViewOrder());
    }

    /**
     * Zpracování vztah typu třídy.
     */
    private List<ParRelationClassType> processRelationClassTypes(@Nullable final RelationClassTypes relationClassTypes,
                                                                 @NotNull final RulPackage rulPackage) {
        List<ParRelationClassType> parRelationClassTypes = partyRelationClassTypeRepository.findByRulPackage(rulPackage);
        List<ParRelationClassType> parRelationClassTypesNew = new ArrayList<>();

        if (relationClassTypes != null && !CollectionUtils.isEmpty(relationClassTypes.getRelationClassTypes())) {
            for (RelationClassType relationClassType : relationClassTypes.getRelationClassTypes()) {
                ParRelationClassType parRelationClassType = findEntity(parRelationClassTypes, relationClassType.getCode(), ParRelationClassType::getCode);
                if (parRelationClassType == null) {
                    parRelationClassType = new ParRelationClassType();
                }
                convertParRelationClassTypes(rulPackage, relationClassType, parRelationClassType);
                parRelationClassTypesNew.add(parRelationClassType);
            }
        }

        parRelationClassTypesNew = partyRelationClassTypeRepository.save(parRelationClassTypesNew);

        List<ParRelationClassType> parRelationClassTypesDelete = new ArrayList<>(parRelationClassTypes);
        parRelationClassTypesDelete.removeAll(parRelationClassTypesNew);

        parRelationClassTypesDelete.forEach(relationTypeRepository::deleteByRelationClassType);

        partyRelationClassTypeRepository.delete(parRelationClassTypesDelete);
        return parRelationClassTypesNew;
    }

    /**
     * Konverze VO -> DO.
     */
    private void convertParRelationClassTypes(final RulPackage rulPackage,
                                              final RelationClassType relationRoleType,
                                              final ParRelationClassType parRelationRoleType) {
        parRelationRoleType.setRulPackage(rulPackage);
        parRelationRoleType.setName(relationRoleType.getName());
        parRelationRoleType.setCode(relationRoleType.getCode());
        parRelationRoleType.setRepeatability(ParRelationClassTypeRepeatabilityEnum.valueOf(relationRoleType.getRepeatability()));
    }

    /**
     * Zpracování TODO
     */
    private void processPartyNameFormTypes(@Nullable final PartyNameFormTypes partyNameFormTypes,
                                           @NotNull final RulPackage rulPackage) {
        List<ParPartyNameFormType> parPartyNameFormTypes = partyNameFormTypeRepository.findByRulPackage(rulPackage);
        List<ParPartyNameFormType> parPartyNameFormTypesNew = new ArrayList<>();

        if (partyNameFormTypes != null && !CollectionUtils.isEmpty(partyNameFormTypes.getPartyNameFormTypes())) {
            for (PartyNameFormType partyNameFormType : partyNameFormTypes.getPartyNameFormTypes()) {
                ParPartyNameFormType parRelationRoleType = findEntity(parPartyNameFormTypes, partyNameFormType.getCode(), ParPartyNameFormType::getCode);
                if (parRelationRoleType == null) {
                    parRelationRoleType = new ParPartyNameFormType();
                }
                convertParPartyNameFormTypes(rulPackage, partyNameFormType, parRelationRoleType);
                parPartyNameFormTypesNew.add(parRelationRoleType);
            }
        }

        parPartyNameFormTypesNew = partyNameFormTypeRepository.save(parPartyNameFormTypesNew);

        List<ParPartyNameFormType> parPartyNameFormTypesDelete = new ArrayList<>(parPartyNameFormTypes);
        parPartyNameFormTypesDelete.removeAll(parPartyNameFormTypesNew);
        partyNameFormTypeRepository.delete(parPartyNameFormTypesDelete);
    }

    /**
     * Konverze VO -> DO.
     */
    private void convertParPartyNameFormTypes(final RulPackage rulPackage,
                                              final PartyNameFormType partyNameFormType,
                                              final ParPartyNameFormType parPartyNameFormType) {
        parPartyNameFormType.setCode(partyNameFormType.getCode());
        parPartyNameFormType.setName(partyNameFormType.getName());
        parPartyNameFormType.setRulPackage(rulPackage);
    }

    /**
     * Zpracování vztah typu tříd.
     */
    private List<ParRelationRoleType> processRelationRoleTypes(@Nullable final RelationRoleTypes relationRoleTypes,
                                                               @NotNull final RulPackage rulPackage) {

        List<ParRelationRoleType> parRelationRoleTypes = relationRoleTypeRepository.findByRulPackage(rulPackage);
        List<ParRelationRoleType> parRelationRoleTypesNew = new ArrayList<>();

        if (relationRoleTypes != null && !CollectionUtils.isEmpty(relationRoleTypes.getRelationRoleTypes())) {
            for (RelationRoleType relationRoleType : relationRoleTypes.getRelationRoleTypes()) {
                ParRelationRoleType parRelationRoleType = findEntity(parRelationRoleTypes, relationRoleType.getCode(), ParRelationRoleType::getCode);
                if (parRelationRoleType == null) {
                    parRelationRoleType = new ParRelationRoleType();
                }
                convertParRelationRoleTypes(rulPackage, relationRoleType, parRelationRoleType);
                parRelationRoleTypesNew.add(parRelationRoleType);
            }
        }

        parRelationRoleTypesNew = relationRoleTypeRepository.save(parRelationRoleTypesNew);

        List<ParRelationRoleType> parRelationRoleTypesDelete = new ArrayList<>(parRelationRoleTypes);
        parRelationRoleTypesDelete.removeAll(parRelationRoleTypesNew);

        parRelationRoleTypesDelete.forEach(relationTypeRoleTypeRepository::deleteByRoleType);
        parRelationRoleTypesDelete.forEach(registryRoleRepository::deleteByRoleType);

        relationRoleTypeRepository.delete(parRelationRoleTypesDelete);
        return parRelationRoleTypesNew;
    }

    /**
     * Konverze VO -> DO.
     */
    private void convertParRelationRoleTypes(@NotNull final RulPackage rulPackage,
                                             @NotNull final RelationRoleType relationRoleType,
                                             @NotNull final ParRelationRoleType parRelationRoleType) {
        parRelationRoleType.setCode(relationRoleType.getCode());
        parRelationRoleType.setName(relationRoleType.getName());
        parRelationRoleType.setRulPackage(rulPackage);
    }

    /**
     * Generická metoda pro vyhledání v seznamu entit podle definované metody.
     *
     * @param list      seznam prohledávaných entit
     * @param find      co hledán v entitě
     * @param function  metoda, jakou hledám v entitě
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
     * @param policyTypes VO policy
     * @param rulPackage  balíček
     * @param rulRuleSet  pravidelo
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

        rulPolicyTypesNew = policyTypeRepository.save(rulPolicyTypesNew);

        List<RulPolicyType> rulPolicyTypesDelete = new ArrayList<>(rulPolicyTypesNew);
        rulPolicyTypesDelete.removeAll(rulPolicyTypesNew);
        policyTypeRepository.delete(rulPolicyTypesDelete);
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
     *  @param rulPackage    balíček
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
     * @param ruc
     *            importovaných seznam pravidel
     * @return seznam pravidel
     */
    private List<RulArrangementRule> processArrangementRules(final RuleUpdateContext ruc)
    {
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

        rulRuleNew = arrangementRuleRepository.save(rulRuleNew);

        List<RulArrangementRule> rulRuleDelete = new ArrayList<>(rulPackageRules);
        rulRuleDelete.removeAll(rulRuleNew);
        List<RulComponent> rulComponentsDelete = rulRuleDelete.stream().map(RulArrangementRule::getComponent).collect(Collectors.toList());
        arrangementRuleRepository.delete(rulRuleDelete);
        componentRepository.delete(rulComponentsDelete);

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
     * @param arrangementExtensions   importované definice rozšíření
     * @param rulPackage     balíček
     * @param rulRuleSet     pravidlo
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

        rulArrangementExtensionsNew = arrangementExtensionRepository.save(rulArrangementExtensionsNew);

        List<RulArrangementExtension> rulArrangementExtensionDelete = new ArrayList<>(rulArrangementExtensions);
        rulArrangementExtensionDelete.removeAll(rulArrangementExtensionsNew);
        arrangementExtensionRepository.delete(rulArrangementExtensionDelete);

        return rulArrangementExtensionsNew;
    }

    /**
     * Zpracování řídících pravidel archivního popisu, které definují dané rozšíření.
     *
     * @param extensionRules           importované řídící pravidla
     * @param rulArrangementExtensions definice rozšíření
     */
    private List<RulExtensionRule> processExtensionRules(
                                                         final RuleUpdateContext ruc,
                                                         final List<RulArrangementExtension> rulArrangementExtensions
                                                         ) {
        ExtensionRules extensionRules = ruc
                .convertXmlStreamToObject(ExtensionRules.class, EXTENSION_RULE_XML);

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
                rulExtensionRulesNew.add(item);
            }
        }

        rulExtensionRulesNew = extensionRuleRepository.save(rulExtensionRulesNew);

        List<RulExtensionRule> rulExtensionRulesDelete = new ArrayList<>(rulExtensionRules);
        rulExtensionRulesDelete.removeAll(rulExtensionRulesNew);

        List<RulComponent> rulComponentsDelete = rulExtensionRulesDelete.stream().map(RulExtensionRule::getComponent).collect(Collectors.toList());
        extensionRuleRepository.delete(rulExtensionRulesDelete);
        componentRepository.delete(rulComponentsDelete);

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
     *  @param rulPackage     balíček
     * @param arrangementRule    VO pravidla
     * @param rulArrangementRule DAO pravidla
     * @param rulRuleSet     pravidlo
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
                                                         ()->new RulAction()
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
        rulPackageActionsNew = packageActionsRepository.save(rulPackageActionsNew);

        // smazání nedefinovaných hromadných akcí včetně vazeb
        List<RulAction> rulPackageActionsDelete = new ArrayList<>(dbActions);
        rulPackageActionsDelete.removeAll(rulPackageActionsNew);

        for (RulAction rulAction : rulPackageActionsDelete) {
            itemTypeActionRepository.deleteByAction(rulAction);
            actionRecommendedRepository.deleteByAction(rulAction);
        }
        packageActionsRepository.delete(rulPackageActionsDelete);

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
            actionRecommendedRepository.save(rulActionRecomendedsNew);

        }
        // vyhkedat a odstranit již nenavázané doporučené akce
        List<RulActionRecommended> rulActionRecommendedsDelete = new ArrayList<>(rulActionRecommendeds);
        rulActionRecommendedsDelete.removeAll(rulActionRecomendedsNew);
        actionRecommendedRepository.delete(rulActionRecommendedsDelete);
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
            itemTypeActionRepository.delete(rulTypeActionsDelete);
        }
    }

    /**
     * Smazání (reálně přesun) souboru.
     *
     * @param dir
     *            adresář
     * @param filename
     *            název souboru
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
     *  @param rulPackage       balíček
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
     * Zpracování typů atributů.
     *
     * @param puc      balíček
     * @return                výsledný seznam atributů v db
     */
    private List<RulItemType> processItemTypes(final PackageContext puc) {
        ItemSpecs itemSpecs = puc.convertXmlStreamToObject(ItemSpecs.class, ITEM_SPEC_XML);
        ItemTypes itemTypes = puc.convertXmlStreamToObject(ItemTypes.class, ITEM_TYPE_XML);

        ItemTypeUpdater updater = applicationContext.getBean(ItemTypeUpdater.class);

        List<RulItemType> updatedItemTypes = updater.update(itemTypes, itemSpecs, puc);
        // check if node cache should be sync
        if (updater.getNumDroppedCachedNode() > 0) {
            puc.setSyncNodeCache(true);
        }
        return updatedItemTypes;
    }

    /**
     * Zpracování typů atributů.
     *
     * @param ruc
     *            context
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

        rulOutputTypesNew = outputTypeRepository.save(rulOutputTypesNew);

        // update templates
        TemplateUpdater templateUpdater = new TemplateUpdater(this.templateRepository, outputDefinitionRepository,
                                                              this.outputResultRepository,
                                                              rulOutputTypesNew);
        templateUpdater.run(ruc);

        List<RulOutputType> rulOutputTypesDelete = new ArrayList<>(rulOutputTypes);
        rulOutputTypesDelete.removeAll(rulOutputTypesNew);

        if (!rulOutputTypesDelete.isEmpty()) {
            List<ArrOutputDefinition> byOutputTypes = outputDefinitionRepository
                    .findByOutputTypes(rulOutputTypesDelete);
            if (!byOutputTypes.isEmpty()) {
                throw new IllegalStateException(
                        "Existuje výstup(y) navázáný na typ výstupu, který je v novém balíčku smazán.");
            }

            List<RulComponent> rulComponentsDelete = rulOutputTypesDelete.stream().map(RulOutputType::getComponent)
                    .filter(Objects::nonNull).collect(Collectors.toList());
            outputTypeRepository.delete(rulOutputTypesDelete);
            componentRepository.delete(rulComponentsDelete);
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
     * @param pkgCtx
     *            package context
     */
    private void processRuleSets(
                                             final PackageContext pkgCtx) {
        RuleSets xmlRulesets = pkgCtx.convertXmlStreamToObject(RuleSets.class, RULE_SET_XML);
        RulPackage rulPackage = pkgCtx.getPackage();
        
        List<RulRuleSet> rulRuleSets = ruleSetRepository.findByRulPackage(rulPackage);
        List<RulRuleSet> rulRuleSetsNew = new ArrayList<>();

        if (xmlRulesets != null && !CollectionUtils.isEmpty(xmlRulesets.getRuleSets())) {
            for (RuleSetXml ruleSet : xmlRulesets.getRuleSets()) {
                // find ruleset in DB
                Optional<RulRuleSet> foundItem = rulRuleSets.stream().filter((r) -> r.getCode().equals(ruleSet
                        .getCode()))
                        .findFirst();
                RulRuleSet item = foundItem.orElseGet(() -> new RulRuleSet());

                convertRuleSet(rulPackage, ruleSet, item);
                rulRuleSetsNew.add(item);
                
                RuleUpdateContext ruc = new RuleUpdateContext(RuleState.UPDATE, pkgCtx, 
                                                              item, this.resourcePathResolver);
                pkgCtx.addRuleUpdateContext(ruc);
            }
        }

        // Uložení pravidel
        rulRuleSetsNew = ruleSetRepository.save(rulRuleSetsNew);

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
    }

    /**
     * Smazání pravidel.
     * 
     * @param pkgCtx
     *            package context
     */
    private void deleteRuleSets(final PackageContext pkgCtx) {
        for (RuleUpdateContext ruc : pkgCtx.getRuleUpdateContexts()) {
            RuleState ruleState = ruc.getRuleState();
            if (ruleState == RuleState.DELETE) {
                ruleSetRepository.delete(ruc.getRulSet());
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
        rulRuleSet.setPackage(rulPackage);
    }

    /**
     * Zpracování importovaného balíčku.
     *
     * @param packageInfo VO importovaného balíčku
     * @param mapEntry
     */
    private void processRulPackage(PackageContext puc) {

        PackageInfo packageInfo = puc.getPackageInfo();

        RulPackage rulPackage = packageRepository.findTopByCode(packageInfo.getCode());

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
        packageDependencyRepository.save(newDependencies);
    }

    /**
     * Smazání importovaného balíčku podle kódu.
     * 
     * Note: only one package can be imported at a time
     *
     * @param code
     *            kód balíčku
     * 
     */
    @Transactional
    @AuthMethod(permission = { UsrPermission.Permission.ADMIN })
    synchronized public void deletePackage(final String code) {
        RulPackage rulPackage = packageRepository.findTopByCode(code);

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
        for (RulItemSpec rulDescItemSpec : rulDescItemSpecs) {
            itemSpecRegisterRepository.deleteByItemSpec(rulDescItemSpec);
        }
        itemSpecRepository.delete(rulDescItemSpecs);

        List<RulRuleSet> ruleSets = ruleSetRepository.findByRulPackage(rulPackage);
        List<RulArrangementRule> arrangementRules = arrangementRuleRepository.findByRulPackage(rulPackage);
        List<RulStructureExtensionDefinition> structureExtensionDefinitions = structureExtensionDefinitionRepository.findByRulPackage(rulPackage);
        List<RulStructureDefinition> structureDefinitions = structureDefinitionRepository.findByRulPackage(rulPackage);
        List<RulAction> actions = packageActionsRepository.findByRulPackage(rulPackage);
        List<RulOutputType> outputTypes = outputTypeRepository.findByRulPackage(rulPackage);

        packageActionsRepository.findByRulPackage(rulPackage).forEach(this::deleteActionLink);
        itemTypeRepository.deleteByRulPackage(rulPackage);
        structureExtensionDefinitionRepository.deleteByRulPackage(rulPackage);
        structureExtensionRepository.deleteByRulPackage(rulPackage);
        structureDefinitionRepository.deleteByRulPackage(rulPackage);
        structureTypeRepository.deleteByRulPackage(rulPackage);
        packageActionsRepository.deleteByRulPackage(rulPackage);
        arrangementRuleRepository.deleteByRulPackage(rulPackage);
        policyTypeRepository.deleteByRulPackage(rulPackage);
        templateRepository.deleteByRulPackage(rulPackage);
        outputTypeRepository.deleteByRulPackage(rulPackage);
        extensionRuleRepository.deleteByRulPackage(rulPackage);
        arrangementExtensionRepository.deleteByRulPackage(rulPackage);
        ruleSetRepository.delete(ruleSets);
        registryRoleRepository.deleteByRulPackage(rulPackage);
        apTypeRepository.preDeleteByRulPackage(rulPackage);
        apTypeRepository.deleteByRulPackage(rulPackage);
        relationTypeRoleTypeRepository.deleteByRulPackage(rulPackage);
        partyTypeRelationRepository.deleteByRulPackage(rulPackage);
        relationTypeRepository.deleteByRulPackage(rulPackage);
        partyTypeComplementTypeRepository.deleteByRulPackage(rulPackage);
        uiPartyGroupRepository.deleteByRulPackage(rulPackage);
        complementTypeRepository.deleteByRulPackage(rulPackage);
        partyRelationClassTypeRepository.deleteByRulPackage(rulPackage);
        partyNameFormTypeRepository.deleteByRulPackage(rulPackage);
        relationRoleTypeRepository.deleteByRulPackage(rulPackage);
        settingsRepository.deleteByRulPackage(rulPackage);
        ruleRepository.deleteByRulPackage(rulPackage);
        ruleSystemRepository.deleteByRulPackage(rulPackage);
        packageRepository.delete(rulPackage);

        entityManager.flush();

        for (RulRuleSet ruleSet : ruleSets) {
            File dirGroovies = resourcePathResolver.getGroovyDir(rulPackage.getPackageId(), ruleSet.getRuleSetId())
                    .toFile();
            File dirActions = resourcePathResolver.getFunctionsDir(rulPackage.getPackageId(), ruleSet.getRuleSetId()).toFile();
            File dirRules = resourcePathResolver.getDroolsDir(rulPackage.getPackageId(), ruleSet.getRuleSetId()).toFile();

            try {

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

                entityManager.flush();

                bulkActionConfigManager.load();
            } catch (IOException e) {
                throw new SystemException("Nastala chyba během obnovy souborů po selhání importu balíčku", e);
            }
        }
    }

    /**
     * Smazání návazných entit.
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
     * 
     * Note: only one package can be imported at a time
     *
     * @param code
     *            kód balíčku
     * @return výsledný soubor
     * @throws IOException
     */
    @Transactional(readOnly = true)
    synchronized public Path exportPackage(final String code) throws IOException {
        RulPackage rulPackage = packageRepository.findTopByCode(code);

        if (rulPackage == null) {
            throw new ObjectNotFoundException("Balíček s kódem " + code + " neexistuje", PackageCode.PACKAGE_NOT_EXIST).set("code", code);
        }

        try (AutoDeletingTempFile tempFile = AutoDeletingTempFile.createTempFile("ElzaPackage-" + code + "-",
                                                                                 "-package.zip"))
        {
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
            exportArrangementRules(rulPackage, zos);
            exportArrangementExtensions(rulPackage, zos);
            exportExtensionRules(rulPackage, zos);
            exportOutputTypes(rulPackage, zos);
            exportTemplates(rulPackage, zos);
            exportRegistryRoles(rulPackage, zos);
            exportRegisterTypes(rulPackage, zos);
            exportRelationTypeRoleTypes(rulPackage, zos);
            exportPartyTypeRelations(rulPackage, zos);
            exportRelationTypes(rulPackage, zos);
            exportPartyTypeComplementTypes(rulPackage, zos);
            exportUIPartyGroups(rulPackage, zos);
            exportComplementTypes(rulPackage, zos);
            exportPartyRelationClassTypes(rulPackage, zos);
            exportPartyNameFormTypes(rulPackage, zos);
            exportRelationRoleTypes(rulPackage, zos);
            exportSettings(rulPackage, zos);
            exportExternalIdTypes(rulPackage, zos);
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


    /**
     * Přidání nastavení do zip souboru.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportSettings(final RulPackage rulPackage, final ZipOutputStream zos) {
        List<UISettings> uiSettings = settingsRepository.findByRulPackage(rulPackage);

        if (uiSettings.size() == 0) {
            return;
        }

        Map<Integer, RulRuleSet> ruleSetMap = ruleSetRepository.findAll().stream()
                .collect(Collectors.toMap(RulRuleSet::getRuleSetId, Function.identity()));
        Map<Integer, RulItemType> itemTypeMap = itemTypeRepository.findAll().stream()
                .collect(Collectors.toMap(RulItemType::getItemTypeId, Function.identity()));

        List<RulRuleSet> ruleSetList = new ArrayList<>();
        Set<Integer> ruleSetIdAdd = new HashSet<>();
        Collection<UISettings.SettingsType> settingsTypesRule = UISettings.SettingsType.findByType(UISettings.EntityType.RULE);
        Collection<UISettings.SettingsType> settingsTypesItemType = UISettings.SettingsType.findByType(UISettings.EntityType.ITEM_TYPE);

        for (UISettings uiSetting : uiSettings) {
            if (settingsTypesRule.contains(uiSetting.getSettingsType())) {
                Integer ruleSetId = uiSetting.getEntityId();
                if (!ruleSetIdAdd.contains(ruleSetId)) {
                    ruleSetIdAdd.add(ruleSetId);
                    ruleSetList.add(ruleSetMap.get(ruleSetId));
                }
            }/* else if (settingsTypesItemType.contains(uiSetting.getSettingsType())) {
                Integer itemTypeId = uiSetting.getEntityId();
                RulItemType rulItemType = itemTypeMap.get(itemTypeId);
                Integer ruleSetId = rulItemType.getRuleSet().getRuleSetId();
                if (!ruleSetIdAdd.contains(ruleSetId)) {
                    ruleSetIdAdd.add(ruleSetId);
                    ruleSetList.add(ruleSetMap.get(ruleSetId));
                }
            }*/
        }

        if (!ruleSetList.contains(null)) {
            ruleSetList.add(null);
        }

        for (RulRuleSet ruleSet : ruleSetList) {
            String path = ruleSet == null ? SETTING_XML : ZIP_DIR_RULE_SET + "/" + ruleSet.getCode() + "/" + SETTING_XML;
            export(rulPackage, zos, settingsRepository, Settings.class, Setting.class,
                    (settingList, settings) -> settings.setSettings(settingList),
                    (uiSetting, setting) -> setting.setValue(uiSetting.getValue()),
                    (uiSetting, rulRuleSet) -> PackageService.convertSetting(uiSetting, itemTypeRepository),
                    (s) -> filterSettingByType(s, ruleSet), path, ruleSet);
        }
    }

    private boolean filterSettingByType(final UISettings setting, final RulRuleSet ruleSet) {
        EntityType entityType = setting.getEntityType();
        if (entityType == EntityType.RULE) {
            if (ruleSet == null) {
                return false;
            }
            Integer ruleSetId = Validate.notNull(setting.getEntityId());
            if (ruleSet.getRuleSetId().equals(ruleSetId)) {
                return true;
            }
            return false;
        }

        if (entityType == EntityType.ITEM_TYPE) {
            if (ruleSet == null) {
                return false;
            }
            StaticDataProvider staticData = staticDataService.getData();
            return staticData.getItemTypeById(setting.getEntityId()) != null;
        }

        if (ruleSet != null) {
            return false;
        }

        return true;
    }

    public static Setting convertSetting(final UISettings uiSettings, final ItemTypeRepository itemTypeRepository) {
        // factory
        Setting entity;
        if (Objects.equals(uiSettings.getSettingsType(), UISettings.SettingsType.FUND_VIEW)
                && Objects.equals(uiSettings.getEntityType(), UISettings.EntityType.RULE)) {
            entity = new SettingFundViews();
        } else if (Objects.equals(uiSettings.getSettingsType(), UISettings.SettingsType.TYPE_GROUPS)
                && Objects.equals(uiSettings.getEntityType(), UISettings.EntityType.RULE)) {
            entity = new SettingTypeGroups();
        } else if (Objects.equals(uiSettings.getSettingsType(), UISettings.SettingsType.FAVORITE_ITEM_SPECS)
                && Objects.equals(uiSettings.getEntityType(), UISettings.EntityType.ITEM_TYPE)) {
            SettingFavoriteItemSpecs settingFavoriteItemSpecs = new SettingFavoriteItemSpecs();
            if (uiSettings.getEntityId() != null) {
                RulItemType itemType = itemTypeRepository.findOne(uiSettings.getEntityId());
                if (itemType != null) {
                    settingFavoriteItemSpecs.setCode(itemType.getCode());
                }
            }
            entity = settingFavoriteItemSpecs;
        } else if (Objects.equals(uiSettings.getSettingsType(), UISettings.SettingsType.RECORD)) {
            entity = new SettingRecord();
        } else if (Objects.equals(uiSettings.getSettingsType(), UISettings.SettingsType.STRUCTURE_TYPES)) {
            entity = new SettingStructureTypes();
        } else if (Objects.equals(uiSettings.getSettingsType(), UISettings.SettingsType.GRID_VIEW)) {
            entity = new SettingGridView();
        } else {
            entity = new SettingBase();
        }
        entity.setSettingsType(uiSettings.getSettingsType());
        entity.setEntityType(uiSettings.getEntityType());
        entity.setValue(uiSettings.getValue());
        return entity;
    }

    private void exportRelationRoleTypes(final RulPackage rulPackage, final ZipOutputStream zos) {
        export(rulPackage, zos, relationRoleTypeRepository, RelationRoleTypes.class, RelationRoleType.class,
                (relationRoleTypeList, relationRoleTypes) -> relationRoleTypes.setRelationRoleTypes(relationRoleTypeList),
                (parRelationRoleType, relationRoleType) -> {
                    relationRoleType.setCode(parRelationRoleType.getCode());
                    relationRoleType.setName(parRelationRoleType.getName());
                }, null, null, RELATION_ROLE_TYPE_XML, null);
    }

    private void exportPartyNameFormTypes(final RulPackage rulPackage, final ZipOutputStream zos) {
        export(rulPackage, zos, partyNameFormTypeRepository, PartyNameFormTypes.class, PartyNameFormType.class,
                (partyNameFormTypeList, partyNameFormTypes) -> partyNameFormTypes.setPartyNameFormTypes(partyNameFormTypeList),
                (parPartyNameFormType, partyNameFormType) -> {
                    partyNameFormType.setCode(parPartyNameFormType.getCode());
                    partyNameFormType.setName(parPartyNameFormType.getName());
                }, null, null, PARTY_NAME_FORM_TYPE_XML, null);
    }

    private void exportPartyRelationClassTypes(final RulPackage rulPackage, final ZipOutputStream zos) {
        export(rulPackage, zos, partyRelationClassTypeRepository, RelationClassTypes.class, RelationClassType.class,
                (relationClassTypeList, relationClassTypes) -> relationClassTypes.setRelationClassTypes(relationClassTypeList),
                (parRelationClassType, relationClassType) -> {
                    relationClassType.setCode(parRelationClassType.getCode());
                    relationClassType.setName(parRelationClassType.getName());
                    relationClassType.setRepeatability(parRelationClassType.getRepeatability().name());
                }, null, null, RELATION_CLASS_TYPE_XML, null);
    }

    private void exportComplementTypes(final RulPackage rulPackage, final ZipOutputStream zos) {
        export(rulPackage, zos, complementTypeRepository, ComplementTypes.class, ComplementType.class,
                (complementTypeList, complementTypes) -> complementTypes.setComplementTypes(complementTypeList),
                (parComplementType, complementType) -> {
                    complementType.setCode(parComplementType.getCode());
                    complementType.setName(parComplementType.getName());
                    complementType.setViewOrder(parComplementType.getViewOrder());
                }, null, null, COMPLEMENT_TYPE_XML, null);
    }

    private void exportUIPartyGroups(final RulPackage rulPackage, final ZipOutputStream zos) {
        export(rulPackage, zos, uiPartyGroupRepository, PartyGroups.class, PartyGroup.class,
                (partyGroupList, partyGroups) -> partyGroups.setPartyGroups(partyGroupList),
                (uiPartyGroup, partyGroup) -> {
                    partyGroup.setCode(uiPartyGroup.getCode());
                    partyGroup.setName(uiPartyGroup.getName());
                    partyGroup.setViewOrder(uiPartyGroup.getViewOrder());
                    partyGroup.setPartyType(uiPartyGroup.getPartyType() == null ? null : uiPartyGroup.getPartyType().getCode());
                    partyGroup.setContentDefinitionsString(uiPartyGroup.getContentDefinition());
                    partyGroup.setType(uiPartyGroup.getType().name());
                }, null, null, PARTY_GROUP_XML, null);
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

    private void exportPartyTypeComplementTypes(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        PartyTypeComplementTypes partyTypeComplementTypes = new PartyTypeComplementTypes();
        List<ParPartyTypeComplementType> parPartyTypeComplementTypes = partyTypeComplementTypeRepository.findByRulPackage(rulPackage);
        if (parPartyTypeComplementTypes.size() == 0) {
            return;
        }
        List<PartyTypeComplementType> partyTypeComplementTypeList = new ArrayList<>(parPartyTypeComplementTypes.size());
        partyTypeComplementTypes.setPartyTypeComplementTypes(partyTypeComplementTypeList);

        for (ParPartyTypeComplementType parPartyTypeComplementType : parPartyTypeComplementTypes) {
            PartyTypeComplementType partyTypeComplementType = new PartyTypeComplementType();
            convertPartyTypeComplementType(parPartyTypeComplementType, partyTypeComplementType);
            partyTypeComplementTypeList.add(partyTypeComplementType);
        }

        addObjectToZipFile(partyTypeComplementTypes, zos, PARTY_TYPE_COMPLEMENT_TYPE_XML);
    }

    private void convertPartyTypeComplementType(final ParPartyTypeComplementType parPartyTypeComplementType, final PartyTypeComplementType partyTypeComplementType) {
        partyTypeComplementType.setPartyType(parPartyTypeComplementType.getPartyType().getCode());
        partyTypeComplementType.setComplementType(parPartyTypeComplementType.getComplementType().getCode());
        partyTypeComplementType.setRepeatable(parPartyTypeComplementType.isRepeatable());
    }

    private void exportRelationTypes(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        RelationTypes relationTypes = new RelationTypes();
        List<ParRelationType> parRelationTypes = relationTypeRepository.findByRulPackage(rulPackage);
        if (parRelationTypes.size() == 0) {
            return;
        }
        List<RelationType> relationTypeList = new ArrayList<>(parRelationTypes.size());
        relationTypes.setRelationTypes(relationTypeList);

        for (ParRelationType parRelationType : parRelationTypes) {
            RelationType relationType = new RelationType();
            convertRelationType(parRelationType, relationType);
            relationTypeList.add(relationType);
        }

        addObjectToZipFile(relationTypes, zos, RELATION_TYPE_XML);
    }

    private void convertRelationType(final ParRelationType parRelationType, final RelationType relationType) {
        relationType.setName(parRelationType.getName());
        relationType.setCode(parRelationType.getCode());
        relationType.setRelatioClassType(parRelationType.getRelationClassType().getCode());
        relationType.setUseUnitdate(parRelationType.getUseUnitdate().name());
    }

    private void exportPartyTypeRelations(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        PartyTypeRelations relationTypeRoleTypes = new PartyTypeRelations();
        List<ParPartyTypeRelation> parRelationTypeRoleTypes = partyTypeRelationRepository.findByRulPackage(rulPackage);
        if (parRelationTypeRoleTypes.size() == 0) {
            return;
        }
        List<PartyTypeRelation> relationTypeRoleTypeList = new ArrayList<>(parRelationTypeRoleTypes.size());
        relationTypeRoleTypes.setPartyTypeRelations(relationTypeRoleTypeList);

        for (ParPartyTypeRelation parPartyTypeRelation : parRelationTypeRoleTypes) {
            PartyTypeRelation relationTypeRoleType = new PartyTypeRelation();
            convertPartyTypeRelation(parPartyTypeRelation, relationTypeRoleType);
            relationTypeRoleTypeList.add(relationTypeRoleType);
        }

        addObjectToZipFile(relationTypeRoleTypes, zos, PARTY_TYPE_RELATION_XML);
    }

    private void convertPartyTypeRelation(final ParPartyTypeRelation parPartyTypeRelation, final PartyTypeRelation relationTypeRoleType) {
        relationTypeRoleType.setViewOrder(parPartyTypeRelation.getViewOrder());
        relationTypeRoleType.setRepeatable(parPartyTypeRelation.isRepeatable());
        relationTypeRoleType.setName(parPartyTypeRelation.getName());
        relationTypeRoleType.setRelationType(parPartyTypeRelation.getRelationType().getCode());
        relationTypeRoleType.setPartyType(parPartyTypeRelation.getPartyType().getCode());
    }

    private void exportRelationTypeRoleTypes(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        RelationTypeRoleTypes relationTypeRoleTypes = new RelationTypeRoleTypes();
        List<ParRelationTypeRoleType> parRelationTypeRoleTypes = relationTypeRoleTypeRepository.findByRulPackage(rulPackage);
        if (parRelationTypeRoleTypes.size() == 0) {
            return;
        }
        List<RelationTypeRoleType> relationTypeRoleTypeList = new ArrayList<>(parRelationTypeRoleTypes.size());
        relationTypeRoleTypes.setRelationTypeRoleTypes(relationTypeRoleTypeList);

        for (ParRelationTypeRoleType parRelationTypeRoleType : parRelationTypeRoleTypes) {
            RelationTypeRoleType relationTypeRoleType = new RelationTypeRoleType();
            convertRelationTypeRoleType(parRelationTypeRoleType, relationTypeRoleType);
            relationTypeRoleTypeList.add(relationTypeRoleType);
        }

        addObjectToZipFile(relationTypeRoleTypes, zos, RELATION_TYPE_ROLE_TYPE_XML);
    }

    private void convertRelationTypeRoleType(final ParRelationTypeRoleType parRelationTypeRoleType, final RelationTypeRoleType relationTypeRoleType) {
        //relationTypeRoleType.setViewOrder(regRegisterType.getViewOrder());
        relationTypeRoleType.setRelationType(parRelationTypeRoleType.getRelationType().getCode());
        relationTypeRoleType.setRepeatable(parRelationTypeRoleType.getRepeatable());
        relationTypeRoleType.setRoleType(parRelationTypeRoleType.getRoleType().getCode());
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
        registerType.setPartyType(apType.getPartyType() == null ? null : apType.getPartyType().getCode());
        registerType.setParentType(apType.getParentApType() == null ? null : apType.getParentApType().getCode());
        registerType.setRuleSystem(apType.getRuleSystem() == null ? null : apType.getRuleSystem().getCode());
    }

    private void exportRegistryRoles(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        RegistryRoles registryRoles = new RegistryRoles();
        List<ParRegistryRole> parRegistryRoles = registryRoleRepository.findByRulPackage(rulPackage);
        if (parRegistryRoles.size() == 0) {
            return;
        }
        List<RegistryRole> registryRoleList = new ArrayList<>(parRegistryRoles.size());
        registryRoles.setRegistryRoles(registryRoleList);

        for (ParRegistryRole parRegistryRole : parRegistryRoles) {
            RegistryRole registryRole = new RegistryRole();
            convertRegistryRole(parRegistryRole, registryRole);
            registryRoleList.add(registryRole);
        }

        addObjectToZipFile(registryRoles, zos, REGISTRY_ROLE_XML);
    }

    private void convertRegistryRole(final ParRegistryRole parRegistryRole, final RegistryRole registryRole) {
        registryRole.setRegisterType(parRegistryRole.getApType().getCode());
        registryRole.setRoleType(parRegistryRole.getRoleType().getCode());
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
     * Exportování informace o balíčku
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportPackageInfo(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {

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
     *  @param rulPackage balíček
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
            ItemType itemType = ItemType.fromEntity(rulDescItemType);
            itemTypeList.add(itemType);
        }

        addObjectToZipFile(itemTypes, zos, ITEM_TYPE_XML);
    }

    /**
     * Exportování specifikací atributů.
     *  @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportItemSpecs(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        List<RulItemSpec> rulDescItemSpecs = itemSpecRepository.findByRulPackage(rulPackage);
        if (rulDescItemSpecs.size() == 0) {
            return;
        }

        ItemSpecs itemSpecs = new ItemSpecs();
        List<ItemSpec> itemSpecList = new ArrayList<>(rulDescItemSpecs.size());
        itemSpecs.setItemSpecs(itemSpecList);

        for (RulItemSpec rulDescItemSpec : rulDescItemSpecs) {
            ItemSpec itemSpec = ItemSpec.fromEntity(rulDescItemSpec, itemSpecRegisterRepository);
            itemSpecList.add(itemSpec);
        }

        addObjectToZipFile(itemSpecs, zos, ITEM_SPEC_XML);
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
     * @param packageRule    VO pravidla
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
     * @param arrangementExtension VO rozšíření
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
     * @param extensionRule VO řídících pravidel
     */
    private void convertExtensionRule(final RulExtensionRule rulExtensionRule,
                                      final ExtensionRule extensionRule) {
        extensionRule.setFilename(rulExtensionRule.getComponent().getFilename());
        extensionRule.setPriority(rulExtensionRule.getPriority());
        extensionRule.setRuleType(rulExtensionRule.getRuleType());
        extensionRule.setArrangementExtension(rulExtensionRule.getArrangementExtension().getCode());
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

    public Boolean getTesting() {
        return testing;
    }

    public void setTesting(final Boolean testing) {
        this.testing = testing;
    }


}

