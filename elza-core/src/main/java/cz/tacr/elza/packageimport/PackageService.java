package cz.tacr.elza.packageimport;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
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
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cz.tacr.elza.api.UseUnitdateEnum;
import cz.tacr.elza.api.enums.ParRelationClassTypeRepeatabilityEnum;
import cz.tacr.elza.api.enums.UIPartyGroupTypeEnum;
import cz.tacr.elza.bulkaction.BulkActionConfigManager;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputDefinition.OutputState;
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
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.domain.RulActionRecommended;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecRegister;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeAction;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPackageDependency;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulPolicyType;
import cz.tacr.elza.domain.RulRule;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.domain.RulTemplate.Engine;
import cz.tacr.elza.domain.UIPartyGroup;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UISettings.EntityType;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.drools.RulesExecutor;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.interpi.service.InterpiService;
import cz.tacr.elza.packageimport.xml.ActionItemType;
import cz.tacr.elza.packageimport.xml.ActionRecommended;
import cz.tacr.elza.packageimport.xml.Category;
import cz.tacr.elza.packageimport.xml.Column;
import cz.tacr.elza.packageimport.xml.ComplementType;
import cz.tacr.elza.packageimport.xml.ComplementTypes;
import cz.tacr.elza.packageimport.xml.ItemSpec;
import cz.tacr.elza.packageimport.xml.ItemSpecRegister;
import cz.tacr.elza.packageimport.xml.ItemSpecs;
import cz.tacr.elza.packageimport.xml.ItemType;
import cz.tacr.elza.packageimport.xml.ItemTypes;
import cz.tacr.elza.packageimport.xml.OutputType;
import cz.tacr.elza.packageimport.xml.OutputTypes;
import cz.tacr.elza.packageimport.xml.PackageAction;
import cz.tacr.elza.packageimport.xml.PackageActions;
import cz.tacr.elza.packageimport.xml.PackageDependency;
import cz.tacr.elza.packageimport.xml.PackageInfo;
import cz.tacr.elza.packageimport.xml.PackageRule;
import cz.tacr.elza.packageimport.xml.PackageRules;
import cz.tacr.elza.packageimport.xml.PacketType;
import cz.tacr.elza.packageimport.xml.PacketTypes;
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
import cz.tacr.elza.packageimport.xml.RegisterType;
import cz.tacr.elza.packageimport.xml.RegisterTypes;
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
import cz.tacr.elza.packageimport.xml.RuleSet;
import cz.tacr.elza.packageimport.xml.RuleSets;
import cz.tacr.elza.packageimport.xml.Setting;
import cz.tacr.elza.packageimport.xml.SettingBase;
import cz.tacr.elza.packageimport.xml.SettingFavoriteItemSpecs;
import cz.tacr.elza.packageimport.xml.SettingFundViews;
import cz.tacr.elza.packageimport.xml.SettingGridView;
import cz.tacr.elza.packageimport.xml.SettingRecord;
import cz.tacr.elza.packageimport.xml.SettingTypeGroups;
import cz.tacr.elza.packageimport.xml.Settings;
import cz.tacr.elza.packageimport.xml.Template;
import cz.tacr.elza.packageimport.xml.Templates;
import cz.tacr.elza.repository.ActionRecommendedRepository;
import cz.tacr.elza.repository.ActionRepository;
import cz.tacr.elza.repository.ComplementTypeRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.ItemSpecRegisterRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeActionRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.OutputDefinitionRepository;
import cz.tacr.elza.repository.OutputTypeRepository;
import cz.tacr.elza.repository.PackageDependencyRepository;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.repository.Packaging;
import cz.tacr.elza.repository.PacketTypeRepository;
import cz.tacr.elza.repository.PartyNameFormTypeRepository;
import cz.tacr.elza.repository.PartyRelationClassTypeRepository;
import cz.tacr.elza.repository.PartyTypeComplementTypeRepository;
import cz.tacr.elza.repository.PartyTypeRelationRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.PolicyTypeRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RegistryRoleRepository;
import cz.tacr.elza.repository.RelationRoleTypeRepository;
import cz.tacr.elza.repository.RelationTypeRepository;
import cz.tacr.elza.repository.RelationTypeRoleTypeRepository;
import cz.tacr.elza.repository.RuleRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.SettingsRepository;
import cz.tacr.elza.repository.TemplateRepository;
import cz.tacr.elza.repository.UIPartyGroupRepository;
import cz.tacr.elza.service.CacheService;
import cz.tacr.elza.service.event.CacheInvalidateEvent;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.ActionEvent;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.output.OutputGeneratorService;
import cz.tacr.elza.utils.AppContext;


/**
 * Service pro správu importovaných balíčků s pravidly, hromadnými akcemi apod.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@Service
public class PackageService {

    @Value("${elza.package.testing:false}")
    private Boolean testing;

    /**
     * hlavní soubor v zipu
     */
    public static final String PACKAGE_XML = "package.xml";

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
     * pravidla v zipu
     */
    public static final String PACKAGE_RULES_XML = "rul_package_rules.xml";

    /**
     * typy outputů
     */
    public static final String OUTPUT_TYPE_XML = "rul_output_type.xml";

    /**
     * templaty outputů
     */
    public static final String TEMPLATE_XML = "rul_template.xml";

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
    public static final String REGISTER_TYPE_XML = "reg_register_type.xml";
    public static final String SETTING_XML = "ui_setting.xml";

    /**
     * Složka templatů
     */
    private final String ZIP_DIR_TEMPLATES = "templates";

    /**
     * název složky pro vyhledání pravidel
     */
    public static final String ZIP_DIR_RULE_SET = "rul_rule_set";

    /**
     * typy packet v zipu
     */
    public static final String PACKET_TYPE_XML = "rul_packet_type.xml";

    /**
     * adresář pro hromadné akce v zip
     */
    private final String ZIP_DIR_ACTIONS = "bulk_actions";

    /**
     * adresář pro pravidla v zip
     */
    private final String ZIP_DIR_RULES = "rules";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PackageRepository packageRepository;

    @Autowired
    private RuleSetRepository ruleSetRepository;

    @Autowired
    private DataTypeRepository dataTypeRepository;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private ItemSpecRegisterRepository itemSpecRegisterRepository;

    @Autowired
    private ActionRepository packageActionsRepository;

    @Autowired
    private RuleRepository packageRulesRepository;

    @Autowired
    private BulkActionConfigManager bulkActionConfigManager;

    @Autowired
    private PacketTypeRepository packetTypeRepository;

    @Autowired
    private PolicyTypeRepository policyTypeRepository;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private RulesExecutor rulesExecutor;

    @Autowired
    private OutputTypeRepository outputTypeRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private ActionRecommendedRepository actionRecommendedRepository;

    @Autowired
    private ItemTypeActionRepository itemTypeActionRepository;

    @Autowired
    private OutputGeneratorService outputGeneratorService;

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
    private RegisterTypeRepository registerTypeRepository;

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

    private List<RulTemplate> newRultemplates = new ArrayList<>();

    /**
     * Provede import balíčku.
     *
     * @param file soubor balíčku
     */
    public void importPackage(final File file) {
        List<File> dirsActions = new ArrayList<>();
        List<File> dirsRules = new ArrayList<>();
        List<File> dirsTemplates = new ArrayList<>();

        ZipFile zipFile = null;
        List<RulAction> rulPackageActions = new ArrayList<>();
        List<RulRule> rulPackageRules = new ArrayList<>();
        List<RulTemplate> originalRulTemplates = new ArrayList<>();

        try {

            zipFile = new ZipFile(file);

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            Map<String, ByteArrayInputStream> mapEntry = PackageUtils.createStreamsMap(zipFile, entries);

            // načtení info o importu
            PackageInfo packageInfo = PackageUtils.convertXmlStreamToObject(PackageInfo.class, mapEntry.get(PACKAGE_XML));
            if (packageInfo == null) {
                throw new BusinessException("Soubor " + PACKAGE_XML + " nenalezen", PackageCode.FILE_NOT_FOUND).set("file", PACKAGE_XML);
            }

            RulPackage rulPackage = processRulPackage(packageInfo);

            Map<String, String> rulePaths = PackageUtils.findRulePaths(ZIP_DIR_RULE_SET, mapEntry.keySet());

            originalRulTemplates = templateRepository.findByRulPackage(rulPackage);

            RuleSets ruleSets = PackageUtils.convertXmlStreamToObject(RuleSets.class, mapEntry.get(RULE_SET_XML));

            List<RulRuleSet> rulRuleSetsDelete = new ArrayList<>();
            List<RulRuleSet> rulRuleSets = processRuleSets(ruleSets, rulPackage, rulRuleSetsDelete);

            for (Map.Entry<String, String> ruleEntry : rulePaths.entrySet()) {
                String ruleDirPath = ruleEntry.getValue();
                String ruleCode = ruleEntry.getKey();
                RulRuleSet rulRuleSet = rulRuleSets.stream().filter(rs -> rs.getCode().equalsIgnoreCase(ruleCode))
                        .findFirst()
                        .orElse(ruleSetRepository.findByCode(ruleCode));
                if (rulRuleSet == null) {
                    throw new BusinessException("RulRuleSet s code=" + ruleCode + " nenalezen", PackageCode.CODE_NOT_FOUND).set("code", ruleCode).set("file", RULE_SET_XML);
                }

                File dirActions = new File(bulkActionConfigManager.getFunctionsDir(ruleCode));
                dirsActions.add(dirActions);
                if (!dirActions.exists()) {
                    dirActions.mkdirs();
                }

                File dirRules = new File(rulesExecutor.getDroolsDir(ruleCode));
                dirsRules.add(dirRules);
                if (!dirRules.exists()) {
                    dirRules.mkdirs();
                }

                File dirTemplates = new File(outputGeneratorService.getTemplatesDir(ruleCode));
                dirsTemplates.add(dirTemplates);
                if (!dirTemplates.exists()) {
                    dirTemplates.mkdirs();
                }

                PolicyTypes policyTypes = PackageUtils.convertXmlStreamToObject(PolicyTypes.class, mapEntry.get(ruleDirPath + POLICY_TYPE_XML));

                ItemSpecs itemSpecs = PackageUtils.convertXmlStreamToObject(ItemSpecs.class, mapEntry.get(ruleDirPath + ITEM_SPEC_XML));
                ItemTypes itemTypes = PackageUtils.convertXmlStreamToObject(ItemTypes.class, mapEntry.get(ruleDirPath + ITEM_TYPE_XML));
                PackageActions packageActions = PackageUtils.convertXmlStreamToObject(PackageActions.class, mapEntry.get(ruleDirPath + PACKAGE_ACTIONS_XML));
                PackageRules packageRules = PackageUtils.convertXmlStreamToObject(PackageRules.class, mapEntry.get(ruleDirPath + PACKAGE_RULES_XML));
                OutputTypes outputTypes = PackageUtils.convertXmlStreamToObject(OutputTypes.class, mapEntry.get(ruleDirPath + OUTPUT_TYPE_XML));
                Templates templates = PackageUtils.convertXmlStreamToObject(Templates.class, mapEntry.get(ruleDirPath + TEMPLATE_XML));
                PacketTypes packetTypes = PackageUtils.convertXmlStreamToObject(PacketTypes.class, mapEntry.get(ruleDirPath + PACKET_TYPE_XML));
                Settings settings = PackageUtils.convertXmlStreamToObject(Settings.class, mapEntry.get(ruleDirPath + SETTING_XML));

                processPacketTypes(packetTypes, rulPackage, rulRuleSet);
                List<RulRule> rulRules = processPackageRules(packageRules, rulPackage, mapEntry, rulRuleSet, dirRules);
                rulPackageRules.addAll(rulRules);

                List<RulOutputType> rulOutputTypes = processOutputTypes(outputTypes, templates, rulPackage, mapEntry, dirTemplates, rulRules, rulRuleSet);
                processPolicyTypes(policyTypes, rulPackage, rulRuleSet);
                List<RulItemType> rulDescItemTypes = processItemTypes(itemTypes, itemSpecs, rulPackage, rulRuleSet);
                List<RulAction> rulActions = processPackageActions(packageActions, rulPackage, mapEntry, dirActions, rulRuleSet);
                rulPackageActions.addAll(rulActions);

                rulDescItemTypes.addAll(itemTypeRepository.findByRuleSet(rulRuleSet));
                processSettings(settings, rulPackage, rulRuleSet, rulDescItemTypes);
            }

            deleteRuleSets(rulRuleSetsDelete);

            // OSOBY ---------------------------------------------------------------------------------------------------

            List<ParPartyType> parPartyTypes = partyTypeRepository.findAll();

            RelationRoleTypes relationRoleTypes = PackageUtils.convertXmlStreamToObject(RelationRoleTypes.class, mapEntry.get(RELATION_ROLE_TYPE_XML));
            List<ParRelationRoleType> parRelationRoleTypes = processRelationRoleTypes(relationRoleTypes, rulPackage);

            PartyNameFormTypes partyNameFormTypes = PackageUtils.convertXmlStreamToObject(PartyNameFormTypes.class, mapEntry.get(PARTY_NAME_FORM_TYPE_XML));
            processPartyNameFormTypes(partyNameFormTypes, rulPackage);

            RelationClassTypes relationClassTypes = PackageUtils.convertXmlStreamToObject(RelationClassTypes.class, mapEntry.get(RELATION_CLASS_TYPE_XML));
            List<ParRelationClassType> parRelationClassTypes = processRelationClassTypes(relationClassTypes, rulPackage);

            ComplementTypes complementTypes = PackageUtils.convertXmlStreamToObject(ComplementTypes.class, mapEntry.get(COMPLEMENT_TYPE_XML));
            List<ParComplementType> parComplementTypes = processComplementTypes(complementTypes, rulPackage);

            PartyGroups partyGroups = PackageUtils.convertXmlStreamToObject(PartyGroups.class, mapEntry.get(PARTY_GROUP_XML));
            processPartyGroups(partyGroups, rulPackage, parPartyTypes);

            PartyTypeComplementTypes partyTypeComplementTypes = PackageUtils.convertXmlStreamToObject(PartyTypeComplementTypes.class, mapEntry.get(PARTY_TYPE_COMPLEMENT_TYPE_XML));
            processPartyTypeComplementTypes(partyTypeComplementTypes, rulPackage, parComplementTypes, parPartyTypes);

            RelationTypes relationTypes = PackageUtils.convertXmlStreamToObject(RelationTypes.class, mapEntry.get(RELATION_TYPE_XML));
            List<ParRelationType> parRelationTypes = processRelationTypes(relationTypes, rulPackage, parRelationClassTypes);

            PartyTypeRelations partyTypeRelations = PackageUtils.convertXmlStreamToObject(PartyTypeRelations.class, mapEntry.get(PARTY_TYPE_RELATION_XML));
            processPartyTypeRelations(partyTypeRelations, rulPackage, parRelationTypes, parPartyTypes);

            RelationTypeRoleTypes relationTypeRoleTypes = PackageUtils.convertXmlStreamToObject(RelationTypeRoleTypes.class, mapEntry.get(RELATION_TYPE_ROLE_TYPE_XML));
            processRelationTypeRoleTypes(relationTypeRoleTypes, rulPackage, parRelationRoleTypes, parRelationTypes);

            RegisterTypes registerTypes = PackageUtils.convertXmlStreamToObject(RegisterTypes.class, mapEntry.get(REGISTER_TYPE_XML));
            List<RegRegisterType> regRegisterTypes = processRegisterTypes(registerTypes, rulPackage, parPartyTypes);

            RegistryRoles registryRoles = PackageUtils.convertXmlStreamToObject(RegistryRoles.class, mapEntry.get(REGISTRY_ROLE_XML));
            processRegistryRoles(registryRoles, rulPackage, parRelationRoleTypes, regRegisterTypes);

            // END OSOBY -----------------------------------------------------------------------------------------------

            // NASTAVENÍ -----------------------------------------------------------------------------------------------

            Settings settings = PackageUtils.convertXmlStreamToObject(Settings.class, mapEntry.get(SETTING_XML));

            processSettings(settings, rulPackage, null, null);

            // END NASTAVENÍ -------------------------------------------------------------------------------------------

            entityManager.flush();

            if (dirsActions.size() > 0) {
                dirsActions.forEach(this::cleanBackupFiles);
            }

            if (dirsRules.size() > 0) {
                dirsRules.forEach(this::cleanBackupFiles);
            }

            if (dirsTemplates.size() > 0) {
                if (originalRulTemplates.size() > 0) {
                    for (File dirsTemplate : dirsTemplates) {
                        cleanBackupTemplates(dirsTemplate, originalRulTemplates);
                    }
                }
                if (newRultemplates.size() > 0) {
                    for (File dirsTemplate : dirsTemplates) {
                        cleanBackupTemplates(dirsTemplate, newRultemplates);
                    }
                }
            }

            staticDataService.reloadOnCommit();

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    cacheService.resetCache(CacheInvalidateEvent.Type.ALL);
                }
            });

            eventNotificationService.publishEvent(new ActionEvent(EventType.PACKAGE));

        } catch (Exception e) {
            try {
                if (rulPackageActions.size() > 0) {
                    for (RulAction rulPackageAction : rulPackageActions) {
                        for (File dirsAction : dirsActions) {
                            forceDeleteFile(dirsAction, rulPackageAction.getFilename());
                        }
                    }
                }

                if (rulPackageRules.size() > 0) {
                    for (RulRule rulPackageRule : rulPackageRules) {
                        for (File dirRule : dirsRules) {
                            forceDeleteFile(dirRule, rulPackageRule.getFilename());
                        }
                    }
                }

                if (newRultemplates.size() > 0) {
                    for (File dirTemplates : dirsTemplates) {
                        deleteTemplatesForRollback(dirTemplates, newRultemplates);
                    }
                }

                if (originalRulTemplates.size() > 0) {
                    for (RulTemplate rulTemplate : originalRulTemplates) {
                        for (File dirTemplates : dirsTemplates) {
                            File dirFile = new File(dirTemplates + File.separator + rulTemplate.getDirectory());
                            if (dirFile.exists()) {
                                rollBackFiles(dirFile);
                            }
                        }
                    }
                }

                if (dirsActions != null) {
                    for (File dirActions : dirsActions) {
                        rollBackFiles(dirActions);
                    }
                }

                if (dirsRules.size() > 0) {
                    for (File dirRules : dirsRules) {
                        rollBackFiles(dirRules);
                    }
                }

                bulkActionConfigManager.load();

                if (e instanceof AbstractException) {
                    throw e;
                } else {
                    throw new SystemException(e);
                }
            } catch (IOException e1) {
                throw new SystemException(e);
            }
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    // ok
                }
            }
        }

    }

    /**
     * Zpracování nastavení.
     */
    private void processSettings(final Settings newSettings,
                                 final RulPackage rulPackage,
                                 final RulRuleSet ruleSet,
                                 final List<RulItemType> rulItemTypes) {

        List<UISettings> allUiSettings = settingsRepository.findAll();

        List<UISettings> currUiSettings = new LinkedList<>();
        List<UISettings> otherUiSettings = new ArrayList<>();

        for (UISettings uiSett : allUiSettings) {
            if (rulPackage.getPackageId().equals(uiSett.getPackageId())) {
                currUiSettings.add(uiSett);
            } else {
                otherUiSettings.add(uiSett);
            }
        }

        if (newSettings != null) {
            for (Setting sett : newSettings.getSettings()) {
                // find same settings in other packages (throws exception when found)
                UISettings otherUiSett = otherUiSettings.stream().filter(sett::isSettingsFor).findFirst().orElse(null);
                if (otherUiSett != null) {
                    throw new SystemException("Settings already exists", PackageCode.OTHER_PACKAGE)
                        .set("UISettingsId", otherUiSett.getSettingsId())
                        .set("settingsType", otherUiSett.getSettingsType());
                }

                // find same settings in this package (update current when found)
                UISettings uiSett = null;
                for (Iterator<UISettings> it = currUiSettings.iterator(); it.hasNext();) {
                    UISettings currUiSett = it.next();
                    if (sett.isSettingsFor(currUiSett)) {
                        if (uiSett != null) {
                            throw new SystemException("Duplicate settings in same package", BaseCode.DB_INTEGRITY_PROBLEM)
                            .set("packageId", uiSett.getPackageId())
                            .set("firstUISettingsId", uiSett.getSettingsId())
                            .set("secondUISettingsId", currUiSett.getSettingsId());
                        }
                        uiSett = currUiSett;
                        it.remove();
                    }
                }

                // create new settings for this package
                if (uiSett == null) {
                    uiSett = new UISettings();
                }

                convertUISettings(rulPackage, sett, uiSett, ruleSet, rulItemTypes);

                settingsRepository.save(uiSett);
            }
        }

        settingsRepository.delete(currUiSettings);
    }

    /**
     * Konverze VO -> DO.
     */
    private void convertUISettings(final RulPackage rulPackage, final Setting setting,
                                   final UISettings uiSetting,
                                   final RulRuleSet ruleSet,
                                   final List<RulItemType> rulItemTypes) {
        uiSetting.setRulPackage(rulPackage);
        uiSetting.setSettingsType(setting.getSettingsType());
        uiSetting.setEntityType(setting.getEntityType());
        uiSetting.setValue(setting.getValue());

        EntityType entityType = setting.getEntityType();
        if (entityType == EntityType.RULE) {
            uiSetting.setEntityId(ruleSet.getRuleSetId());

        } else if (entityType == EntityType.ITEM_TYPE) {
            Validate.isTrue(setting.getClass() == SettingFavoriteItemSpecs.class);

            String code = ((SettingFavoriteItemSpecs) setting).getCode();
            setItemTypeToSetting(rulItemTypes, code, uiSetting);
        }
    }

    private void setItemTypeToSetting(final List<RulItemType> rulItemTypes, final String code, final UISettings uiSetting) {
        RulItemType entity = findEntity(rulItemTypes, code, RulItemType::getCode);
        if (entity == null) {
            throw new BusinessException("RulItemType s code=" + code + " nenalezen", PackageCode.CODE_NOT_FOUND).set("code", code).set("file", SETTING_XML);
        }
        uiSetting.setEntityId(entity.getItemTypeId());
    }

    /**
     * Zpracování vztahů typu třídy.
     *
     * @param registryRoles        VO vztahy typu třídy
     * @param rulPackage           balíček
     * @param parRelationRoleTypes seznam rolí entit ve vztahu
     * @param regRegisterTypes     seznam typů rejstříků
     */
    private void processRegistryRoles(final RegistryRoles registryRoles,
                                      final RulPackage rulPackage,
                                      final List<ParRelationRoleType> parRelationRoleTypes,
                                      final List<RegRegisterType> regRegisterTypes) {
        List<ParRegistryRole> parRegistryRoles = registryRoleRepository.findByRulPackage(rulPackage);
        List<ParRegistryRole> parRegistryRolesNew = new ArrayList<>();

        if (registryRoles != null && !CollectionUtils.isEmpty(registryRoles.getRegistryRoles())) {
            for (RegistryRole registryRole : registryRoles.getRegistryRoles()) {
                ParRegistryRole parRegistryRole = findEntity(parRegistryRoles,
                        registryRole.getRegisterType(), registryRole.getRoleType(),
                        i -> i.getRegisterType().getCode(), i -> i.getRoleType().getCode());
                if (parRegistryRole == null) {
                    parRegistryRole = new ParRegistryRole();
                }
                convertParRegistryRoles(rulPackage, registryRole, parRegistryRole, regRegisterTypes, parRelationRoleTypes);
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
     * @param regRegisterTypes      seznam typů rejstříků
     * @param parRelationRoleTypes  seznam rolí entit ve vztahu
     */
    private void convertParRegistryRoles(final RulPackage rulPackage,
                                         final RegistryRole registryRole,
                                         final ParRegistryRole parRegistryRole,
                                         final List<RegRegisterType> regRegisterTypes,
                                         final List<ParRelationRoleType> parRelationRoleTypes) {
        parRegistryRole.setRulPackage(rulPackage);
        ParRelationRoleType parRelationRoleType = findEntity(parRelationRoleTypes, registryRole.getRoleType(), ParRelationRoleType::getCode);
        if (parRelationRoleType == null) {
            throw new BusinessException("ParRelationRoleType s code=" + registryRole.getRoleType() + " nenalezen", PackageCode.CODE_NOT_FOUND).set("code", registryRole.getRoleType()).set("file", REGISTRY_ROLE_XML);
        }
        parRegistryRole.setRoleType(parRelationRoleType);
        RegRegisterType regRegisterType = findEntity(regRegisterTypes, registryRole.getRegisterType(), RegRegisterType::getCode);
        if (regRegisterType == null) {
            throw new BusinessException("RegRegisterType s code=" + registryRole.getRoleType() + " nenalezen", PackageCode.CODE_NOT_FOUND).set("code", registryRole.getRoleType()).set("file", REGISTRY_ROLE_XML);
        }
        parRegistryRole.setRegisterType(regRegisterType);
    }

    /**
     * Zpracování vztahy typu třídy.
     *
     * @param registerTypes vztahy typů tříd
     * @param rulPackage    balíček
     * @param parPartyTypes seznam typů osob
     * @return seznam aktuálních záznamů
     */
    private List<RegRegisterType> processRegisterTypes(@Nullable final RegisterTypes registerTypes,
                                                       @NotNull final RulPackage rulPackage,
                                                       @NotNull final List<ParPartyType> parPartyTypes) {
        List<RegRegisterType> regRegisterTypes = registerTypeRepository.findByRulPackage(rulPackage);
        List<RegRegisterType> regRegisterTypesNew = new ArrayList<>();

        if (registerTypes != null && !CollectionUtils.isEmpty(registerTypes.getRegisterTypes())) {
            for (RegisterType registerType : registerTypes.getRegisterTypes()) {
                RegRegisterType regRegisterType = findEntity(regRegisterTypes, registerType.getCode(), RegRegisterType::getCode);
                if (regRegisterType == null) {
                    regRegisterType = new RegRegisterType();
                }
                convertRegRegisterTypes(rulPackage, registerType, regRegisterType, parPartyTypes);
                regRegisterTypesNew.add(regRegisterType);
            }
            // druhým průchodem nastavíme rodiče (stromová struktura)
            for (RegisterType registerType : registerTypes.getRegisterTypes()) {
                if (registerType.getParentRegisterType() != null) {
                    RegRegisterType regRegisterType = findEntity(regRegisterTypesNew, registerType.getCode(), RegRegisterType::getCode);
                    RegRegisterType regRegisterTypeParent = findEntity(regRegisterTypesNew, registerType.getParentRegisterType(), RegRegisterType::getCode);
                    regRegisterType.setParentRegisterType(regRegisterTypeParent);
                }
            }
        }

        regRegisterTypesNew = registerTypeRepository.save(regRegisterTypesNew);

        List<RegRegisterType> regRegisterTypesDelete = new ArrayList<>(regRegisterTypes);
        regRegisterTypesDelete.removeAll(regRegisterTypesNew);

        regRegisterTypesDelete.forEach(registryRoleRepository::deleteByRegisterType);

        registerTypeRepository.delete(regRegisterTypesDelete);

        return regRegisterTypesNew;
    }

    /**
     * Konverze VO -> DO.
     *
     * @param rulPackage       balíček
     * @param registerType     vztah typů tříd - VO
     * @param regRegisterType  vztah typů tříd - DO
     * @param parPartyTypes    seznam typů osob
     */
    private void convertRegRegisterTypes(final RulPackage rulPackage,
                                         final RegisterType registerType,
                                         final RegRegisterType regRegisterType,
                                         final List<ParPartyType> parPartyTypes) {
        regRegisterType.setRulPackage(rulPackage);
        regRegisterType.setCode(registerType.getCode());
        regRegisterType.setName(registerType.getName());
        regRegisterType.setHierarchical(registerType.getHierarchical());
        regRegisterType.setAddRecord(registerType.getAddRecord());
        if (registerType.getPartyType() != null) {
            ParPartyType parPartyType = findEntity(parPartyTypes, registerType.getPartyType(), ParPartyType::getCode);
            if (parPartyType == null) {
                throw new BusinessException("ParPartyType s code=" + registerType.getPartyType() + " nenalezen", PackageCode.CODE_NOT_FOUND).set("code", registerType.getPartyType()).set("file", REGISTER_TYPE_XML);
            }
            regRegisterType.setPartyType(parPartyType);
        }
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
    private <T, S> T findEntity(@NotNull final Collection<T> list,
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

    private void cleanBackupTemplates(final File dirTemplates, final List<RulTemplate> originalRulTemplates) {
        for (RulTemplate rulTemplate : originalRulTemplates) {
            File dirFile = new File(dirTemplates + File.separator + rulTemplate.getDirectory());
            if (dirFile.exists()) {
                cleanBackupFiles(dirFile);
            }
        }
    }

    /**
     * Zpracování policy.
     *
     * @param policyTypes VO policy
     * @param rulPackage  balíček
     * @param rulRuleSet  pravidelo
     */
    private void processPolicyTypes(final PolicyTypes policyTypes,
                                    final RulPackage rulPackage,
                                    final RulRuleSet rulRuleSet) {
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
     * Zpracování packet.
     *  @param packetTypes importovaný seznam packets
     * @param rulPackage  balíček
     * @param rulRuleSet  pravidla
     */
    private void processPacketTypes(final PacketTypes packetTypes, final RulPackage rulPackage, final RulRuleSet rulRuleSet) {
        List<RulPacketType> rulPacketTypes = packetTypeRepository.findByRulPackageAndRuleSet(rulPackage, rulRuleSet);
        List<RulPacketType> rulPacketTypesNew = new ArrayList<>();

        if (packetTypes != null && !CollectionUtils.isEmpty(packetTypes.getPacketTypes())) {
            for (PacketType packetType : packetTypes.getPacketTypes()) {
                List<RulPacketType> findItems = rulPacketTypes.stream().filter(
                        (r) -> r.getCode().equals(packetType.getCode())).collect(
                        Collectors.toList());
                RulPacketType item;
                if (findItems.size() > 0) {
                    item = findItems.get(0);
                } else {
                    item = new RulPacketType();
                }

                convertRulPacketTypes(rulPackage, packetType, item, rulRuleSet);
                rulPacketTypesNew.add(item);
            }
        }

        rulPacketTypesNew = packetTypeRepository.save(rulPacketTypesNew);

        List<RulPacketType> rulPacketTypesDelete = new ArrayList<>(rulPacketTypesNew);
        rulPacketTypesDelete.removeAll(rulPacketTypesNew);
        packetTypeRepository.delete(rulPacketTypesDelete);
    }

    /**
     * Převod VO na DAO packet.
     *  @param rulPackage    balíček
     * @param packetType    VO packet
     * @param rulPacketType DAO packet
     * @param rulRuleSet    pravidla
     */
    private void convertRulPacketTypes(final RulPackage rulPackage,
                                       final PacketType packetType,
                                       final RulPacketType rulPacketType,
                                       final RulRuleSet rulRuleSet) {
        rulPacketType.setPackage(rulPackage);
        rulPacketType.setCode(packetType.getCode());
        rulPacketType.setName(packetType.getName());
        rulPacketType.setShortcut(packetType.getShortcut());
        rulPacketType.setRuleSet(rulRuleSet);
    }

    /**
     * Převod VO na DAO packet.
     *  @param rulPackage    balíček
     * @param outputType    VO packet
     * @param rulOutputType DAO packet
     * @param rulRuleList   seznam souborů s pravidly
     * @param rulRuleSet    pravidla
     */
    private void convertRulOutputType(final RulPackage rulPackage,
                                      final OutputType outputType,
                                      final RulOutputType rulOutputType,
                                      final List<RulRule> rulRuleList,
                                      final RulRuleSet rulRuleSet) {
        if (rulRuleList == null) {
            throw new BusinessException("Soubor " + PACKAGE_RULES_XML + " nenalezen", PackageCode.FILE_NOT_FOUND).set("file", PACKAGE_RULES_XML);
        }

        rulOutputType.setPackage(rulPackage);
        rulOutputType.setCode(outputType.getCode());
        rulOutputType.setName(outputType.getName());
        rulOutputType.setRuleSet(rulRuleSet);

        String filename = outputType.getFilename();
        if (filename != null) {
            RulRule rule = rulRuleList.stream()
                    .filter(r -> r.getFilename().equals(filename))
                    .findFirst().orElse(null);

            if (rule == null) {
                throw new BusinessException("RulRule s kódem " + filename + " nenalezen", PackageCode.CODE_NOT_FOUND).set("code", filename).set("file", RULE_SET_XML);
            }
            /*if (!rule.getRuleType().equals(RulRule.RuleType.OUTPUT_ATTRIBUTE_TYPES)) {
                throw new IllegalStateException("Typ u souboru '" + filename + "' musí být OUTPUT_ATTRIBUTE_TYPES");
            }*/
            rulOutputType.setRule(rule);
        } else {
            rulOutputType.setRule(null);
        }

    }

    /**
     * Zpracování pravidel.
     *
     * @param packageRules   importovaných seznam pravidel
     * @param rulPackage     balíček
     * @param mapEntry       mapa streamů souborů v ZIP
     * @param rulRuleSet     pravidlo
     * @param dir            adresář pravidel
     * @return seznam pravidel
     */
    private List<RulRule> processPackageRules(final PackageRules packageRules,
                                              final RulPackage rulPackage,
                                              final Map<String, ByteArrayInputStream> mapEntry,
                                              final RulRuleSet rulRuleSet,
                                              final File dir) {

        List<RulRule> rulPackageRules = packageRulesRepository.findByRulPackageAndRuleSet(rulPackage, rulRuleSet);
        List<RulRule> rulRuleNew = new ArrayList<>();

        if (packageRules != null && !CollectionUtils.isEmpty(packageRules.getPackageRules())) {
            for (PackageRule packageRule : packageRules.getPackageRules()) {
                List<RulRule> findItems = rulPackageRules.stream().filter(
                        (r) -> r.getFilename().equals(packageRule.getFilename())).collect(
                        Collectors.toList());
                RulRule item;
                if (findItems.size() > 0) {
                    item = findItems.get(0);
                } else {
                    item = new RulRule();
                }

                convertRulPackageRule(rulPackage, packageRule, item, rulRuleSet);
                rulRuleNew.add(item);
            }
        }

        rulRuleNew = packageRulesRepository.save(rulRuleNew);

        List<RulRule> rulRuleDelete = new ArrayList<>(rulPackageRules);
        rulRuleDelete.removeAll(rulRuleNew);
        packageRulesRepository.delete(rulRuleDelete);

        try {
            for (RulRule rule : rulRuleDelete) {
                deleteFile(dir, rule.getFilename());
            }

            for (RulRule rule : rulRuleNew) {
                saveFile(mapEntry, dir, ZIP_DIR_RULE_SET + "/" + rulRuleSet.getCode() + "/" + ZIP_DIR_RULES, rule.getFilename());
            }

            bulkActionConfigManager.load();
        } catch (IOException e) {
            throw new SystemException(e);
        }

        return rulRuleNew;

    }

    /**
     * Převod VO na DAO pravidla.
     *  @param rulPackage     balíček
     * @param packageRule    VO pravidla
     * @param rulPackageRule DAO pravidla
     * @param rulRuleSet     pravidlo
     */
    private void convertRulPackageRule(final RulPackage rulPackage,
                                       final PackageRule packageRule,
                                       final RulRule rulPackageRule,
                                       final RulRuleSet rulRuleSet) {

        rulPackageRule.setPackage(rulPackage);
        rulPackageRule.setFilename(packageRule.getFilename());
        rulPackageRule.setPriority(packageRule.getPriority());
        rulPackageRule.setRuleType(packageRule.getRuleType());
        rulPackageRule.setRuleSet(rulRuleSet);
    }

    /**
     * Zpracování hromadných akcí.
     *  @param packageActions   importovaných seznam hromadných akcí
     * @param rulPackage       balíček
     * @param mapEntry         mapa streamů souborů v ZIP
     * @param dir              adresář hromadných akcí  @return seznam hromadných akcí
     * @param rulRuleSet       pravidla
     */
    private List<RulAction> processPackageActions(final PackageActions packageActions,
                                                  final RulPackage rulPackage,
                                                  final Map<String, ByteArrayInputStream> mapEntry,
                                                  final File dir,
                                                  final RulRuleSet rulRuleSet) {

        List<RulAction> rulPackageActions = packageActionsRepository.findByRulPackage(rulPackage);
        List<RulAction> rulPackageActionsNew = new ArrayList<>();

        if (packageActions != null && !CollectionUtils.isEmpty(packageActions.getPackageActions())) {
            // procházím všechny definice akcí z pabíčku
            for (PackageAction packageAction : packageActions.getPackageActions()) {

                //vyhledám akci podle záznamů v DB, pokud existuje
                List<RulAction> findItems = rulPackageActions.stream().filter(
                        (r) -> r.getFilename().equals(packageAction.getFilename())).collect(
                        Collectors.toList());
                RulAction item;
                List<RulItemTypeAction> rulTypeActions;
                List<RulActionRecommended> rulActionRecommendeds;

                // pokud existuje v DB, vyhledám návazné typy atributů a doporučené akce,
                // jinak založím prázdné seznamy
                if (findItems.size() > 0) {
                    item = findItems.get(0);
                    rulTypeActions = itemTypeActionRepository.findByAction(item);
                    rulActionRecommendeds = actionRecommendedRepository.findByAction(item);
                } else {
                    item = new RulAction();
                    rulTypeActions = new ArrayList<>();
                    rulActionRecommendeds = new ArrayList<>();
                }

                // vytvořím/úpravím a uložím akci
                convertRulPackageAction(rulPackage, packageAction, item, rulRuleSet);
                packageActionsRepository.save(item);

                List<RulItemTypeAction> rulTypeActionsNew = new ArrayList<>();
                if (!CollectionUtils.isEmpty(packageAction.getActionItemTypes())) {
                    // pokud existují v balíčku u akce typy atributů, které se počítají,
                    // je potřeba je dohledat pokud existují v DB a následně upravit,
                    // nebo přidat/smazat

                    for (ActionItemType actionItemType : packageAction.getActionItemTypes()) {
                        RulItemTypeAction rulItemTypeAction = itemTypeActionRepository.findOneByItemTypeCodeAndAction(actionItemType.getItemType(), item);
                        RulItemType rulItemType = itemTypeRepository.findOneByCode(actionItemType.getItemType());

                        if (rulItemType == null) {
                            throw new BusinessException("RulItemType s code=" + actionItemType.getItemType() + " nenalezen", PackageCode.CODE_NOT_FOUND).set("code", actionItemType.getItemType() ).set("file", ITEM_TYPE_XML);
                        }

                        // pokud typ z balíčku ještě neexistuje v DB
                        if (rulItemTypeAction == null) {
                            rulItemTypeAction = new RulItemTypeAction();
                        }

                        rulItemTypeAction.setItemType(rulItemType);
                        rulItemTypeAction.setAction(item);
                        rulTypeActionsNew.add(rulItemTypeAction);
                    }

                    // uložení seznamu upravených/přidaných navázaných typů atributů
                    itemTypeActionRepository.save(rulTypeActionsNew);
                }
                // vyhledat a odstranit již nenavázané typy atributů z DB
                List<RulItemTypeAction> rulTypeActionsDelete = new ArrayList<>(rulTypeActions);
                rulTypeActionsDelete.removeAll(rulTypeActionsNew);
                itemTypeActionRepository.delete(rulTypeActionsDelete);

                List<RulActionRecommended> rulActionRecomendedsNew = new ArrayList<>();
                if (!CollectionUtils.isEmpty(packageAction.getActionRecommendeds())) {
                    // pokud existují v balíčku u akce typy výstupů, pro které jsou akce doporučené,
                    // je potřeba je dohledat pokud existují v DB a následně upravit, nebo přidat/smaza
                    for (ActionRecommended actionRecommended : packageAction.getActionRecommendeds()) {
                        RulActionRecommended rulActionRecommended = actionRecommendedRepository.findOneByOutputTypeCodeAndAction(actionRecommended.getOutputType(), item);
                        RulOutputType rulOutputType = outputTypeRepository.findOneByCode(actionRecommended.getOutputType());

                        if (rulOutputType == null) {
                            throw new BusinessException("RulOutputType s code=" + actionRecommended.getOutputType() + " nenalezen", PackageCode.CODE_NOT_FOUND).set("code", actionRecommended.getOutputType()).set("file", OUTPUT_TYPE_XML);
                        }

                        // pokud vazba na doporučenou akci ještě neexistuje v DB
                        if (rulActionRecommended == null) {
                            rulActionRecommended = new RulActionRecommended();
                        }

                        rulActionRecommended.setOutputType(rulOutputType);
                        rulActionRecommended.setAction(item);
                        rulActionRecomendedsNew.add(rulActionRecommended);
                    }

                    // uložení seznamu upravených/přidaných vazeb na doporučené akce
                    actionRecommendedRepository.save(rulActionRecomendedsNew);

                }
                // vyhkedat a odstranit již nenavázané doporučené akce
                List<RulActionRecommended> rulActionRecommendedsDelete = new ArrayList<>(rulActionRecommendeds);
                rulActionRecommendedsDelete.removeAll(rulActionRecomendedsNew);
                actionRecommendedRepository.delete(rulActionRecommendedsDelete);

                rulPackageActionsNew.add(item);
            }
        }

        // uložení nově vytvořených hromadných akcí
        rulPackageActionsNew = packageActionsRepository.save(rulPackageActionsNew);

        // smazání nedefinovaných hromadných akcí včetně vazeb
        List<RulAction> rulPackageActionsDelete = new ArrayList<>(rulPackageActions);
        rulPackageActionsDelete.removeAll(rulPackageActionsNew);
        for (RulAction rulAction : rulPackageActionsDelete) {
            itemTypeActionRepository.deleteByAction(rulAction);
            actionRecommendedRepository.deleteByAction(rulAction);
        }
        packageActionsRepository.delete(rulPackageActionsDelete);

        // odstranění/vytvoření definičních souborů pro hromadné akce
        try {
            for (RulAction action : rulPackageActionsDelete) {
                deleteFile(dir, action.getFilename());
            }

            for (RulAction action : rulPackageActionsNew) {
                saveFile(mapEntry, dir, ZIP_DIR_RULE_SET + "/" + rulRuleSet.getCode() + "/" + ZIP_DIR_ACTIONS, action.getFilename());
            }

            bulkActionConfigManager.load();
        } catch (IOException e) {
            throw new SystemException(e);
        }

        return rulPackageActionsNew;

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
     * Smazání souboru.
     *
     * @param dir      adresář
     * @param filename název souboru
     */
    private void forceDeleteFile(final File dir, final String filename) {
        File file = new File(dir.getPath() + File.separator + filename);

        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Odstranění rollback souborů.
     *
     * @param dir adresář
     */
    private void cleanBackupFiles(final File dir) {

        File[] files = dir.listFiles((dir1, name) -> name.endsWith(".bck"));

        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    /**
     * Provedení obnovy při selhání importu.
     *
     * @param dir adresář
     */
    private void rollBackFiles(final File dir) throws IOException {

        File[] files = dir.listFiles((dir1, name) -> name.endsWith(".bck"));

        if (files != null) {
            for (File file : files) {
                File fileMove = new File(StringUtils.stripEnd(file.getPath(), ".bck"));
                if (fileMove.exists()) {
                    fileMove.delete();
                }
                Files.move(file.toPath(), fileMove.toPath());
            }
        }
    }

    /**
     * Uložení souboru.
     *
     * @param mapEntry mapa streamů souborů v ZIP
     * @param dir      adresář
     * @param zipDir   adresář v ZIP
     * @param filename název souboru
     */
    private void saveFile(final Map<String, ByteArrayInputStream> mapEntry,
                          final File dir,
                          final String zipDir,
                          final String filename) throws IOException {

        File file = new File(dir.getPath() + File.separator + filename);

        if (file.exists()) {
            File fileMove = new File(dir.getPath() + File.separator + filename + ".bck");
            Files.move(file.toPath(), fileMove.toPath());
        }

        BufferedWriter output = null;
        try {
            output = new BufferedWriter(new FileWriter(file));
            ByteArrayInputStream byteArrayInputStream = mapEntry.get(zipDir + "/" + filename);

            if (byteArrayInputStream == null) {
                throw new IllegalStateException("Soubor " + zipDir + "/" + filename + " neexistuje v zip");
            }

            FileOutputStream bw = new FileOutputStream(file);

            byte[] buf = new byte[8192];
            for (; ; ) {
                int nread = byteArrayInputStream.read(buf, 0, buf.length);
                if (nread <= 0) {
                    break;
                }
                bw.write(buf, 0, nread);
            }

            bw.close();

        } finally {
            if (output != null) {
                output.close();
            }
        }

        mapEntry.keySet();

    }


    /**
     * Převod VO na DAO hromadné akce.
     *  @param rulPackage       balíček
     * @param packageAction    VO hromadné akce
     * @param rulPackageAction DAO hromadné akce
     * @param rulRuleSet       pravidla
     */
    private void convertRulPackageAction(final RulPackage rulPackage,
                                         final PackageAction packageAction,
                                         final RulAction rulPackageAction,
                                         final RulRuleSet rulRuleSet) {
        rulPackageAction.setPackage(rulPackage);
        rulPackageAction.setFilename(packageAction.getFilename());
        rulPackageAction.setRuleSet(rulRuleSet);
    }

    /**
     * Zpracování typů atributů.
     *
     * @param itemTypes       seznam importovaných typů
     * @param itemSpecs       seznam importovaných specifikací
     * @param rulPackage      balíček
     * @param rulRuleSet      pravidla
     * @return                výsledný seznam atributů v db
     */
    private List<RulItemType> processItemTypes(final ItemTypes itemTypes,
                                               final ItemSpecs itemSpecs,
                                               final RulPackage rulPackage,
                                               final RulRuleSet rulRuleSet) {
        List<RulDataType> rulDataTypes = dataTypeRepository.findAll();

        ItemTypeUpdater updater = AppContext.getBean(ItemTypeUpdater.class);

        return updater.update(rulDataTypes, rulPackage, itemTypes, itemSpecs, rulRuleSet);
    }

    /**
     * Zpracování typů atributů.
     *
     * @param outputTypes  seznam importovaných typů
     * @param templates    seznam importovaných specifikací
     * @param rulPackage   balíček
     * @param dirTemplates
     * @param rulRuleList  seznam souborů s pravidly
     * @param rulRuleSet   pravidla
     * @return výsledný seznam atributů v db
     */
    private List<RulOutputType> processOutputTypes(final OutputTypes outputTypes,
                                                   final Templates templates,
                                                   final RulPackage rulPackage,
                                                   final Map<String, ByteArrayInputStream> mapEntry,
                                                   final File dirTemplates,
                                                   final List<RulRule> rulRuleList,
                                                   final RulRuleSet rulRuleSet) {

        List<RulOutputType> rulOutputTypes = outputTypeRepository.findByRulPackageAndRuleSet(rulPackage, rulRuleSet);
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

                convertRulOutputType(rulPackage, outputType, item, rulRuleList, rulRuleSet);
                rulOutputTypesNew.add(item);
            }
        }

        rulOutputTypesNew = outputTypeRepository.save(rulOutputTypesNew);

        newRultemplates.addAll(processTemplates(templates, rulPackage, rulOutputTypesNew, mapEntry, dirTemplates, rulRuleSet));

        List<RulOutputType> rulPacketTypesDelete = new ArrayList<>(rulOutputTypes);
        rulPacketTypesDelete.removeAll(rulOutputTypesNew);

        if (!rulPacketTypesDelete.isEmpty()) {
            List<ArrOutputDefinition> byOutputTypes = outputDefinitionRepository.findByOutputTypes(rulPacketTypesDelete);
            if (!byOutputTypes.isEmpty()) {
                throw new IllegalStateException("Existuje výstup(y) navázáný na typ výstupu, který je v novém balíčku smazán.");
            }
            outputTypeRepository.delete(rulPacketTypesDelete);
        }

        return rulOutputTypesNew;
    }

    /**
     * Zpracování specifikací atributů.
     * @param templates       seznam importovaných specifikací
     * @param rulPackage          balíček
     * @param rulOutputTypes    seznam typů atributů
     * @param dirTemplates
     * @param rulRuleSet     pravidla
     */
    private List<RulTemplate> processTemplates(
            final Templates templates,
            final RulPackage rulPackage,
            final List<RulOutputType> rulOutputTypes,
            final Map<String, ByteArrayInputStream> mapEntry,
            final File dirTemplates,
            final RulRuleSet rulRuleSet) {
        List<RulTemplate> rulTemplate = templateRepository.findByRulPackage(rulPackage);
        List<RulTemplate> rulTemplateNew = new ArrayList<>();
        List<RulTemplate> rulTemplateActual = new ArrayList<>();

        if (templates != null && !CollectionUtils.isEmpty(templates.getTemplates())) {
            for (Template template : templates.getTemplates()) {
                List<RulTemplate> findItems = rulTemplate.stream()
                        .filter((r) -> r.getCode().equals(template.getCode())).collect(
                                Collectors.toList());
                RulTemplate item;

                boolean existTemplate = findItems.size() > 0;
                if (existTemplate) {
                    item = findItems.get(0);
                } else {
                    item = new RulTemplate();
                }

                convertRulTemplate(rulPackage, template, item, rulOutputTypes);
                if (existTemplate) {
                    rulTemplateActual.add(item);
                }
                rulTemplateNew.add(item);
            }
        }

        rulTemplateNew = templateRepository.save(rulTemplateNew);

        List<RulTemplate> rulTemplateToDelete = new ArrayList<>(rulTemplate);
        rulTemplateToDelete.removeAll(rulTemplateNew);
        if (!rulTemplateToDelete.isEmpty()) {
            // Check if there exists non deleted templates
            List<ArrOutputDefinition> byTemplate = outputDefinitionRepository.findNonDeletedByTemplatesAndStates(rulTemplateToDelete, Arrays.asList(OutputState.OPEN, OutputState.GENERATING, OutputState.COMPUTING));
            if (!byTemplate.isEmpty()) {
                StringBuilder sb = new StringBuilder().append("Existuje výstup(y), který nebyl vygenerován či smazán a je navázán na šablonu, která je v novém balíčku smazána.");
                byTemplate.forEach((a) -> {
                    ArrFund fund = a.getFund();
                    sb.append("\noutputDefinitionId: ").append(a.getOutputDefinitionId())
                            .append(", outputName: ").append(a.getName())
                            .append(", fundId: ").append(fund.getFundId())
                            .append(", fundName: ").append(fund.getName()).toString();

                });
                throw new IllegalStateException(sb.toString());
            }
            templateRepository.updateDeleted(rulTemplateToDelete, true);
        }

        try {
            deleteTemplates(dirTemplates, rulTemplateToDelete);
            deleteTemplates(dirTemplates, rulTemplateActual);

            importTemplatesFiles(mapEntry, dirTemplates, rulTemplateNew, rulRuleSet);

            return rulTemplateNew;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void deleteTemplates(final File dirTemplates, final List<RulTemplate> rulTemplateActual) throws IOException {
        for (RulTemplate template : rulTemplateActual) {
            File dirFile = new File(dirTemplates + File.separator + template.getDirectory());
            if (!dirFile.exists()) {
                continue;
            }
            for (File file : dirFile.listFiles()) {
                deleteFile(dirFile, file.getName());
            }
        }
    }

    private void deleteTemplatesForRollback(final File dirTemplates, final List<RulTemplate> rulTemplateActual) throws IOException {
        for (RulTemplate template : rulTemplateActual) {
            File dirFile = new File(dirTemplates + File.separator + template.getDirectory());
            if (!dirFile.exists()) {
                continue;
            }
            File[] files = dirFile.listFiles((dir1, name) -> !name.endsWith(".bck"));

            for (File file : files) {
                file.delete();
            }
        }
    }

    private void importTemplatesFiles(final Map<String, ByteArrayInputStream> mapEntry,
                                      final File dirTemplates,
                                      final List<RulTemplate> rulTemplateActual,
                                      final RulRuleSet rulRuleSet) throws IOException {
        for (RulTemplate template : rulTemplateActual) {
            final String templateDir = ZIP_DIR_RULE_SET + "/" + rulRuleSet.getCode() + "/" + ZIP_DIR_TEMPLATES + "/" + template.getDirectory();
            final String templateZipKeyDir = templateDir + "/";
            Set<String> templateFileKeys = mapEntry.keySet()
                    .stream()
                    .filter(key -> key.startsWith(templateZipKeyDir) && !key.equals(templateZipKeyDir))
                    .map(key -> key.replace(templateZipKeyDir, ""))
                    .collect(Collectors.toSet());
            File dirFile = new File(dirTemplates + File.separator + template.getDirectory());
            if (!dirFile.exists() && !dirFile.mkdirs()) {
                throw new IOException("Nepodařilo se vytvořit složku.");
            }
            for (String file : templateFileKeys) {
                saveFile(mapEntry, dirFile, templateDir, file);
            }
            try {
                bulkActionConfigManager.load();
            }
            catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }


    /**
     * Převod VO na DAO Templaty outputů
     *
     * @param rulPackage       balíček
     * @param template     VO template
     * @param rulTemplate  DAO template
     * @param rulOutputTypes seznam typů outputů
     */
    private void convertRulTemplate(final RulPackage rulPackage, final Template template, final RulTemplate rulTemplate, final List<RulOutputType> rulOutputTypes) {
        rulTemplate.setName(template.getName());
        rulTemplate.setCode(template.getCode());
        rulTemplate.setEngine(Engine.valueOf(template.getEngine()));
        rulTemplate.setPackage(rulPackage);
        rulTemplate.setDirectory(template.getDirectory());
        rulTemplate.setMimeType(template.getMimeType());
        rulTemplate.setExtension(template.getExtension());
        rulTemplate.setDeleted(false);

        List<RulOutputType> findItems = rulOutputTypes.stream()
                .filter((r) -> r.getCode().equals(template.getOutputType()))
                .collect(Collectors.toList());

        RulOutputType item;

        if (findItems.size() > 0) {
            item = findItems.get(0);
        } else {
            throw new IllegalStateException("Kód " + template.getOutputType() + " neexistuje v RulOutputType");
        }

        rulTemplate.setOutputType(item);
    }

    /**
     * Zpracování pravidel.
     *
     * @param ruleSets          seznam importovaných pravidel
     * @param rulPackage        balíček
     * @param rulRuleSetsDelete výstupní parametr pro pravidla ke smazání na konci importu
     * @return seznam pravidel
     */
    private List<RulRuleSet> processRuleSets(final RuleSets ruleSets,
                                             final RulPackage rulPackage,
                                             final List<RulRuleSet> rulRuleSetsDelete) {

        List<RulRuleSet> rulRuleSets = ruleSetRepository.findByRulPackage(rulPackage);
        List<RulRuleSet> rulRuleSetsNew = new ArrayList<>();

        if (ruleSets != null && !CollectionUtils.isEmpty(ruleSets.getRuleSets())) {
            for (RuleSet ruleSet : ruleSets.getRuleSets()) {
                List<RulRuleSet> findItems = rulRuleSets.stream().filter((r) -> r.getCode().equals(ruleSet.getCode()))
                        .collect(
                                Collectors.toList());
                RulRuleSet item;
                if (findItems.size() > 0) {
                    item = findItems.get(0);
                } else {
                    item = new RulRuleSet();
                }

                convertRuleSet(rulPackage, ruleSet, item);
                rulRuleSetsNew.add(item);
            }
        }

        // Uložení pravidel
        rulRuleSetsNew = ruleSetRepository.save(rulRuleSetsNew);

        // Naplnění pravidel ke smazání, které již nejsou v xml
        rulRuleSetsDelete.addAll(rulRuleSets);
        rulRuleSetsDelete.removeAll(rulRuleSetsNew);

        return rulRuleSetsNew;
    }

    /**
     * Smazání pravidla.
     * @param rulRuleSets pravidla
     */
    private void deleteRuleSets(final Collection<RulRuleSet> rulRuleSets) {
        ruleSetRepository.delete(rulRuleSets);
    }

    /**
     * Převod VO na DAO pravidla.
     *
     * @param rulPackage balíček
     * @param ruleSet    VO pravidla
     * @param rulRuleSet DAO pravidla
     */
    private void convertRuleSet(final RulPackage rulPackage, final RuleSet ruleSet, final RulRuleSet rulRuleSet) {
        rulRuleSet.setCode(ruleSet.getCode());
        rulRuleSet.setName(ruleSet.getName());
        rulRuleSet.setPackage(rulPackage);
    }

    /**
     * Zpracování importovaného balíčku.
     *
     * @param packageInfo VO importovaného balíčku
     */
    private RulPackage processRulPackage(final PackageInfo packageInfo) {
        RulPackage rulPackage = packageRepository.findTopByCode(packageInfo.getCode());

        if (rulPackage == null) {
            rulPackage = new RulPackage();
        } else {
            if (BooleanUtils.isNotTrue(getTesting()) && rulPackage.getVersion().equals(packageInfo.getVersion())) {
                throw new BusinessException("Verze (" + packageInfo.getVersion() + ") balíčku (" + rulPackage.getCode() + ") byla již aplikována", PackageCode.VERSION_APPLIED).set("code", rulPackage.getCode()).set("version", packageInfo.getVersion())
                        .set("version", rulPackage.getVersion());
            }
        }

        rulPackage.setCode(packageInfo.getCode());
        rulPackage.setName(packageInfo.getName());
        rulPackage.setDescription(packageInfo.getDescription());
        rulPackage.setVersion(packageInfo.getVersion());

        rulPackage = packageRepository.save(rulPackage);

        processRulPackageDependencies(packageInfo, rulPackage);

        detectCyclicDependencies();
        return rulPackage;
    }

    /**
     * Zkontroluje závislosti, zda-li neobsahují cyklus.
     */
    private void detectCyclicDependencies() {
        packageDependencyRepository.flush();
        List<RulPackageDependency> packageDependencies = packageDependencyRepository.findAll();

        Map<Integer, Set<Integer>> dependencies = new HashMap<>();
        for (RulPackageDependency packageDependency : packageDependencies) {
            Set<Integer> targetIds = dependencies.computeIfAbsent(packageDependency.getSourcePackageId(), k -> new HashSet<>());
            targetIds.add(packageDependency.getTargetPackageId());
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
        packageDependencyRepository.deleteBySourcePackage(rulPackage);

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
            packageDependency.setSourcePackage(rulPackage);
            packageDependency.setTargetPackage(requiredDependency);
            newDependencies.add(packageDependency);
        }
        packageDependencyRepository.save(newDependencies);
    }

    /**
     * Smazání importovaného balíčku podle kódu.
     *
     * @param code kód balíčku
     */
    public void deletePackage(final String code) {
        RulPackage rulPackage = packageRepository.findTopByCode(code);

        if (rulPackage == null) {
            throw new ObjectNotFoundException("Balíček s kódem " + code + " neexistuje", BaseCode.ID_NOT_EXIST);
        }

        // kontrola na existující zavíslostí z jiných balíčků
        List<RulPackageDependency> targetPackages = packageDependencyRepository.findByTargetPackage(rulPackage);
        if (targetPackages.size() > 0) {
            throw new BusinessException("Balíček nelze odebrat, protože je používán jiným balíčkem", PackageCode.FOREIGN_DEPENDENCY)
                    .set("foreignPackageCodes", targetPackages.stream().map(pd -> pd.getSourcePackage().getCode()).collect(Collectors.toList()));
        }
        packageDependencyRepository.deleteBySourcePackage(rulPackage);

        List<RulItemSpec> rulDescItemSpecs = itemSpecRepository.findByRulPackage(rulPackage);
        for (RulItemSpec rulDescItemSpec : rulDescItemSpecs) {
            itemSpecRegisterRepository.deleteByItemSpec(rulDescItemSpec);
        }
        itemSpecRepository.delete(rulDescItemSpecs);

        List<RulRuleSet> ruleSets = ruleSetRepository.findByRulPackage(rulPackage);
        List<RulRule> rules = packageRulesRepository.findByRulPackage(rulPackage);
        List<RulAction> actions = packageActionsRepository.findByRulPackage(rulPackage);

        packageActionsRepository.findByRulPackage(rulPackage).forEach(this::deleteActionLink);
        itemTypeRepository.deleteByRulPackage(rulPackage);
        packageActionsRepository.deleteByRulPackage(rulPackage);
        packageRulesRepository.deleteByRulPackage(rulPackage);
        policyTypeRepository.deleteByRulPackage(rulPackage);
        packetTypeRepository.deleteByRulPackage(rulPackage);
        templateRepository.deleteByRulPackage(rulPackage);
        outputTypeRepository.deleteByRulPackage(rulPackage);
        deleteRuleSets(ruleSetRepository.findByRulPackage(rulPackage));
        registryRoleRepository.deleteByRulPackage(rulPackage);
        registerTypeRepository.preDeleteByRulPackage(rulPackage);
        registerTypeRepository.deleteByRulPackage(rulPackage);
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
        packageRepository.delete(rulPackage);

        entityManager.flush();

        for (RulRuleSet ruleSet : ruleSets) {
            File dirActions = new File(bulkActionConfigManager.getFunctionsDir(ruleSet.getCode()));
            File dirRules = new File(rulesExecutor.getDroolsDir(ruleSet.getCode()));

            try {

                for (RulRule rulPackageRule : rules) {
                    deleteFile(dirRules, rulPackageRule.getFilename());
                }

                for (RulAction rulPackageAction : actions) {
                    deleteFile(dirActions, rulPackageAction.getFilename());
                }

                entityManager.flush();

                cleanBackupFiles(dirActions);
                cleanBackupFiles(dirRules);

                bulkActionConfigManager.load();
            } catch (Exception e) {
                try {
                    rollBackFiles(dirActions);
                    rollBackFiles(dirRules);

                    bulkActionConfigManager.load();
                    throw new SystemException("Nastala chyba během importu balíčku", e);
                } catch (IOException e1) {
                    throw new SystemException("Nastala chyba během obnovy souborů po selhání importu balíčku", e);
                }
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
     * @param code kód balíčku
     * @return výsledný soubor
     */
    public File exportPackage(final String code) {
        RulPackage rulPackage = packageRepository.findTopByCode(code);

        if (rulPackage == null) {
            throw new ObjectNotFoundException("Balíček s kódem " + code + " neexistuje", PackageCode.PACKAGE_NOT_EXIST).set("code", code);
        }

        File file = null;
        FileOutputStream fos = null;
        ZipOutputStream zos = null;

        try {
            file = File.createTempFile(code + "-", "-package.zip");
            fos = new FileOutputStream(file);
            zos = new ZipOutputStream(fos);

            exportPackageInfo(rulPackage, zos);
            exportRuleSet(rulPackage, zos);
            exportDescItemSpecs(rulPackage, zos);
            exportDescItemTypes(rulPackage, zos);
            exportPackageActions(rulPackage, zos);
            exportPackageRules(rulPackage, zos);
            exportPacketTypes(rulPackage, zos);
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

            file.deleteOnExit();
        } catch (Exception e) {

            if (file != null) {
                file.delete();
            }

            throw new IllegalStateException(e);

        } finally {

            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return file;

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
        ruleSetList.add(null);
        Collection<UISettings.SettingsType> settingsTypesRule = UISettings.SettingsType.findByType(UISettings.EntityType.RULE);
        Collection<UISettings.SettingsType> settingsTypesItemType = UISettings.SettingsType.findByType(UISettings.EntityType.ITEM_TYPE);

        for (UISettings uiSetting : uiSettings) {
            if (settingsTypesRule.contains(uiSetting.getSettingsType())) {
                Integer ruleSetId = uiSetting.getEntityId();
                if (!ruleSetIdAdd.contains(ruleSetId)) {
                    ruleSetIdAdd.add(ruleSetId);
                    ruleSetList.add(ruleSetMap.get(ruleSetId));
                }
            } else if (settingsTypesItemType.contains(uiSetting.getSettingsType())) {
                Integer itemTypeId = uiSetting.getEntityId();
                RulItemType rulItemType = itemTypeMap.get(itemTypeId);
                Integer ruleSetId = rulItemType.getRuleSet().getRuleSetId();
                if (!ruleSetIdAdd.contains(ruleSetId)) {
                    ruleSetIdAdd.add(ruleSetId);
                    ruleSetList.add(ruleSetMap.get(ruleSetId));
                }
            }
        }

        for (RulRuleSet ruleSet : ruleSetList) {
            String path = ruleSet == null ? SETTING_XML : ZIP_DIR_RULE_SET + "/" + ruleSet.getCode() + "/" + SETTING_XML;
            export(rulPackage, zos, settingsRepository, Settings.class, Setting.class,
                    (settingList, settings) -> settings.setSettings(settingList),
                    (uiSetting, setting) -> setting.setValue(uiSetting.getValue()),
                    this::convertSetting, (s) -> filterSettingByType(s, ruleSet), path, ruleSet);
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
            RuleSystem ruleSystem = staticDataService.getData().getRuleSystems().getByRuleSetId(ruleSet.getRuleSetId());
            RuleSystemItemType itemType = ruleSystem.getItemTypeById(setting.getEntityId());
            return itemType != null;
        }

        if (ruleSet != null) {
            return false;
        }

        return true;
    }

    public Setting convertSetting(final UISettings uiSettings, final RulRuleSet ruleSet) {
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
        } else if (Objects.equals(uiSettings.getSettingsType(), UISettings.SettingsType.GRID_VIEW)) {
            entity = new SettingGridView();
        } else {
            entity = new SettingBase();
        }
        entity.setSettingsType(uiSettings.getSettingsType());
        entity.setEntityType(uiSettings.getEntityType());
        entity.setEntityId(uiSettings.getEntityId());
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
        RegisterTypes registerTypes = new RegisterTypes();
        List<RegRegisterType> regRegisterTypes = registerTypeRepository.findByRulPackage(rulPackage);
        if (regRegisterTypes.size() == 0) {
            return;
        }
        List<RegisterType> registerTypeList = new ArrayList<>(regRegisterTypes.size());
        registerTypes.setRegisterTypes(registerTypeList);

        for (RegRegisterType regRegisterType : regRegisterTypes) {
            RegisterType registerType = new RegisterType();
            convertRegisterType(regRegisterType, registerType);
            registerTypeList.add(registerType);
        }

        addObjectToZipFile(registerTypes, zos, REGISTER_TYPE_XML);
    }

    private void convertRegisterType(final RegRegisterType regRegisterType, final RegisterType registerType) {
        registerType.setName(regRegisterType.getName());
        registerType.setCode(regRegisterType.getCode());
        registerType.setAddRecord(regRegisterType.getAddRecord());
        registerType.setHierarchical(regRegisterType.getHierarchical());
        registerType.setPartyType(regRegisterType.getPartyType() == null ? null : regRegisterType.getPartyType().getCode());
        registerType.setParentRegisterType(regRegisterType.getParentRegisterType() == null ? null : regRegisterType.getParentRegisterType().getCode());
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
        registryRole.setRegisterType(parRegistryRole.getRegisterType().getCode());
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
            List<Template> templateList = new ArrayList<>(rulTemplatesList.size());
            outputTypes.setTemplates(templateList);
            String ruleSetCode = entry.getKey().getCode();
            for (RulTemplate rulOutputType : rulTemplatesList) {
                Template outputType = new Template();
                convertTemplate(rulOutputType, outputType);
                templateList.add(outputType);
                File dir = new File(outputGeneratorService.getTemplatesDir(rulPackage.getCode()) + File.separator + rulOutputType.getDirectory() + File.separator);
                for (File dirFile : dir.listFiles()) {
                    addToZipFile(ZIP_DIR_RULE_SET + "/" + ruleSetCode + "/" + ZIP_DIR_TEMPLATES + "/" + rulOutputType.getDirectory() + "/" + dirFile.getName(), dirFile, zos);
                }
            }

            addObjectToZipFile(outputTypes, zos, ZIP_DIR_RULE_SET + "/" + ruleSetCode + "/" + TEMPLATE_XML);
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
        List<RuleSet> ruleSetList = new ArrayList<>(rulRuleSets.size());
        ruleSets.setRuleSets(ruleSetList);

        for (RulRuleSet rulRuleSet : rulRuleSets) {
            RuleSet ruleSet = new RuleSet();
            covertRuleSet(rulRuleSet, ruleSet);
            ruleSetList.add(ruleSet);
        }

        addObjectToZipFile(ruleSets, zos, RULE_SET_XML);
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

        List<RulPackageDependency> dependencies = packageDependencyRepository.findBySourcePackage(rulPackage);
        packageInfo.setDependencies(dependencies.stream()
                .map(d -> {
                    PackageDependency pd = new PackageDependency();
                    pd.setCode(d.getTargetPackage().getCode());
                    pd.setMinVersion(d.getMinVersion());
                    return pd;
                }).collect(Collectors.toList()));

        addObjectToZipFile(packageInfo, zos, PACKAGE_XML);
    }

    /**
     * Exportování packet typů.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportPacketTypes(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        List<RulPacketType> rulPacketTypes = packetTypeRepository.findByRulPackage(rulPackage);

        if (rulPacketTypes.size() == 0) {
            return;
        }

        Map<RulRuleSet, List<RulPacketType>> ruleSetPacketTypeMap = rulPacketTypes.stream()
                .collect(Collectors.groupingBy(RulPacketType::getRuleSet));

        for (Map.Entry<RulRuleSet, List<RulPacketType>> entry : ruleSetPacketTypeMap.entrySet()) {
            PacketTypes packetTypes = new PacketTypes();
            List<RulPacketType> rulPacketTypeList = entry.getValue();
            List<PacketType> packetTypeList = new ArrayList<>(rulPacketTypeList.size());
            packetTypes.setPacketTypes(packetTypeList);

            for (RulPacketType rulPacketType : rulPacketTypeList) {
                PacketType packetType = new PacketType();
                covertPacketType(rulPacketType, packetType);
                packetTypeList.add(packetType);
            }

            addObjectToZipFile(packetTypes, zos, ZIP_DIR_RULE_SET + "/" + entry.getKey().getCode() + "/" + PACKET_TYPE_XML);
        }
    }

    /**
     * Exportování pravidel.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportPackageRules(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        List<RulRule> rulPackageRules = packageRulesRepository.findByRulPackage(rulPackage);

        if (rulPackageRules.size() == 0) {
            return;
        }

        Map<RulRuleSet, List<RulRule>> ruleSetRuleMap = rulPackageRules.stream()
                .collect(Collectors.groupingBy(RulRule::getRuleSet));

        for (Map.Entry<RulRuleSet, List<RulRule>> entry : ruleSetRuleMap.entrySet()) {
            PackageRules packageRules = new PackageRules();
            List<RulRule> ruleList = entry.getValue();
            List<PackageRule> packageRuleList = new ArrayList<>(ruleList.size());
            packageRules.setPackageRules(packageRuleList);
            String ruleSetCode = entry.getKey().getCode();
            for (RulRule rulPackageRule : ruleList) {
                PackageRule packageRule = new PackageRule();
                convertPackageRule(rulPackageRule, packageRule);
                packageRuleList.add(packageRule);

                addToZipFile(ZIP_DIR_RULE_SET + "/" + ruleSetCode + "/" + ZIP_DIR_RULES + "/" + rulPackageRule.getFilename(), new File(rulesExecutor.getDroolsDir(ruleSetCode)
                        + File.separator + rulPackageRule.getFilename()), zos);

            }

            addObjectToZipFile(packageRules, zos, ZIP_DIR_RULE_SET + "/" + ruleSetCode + "/" + PACKAGE_RULES_XML);
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
            PackageActions packageActions = new PackageActions();
            List<RulAction> actionList = entry.getValue();
            List<PackageAction> packageActionList = new ArrayList<>(actionList.size());
            packageActions.setPackageActions(packageActionList);
            String ruleSetCode = entry.getKey().getCode();
            for (RulAction rulPackageAction : actionList) {
                PackageAction packageAction = new PackageAction();
                convertPackageAction(rulPackageAction, packageAction);
                packageActionList.add(packageAction);

                addToZipFile(ZIP_DIR_RULE_SET + "/" + ruleSetCode + "/" + ZIP_DIR_ACTIONS + "/" + rulPackageAction.getFilename(),
                        new File(bulkActionConfigManager.getFunctionsDir(ruleSetCode) + File.separator + rulPackageAction.getFilename()), zos);
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
    private void exportDescItemTypes(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        List<RulItemType> rulDescItemTypes = itemTypeRepository.findByRulPackageOrderByViewOrderAsc(rulPackage);
        if (rulDescItemTypes.size() == 0) {
            return;
        }

        Map<RulRuleSet, List<RulItemType>> ruleSetTypesMap = rulDescItemTypes.stream()
                .collect(Collectors.groupingBy(RulItemType::getRuleSet));

        for (Map.Entry<RulRuleSet, List<RulItemType>> entry : ruleSetTypesMap.entrySet()) {
            ItemTypes itemTypes = new ItemTypes();
            List<RulItemType> typeList = entry.getValue();
            List<ItemType> itemTypeList = new ArrayList<>(typeList.size());
            itemTypes.setItemTypes(itemTypeList);

            for (RulItemType rulDescItemType : typeList) {
                ItemType itemType = new ItemType();
                convertDescItemType(rulDescItemType, itemType);
                itemTypeList.add(itemType);
            }

            addObjectToZipFile(itemTypes, zos, ZIP_DIR_RULE_SET + "/" + entry.getKey().getCode() + "/" + ITEM_TYPE_XML);
        }
    }

    /**
     * Exportování specifikací atributů.
     *  @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportDescItemSpecs(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        List<RulItemSpec> rulDescItemSpecs = itemSpecRepository.findByRulPackage(rulPackage);
        if (rulDescItemSpecs.size() == 0) {
            return;
        }

        Map<RulRuleSet, List<RulItemSpec>> ruleSetSpecsMap = rulDescItemSpecs.stream()
                .collect(Collectors.groupingBy(spec -> spec.getItemType().getRuleSet()));

        for (Map.Entry<RulRuleSet, List<RulItemSpec>> entry : ruleSetSpecsMap.entrySet()) {
            ItemSpecs itemSpecs = new ItemSpecs();
            List<RulItemSpec> specList = entry.getValue();
            List<ItemSpec> itemSpecList = new ArrayList<>(specList.size());
            itemSpecs.setItemSpecs(itemSpecList);

            for (RulItemSpec rulDescItemSpec : specList) {
                ItemSpec itemSpec = new ItemSpec();
                convertDescItemSpec(rulDescItemSpec, itemSpec);
                itemSpecList.add(itemSpec);
            }

            addObjectToZipFile(itemSpecs, zos, ZIP_DIR_RULE_SET + "/" + entry.getKey().getCode() + "/" + ITEM_SPEC_XML);
        }
    }

    /**
     * Převod DAO na VO pravidla.
     *
     * @param rulRuleSet DAO pravidla
     * @param ruleSet    VO pravidla
     */
    private void covertRuleSet(final RulRuleSet rulRuleSet, final RuleSet ruleSet) {
        ruleSet.setCode(rulRuleSet.getCode());
        ruleSet.setName(rulRuleSet.getName());
    }

    /**
     * Převod DAO na VO typu packet.
     *
     * @param rulPacketType DAO packet
     * @param packetType    VO packet
     */
    private void covertPacketType(final RulPacketType rulPacketType, final PacketType packetType) {
        packetType.setCode(rulPacketType.getCode());
        packetType.setName(rulPacketType.getName());
        packetType.setShortcut(rulPacketType.getShortcut());
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
        outputType.setFilename(rulOutputType.getRule() == null ? null : rulOutputType.getRule().getFilename());
    }

    /**
     * Převod DAO na VO template.
     *
     * @param rulOutputType DAO packet
     * @param outputType    VO packet
     */
    private void convertTemplate(final RulTemplate rulOutputType, final Template outputType) {
        outputType.setCode(rulOutputType.getCode());
        outputType.setName(rulOutputType.getName());
        outputType.setDirectory(rulOutputType.getDirectory());
        outputType.setEngine(rulOutputType.getEngine().toString());
        outputType.setOutputType(rulOutputType.getOutputType().getCode());
        outputType.setMimeType(rulOutputType.getMimeType());
        outputType.setExtension(rulOutputType.getExtension());
    }

    /**
     * Převod DAO na VO typů atributu.
     *
     * @param rulDescItemType DAO typy
     * @param itemType    VO typu
     */
    private void convertDescItemType(final RulItemType rulDescItemType, final ItemType itemType) {
        itemType.setCode(rulDescItemType.getCode());
        itemType.setName(rulDescItemType.getName());
        itemType.setShortcut(rulDescItemType.getShortcut());
        itemType.setCanBeOrdered(rulDescItemType.getCanBeOrdered());
        itemType.setDataType(rulDescItemType.getDataType().getCode());
        itemType.setDescription(rulDescItemType.getDescription());
        itemType.setIsValueUnique(rulDescItemType.getIsValueUnique());
        itemType.setUseSpecification(rulDescItemType.getUseSpecification());

        List<ElzaColumn> columnsDefinition = rulDescItemType.getColumnsDefinition();
        if (columnsDefinition != null) {
            List<Column> columns = new ArrayList<>(columnsDefinition.size());
            for (ElzaColumn elzaColumn : columnsDefinition) {
                Column column = new Column();
                column.setCode(elzaColumn.getCode());
                column.setName(elzaColumn.getName());
                column.setDataType(elzaColumn.getDataType().name());
                column.setWidth(elzaColumn.getWidth());
                columns.add(column);
            }
            itemType.setColumnsDefinition(columns);
        }

    }

    /**
     * Převod DAO na VO specifikace.
     *
     * @param rulDescItemSpec DAO specifikace
     * @param itemSpec    VO specifikace
     */
    private void convertDescItemSpec(final RulItemSpec rulDescItemSpec, final ItemSpec itemSpec) {
        itemSpec.setCode(rulDescItemSpec.getCode());
        itemSpec.setName(rulDescItemSpec.getName());
        itemSpec.setDescription(rulDescItemSpec.getDescription());
        itemSpec.setItemType(rulDescItemSpec.getItemType().getCode());
        itemSpec.setShortcut(rulDescItemSpec.getShortcut());

        List<RulItemSpecRegister> rulItemSpecRegisters = itemSpecRegisterRepository
                .findByDescItemSpecId(rulDescItemSpec);

        List<ItemSpecRegister> itemSpecRegisterList = new ArrayList<>(rulItemSpecRegisters.size());

        for (RulItemSpecRegister rulItemSpecRegister : rulItemSpecRegisters) {
            ItemSpecRegister itemSpecRegister = new ItemSpecRegister();
            convertDescItemSpecRegister(rulItemSpecRegister, itemSpecRegister);
            itemSpecRegisterList.add(itemSpecRegister);
        }

        itemSpec.setItemSpecRegisters(itemSpecRegisterList);

        if (StringUtils.isNotEmpty(rulDescItemSpec.getCategory())) {
            String[] categoriesString = rulDescItemSpec.getCategory().split("\\" + ItemTypeUpdater.CATEGORY_SEPARATOR);
            List<Category> categories = Arrays.asList(categoriesString).stream()
                    .map(s -> new Category(s))
                    .collect(Collectors.toList());
            itemSpec.setCategories(categories);
        }
    }

    /**
     * Převod DAO na VO napojení specifikací na reg.
     *
     * @param rulItemSpecRegister DAO napojení specifikací
     * @param itemSpecRegister    VO napojení specifikací
     */
    private void convertDescItemSpecRegister(final RulItemSpecRegister rulItemSpecRegister,
                                             final ItemSpecRegister itemSpecRegister) {
        itemSpecRegister.setRegisterType(rulItemSpecRegister.getRegisterType().getCode());
    }

    /**
     * Převod DAO na VO pravidla.
     *
     * @param rulPackageRule DAO pravidla
     * @param packageRule    VO pravidla
     */
    private void convertPackageRule(final RulRule rulPackageRule, final PackageRule packageRule) {
        packageRule.setFilename(rulPackageRule.getFilename());
        packageRule.setPriority(rulPackageRule.getPriority());
        packageRule.setRuleType(rulPackageRule.getRuleType());
    }

    /**
     * Převod DAO na VO hromadné akce.
     *
     * @param rulPackageAction DAO hromadné akce
     * @param packageAction    VO hromadné akce
     */
    private void convertPackageAction(final RulAction rulPackageAction, final PackageAction packageAction) {
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
        FileInputStream fis = new FileInputStream(file);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zos.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }
        zos.closeEntry();
        fis.close();
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
        addToZipFile(fileName, xmlFile, zos);
        xmlFile.delete();
    }

    public Boolean getTesting() {
        return testing;
    }

    public void setTesting(final Boolean testing) {
        this.testing = testing;
    }
}

