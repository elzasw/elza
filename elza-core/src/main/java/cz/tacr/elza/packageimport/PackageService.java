package cz.tacr.elza.packageimport;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import cz.tacr.elza.api.ArrOutputDefinition.OutputState;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.repository.OutputDefinitionRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.collect.Maps;

import cz.tacr.elza.bulkaction.BulkActionConfigManager;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.domain.RulActionRecommended;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDefaultItemType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecRegister;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeAction;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulPolicyType;
import cz.tacr.elza.domain.RulRule;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.drools.RulesExecutor;
import cz.tacr.elza.packageimport.xml.ActionItemType;
import cz.tacr.elza.packageimport.xml.ActionRecommended;
import cz.tacr.elza.packageimport.xml.Column;
import cz.tacr.elza.packageimport.xml.DescItemSpecRegister;
import cz.tacr.elza.packageimport.xml.ItemSpec;
import cz.tacr.elza.packageimport.xml.ItemSpecs;
import cz.tacr.elza.packageimport.xml.ItemType;
import cz.tacr.elza.packageimport.xml.ItemTypes;
import cz.tacr.elza.packageimport.xml.OutputType;
import cz.tacr.elza.packageimport.xml.OutputTypes;
import cz.tacr.elza.packageimport.xml.PackageAction;
import cz.tacr.elza.packageimport.xml.PackageActions;
import cz.tacr.elza.packageimport.xml.PackageInfo;
import cz.tacr.elza.packageimport.xml.PackageRule;
import cz.tacr.elza.packageimport.xml.PackageRules;
import cz.tacr.elza.packageimport.xml.PacketType;
import cz.tacr.elza.packageimport.xml.PacketTypes;
import cz.tacr.elza.packageimport.xml.PolicyType;
import cz.tacr.elza.packageimport.xml.PolicyTypes;
import cz.tacr.elza.packageimport.xml.RuleSet;
import cz.tacr.elza.packageimport.xml.RuleSets;
import cz.tacr.elza.packageimport.xml.Template;
import cz.tacr.elza.packageimport.xml.Templates;
import cz.tacr.elza.repository.ActionRecommendedRepository;
import cz.tacr.elza.repository.ActionRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.DefaultItemTypeRepository;
import cz.tacr.elza.repository.ItemSpecRegisterRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeActionRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.OutputTypeRepository;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.repository.PacketTypeRepository;
import cz.tacr.elza.repository.PolicyTypeRepository;
import cz.tacr.elza.repository.RuleRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.TemplateRepository;
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
     * Složka templatů
     */
    private final String ZIP_DIR_TEMPLATES = "templates";

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
    private DefaultItemTypeRepository defaultItemTypeRepository;

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

    private List<RulTemplate> newRultemplates = null;

    /**
     * Provede import balíčku.
     *
     * @param file soubor balíčku
     */
    public void importPackage(final File file) {
        File dirActions = new File(bulkActionConfigManager.getPath());
        File dirRules = new File(rulesExecutor.getRootPath());
        File dirTemplates = new File(outputGeneratorService.getTemplatesDir());

        ZipFile zipFile = null;
        List<RulAction> rulPackageActions = null;
        List<RulRule> rulPackageRules = null;
        List<RulTemplate> originalRulTemplates = null;

        try {

            zipFile = new ZipFile(file);

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            Map<String, ByteArrayInputStream> mapEntry = createStreamsMap(zipFile, entries);

            // načtení info o importu
            PackageInfo packageInfo = convertXmlStreamToObject(PackageInfo.class, mapEntry.get(PACKAGE_XML));

            RulPackage rulPackage = processRulPackage(packageInfo);

            originalRulTemplates = templateRepository.findByRulPackage(rulPackage);

            RuleSets ruleSets = convertXmlStreamToObject(RuleSets.class, mapEntry.get(RULE_SET_XML));

            PolicyTypes policyTypes = convertXmlStreamToObject(PolicyTypes.class, mapEntry.get(POLICY_TYPE_XML));

            ItemSpecs itemSpecs = convertXmlStreamToObject(ItemSpecs.class, mapEntry.get(ITEM_SPEC_XML));
            ItemTypes itemTypes = convertXmlStreamToObject(ItemTypes.class, mapEntry.get(ITEM_TYPE_XML));

            PackageActions packageActions = convertXmlStreamToObject(PackageActions.class, mapEntry.get(PACKAGE_ACTIONS_XML));

            PackageRules packageRules = convertXmlStreamToObject(PackageRules.class, mapEntry.get(PACKAGE_RULES_XML));

            OutputTypes outputTypes = convertXmlStreamToObject(OutputTypes.class, mapEntry.get(OUTPUT_TYPE_XML));

            Templates templates = convertXmlStreamToObject(Templates.class, mapEntry.get(TEMPLATE_XML));

            PacketTypes packetTypes = convertXmlStreamToObject(PacketTypes.class, mapEntry.get(PACKET_TYPE_XML));

            processPacketTypes(packetTypes, rulPackage);

            List<RulRuleSet> rulRuleSets = processRuleSets(ruleSets, rulPackage);
            rulPackageRules = processPackageRules(packageRules, rulPackage, mapEntry, rulRuleSets, dirRules);

            List<RulOutputType> rulOutputTypes = processOutputTypes(outputTypes, templates, rulPackage, mapEntry, dirTemplates, rulPackageRules);

            processPolicyTypes(policyTypes, rulPackage, rulRuleSets);

            List<RulItemType> rulDescItemTypes = processItemTypes(itemTypes, itemSpecs, rulPackage);
            rulPackageActions = processPackageActions(packageActions, rulPackage, mapEntry, dirActions);

            // Zde se může importovat vazba mezi pravidlem a atributem
            processDefaultItemTypes(rulRuleSets, ruleSets, rulDescItemTypes);


            entityManager.flush();

            cleanBackupFiles(dirActions);
            cleanBackupFiles(dirRules);

            if (originalRulTemplates != null) {
                cleanBackupTemplates(dirTemplates, originalRulTemplates);
            }
            if (newRultemplates != null) {
                cleanBackupTemplates(dirTemplates, newRultemplates);
            }

            eventNotificationService.publishEvent(new ActionEvent(EventType.PACKAGE));

        } catch (Exception e) {
            try {
                if (rulPackageActions != null) {
                    for (RulAction rulPackageAction : rulPackageActions) {
                        forceDeleteFile(dirActions, rulPackageAction.getFilename());
                    }
                }

                if (rulPackageRules != null) {
                    for (RulRule rulPackageRule : rulPackageRules) {
                        forceDeleteFile(dirRules, rulPackageRule.getFilename());
                    }
                }

                if (newRultemplates != null) {
                    deleteTemplates(dirTemplates, newRultemplates);
                }

                if (originalRulTemplates != null) {
                    for (RulTemplate rulTemplate : originalRulTemplates) {
                        File dirFile = new File(dirTemplates + File.separator + rulTemplate.getDirectory());
                        if (dirFile.exists()) {
                            rollBackFiles(dirFile);
                        }
                    }
                }

                rollBackFiles(dirActions);
                rollBackFiles(dirRules);
                bulkActionConfigManager.load();
            } catch (IOException e1) {
                throw new IllegalStateException(e);
            }
            throw new IllegalStateException(e);
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

    private void cleanBackupTemplates(final File dirTemplates, final List<RulTemplate> originalRulTemplates) {
        for (RulTemplate rulTemplate : originalRulTemplates) {
            File dirFile = new File(dirTemplates + File.separator + rulTemplate.getDirectory());
            if (dirFile.exists()) {
                cleanBackupFiles(dirFile);
            }
        }
    }

    /**
     * Zpracování implicitních sloupců pro pravidla.
     * @param rulRuleSets pravidla
     * @param ruleSets xml pravidla, ze který se budou pravidla aktualizovat
     * @param rulDescItemTypes aktuální seznam atributů
     */
    private void processDefaultItemTypes(final List<RulRuleSet> rulRuleSets, final RuleSets ruleSets, final List<RulItemType> rulDescItemTypes) {
        if (rulRuleSets.isEmpty() || ruleSets.getRuleSets() == null || ruleSets.getRuleSets().isEmpty()) {
            // Nejsou žádná pravidla pro synchronizaci
            return;
        }

        // Mapa kódu na xml pravidlo
        final Map<String, RuleSet> xmlRuleSetMap = Maps.uniqueIndex(ruleSets.getRuleSets(), RuleSet::getCode);

        // Mapa kódu na atribut
        final Map<String, RulItemType> rulDescItemTypeMap = Maps.uniqueIndex(rulDescItemTypes, RulItemType::getCode);

        // Synchronizace
        rulRuleSets.forEach(rulRuleSet -> {
            final RuleSet xmlRuleSet = xmlRuleSetMap.get(rulRuleSet.getCode());

            // Smazání původních vazeb
            List<RulDefaultItemType> currItems = defaultItemTypeRepository.findByRuleSet(rulRuleSet);
            defaultItemTypeRepository.delete(currItems);

            // Import nových vazeb
            if (xmlRuleSet.getDefaultItemTypes() != null) {
                xmlRuleSet.getDefaultItemTypes().getDefaultItemTypes().forEach(defaultItemType -> {
                    RulItemType rulDescItem = rulDescItemTypeMap.get(defaultItemType.getCode());
                    if (rulDescItem == null) {
                        throw new IllegalStateException("Pravidlo s kódem " + rulRuleSet.getCode()
                                + " obsahuje odkaz na neexistující atribut jednotky popisu (atribut s kódem "
                                + defaultItemType.getCode() + " neexistuje).");
                    }

                    RulDefaultItemType rel = new RulDefaultItemType();
                    rel.setItemType(rulDescItem);
                    rel.setRuleSet(rulRuleSet);
                    defaultItemTypeRepository.save(rel);
                });
            }
        });
    }

    /**
     * Zpracování policy.
     *
     * @param policyTypes VO policy
     * @param rulPackage  balíček
     * @param rulRuleSets seznam pravidel
     */
    private void processPolicyTypes(final PolicyTypes policyTypes,
                                    final RulPackage rulPackage,
                                    final List<RulRuleSet> rulRuleSets) {
        List<RulPolicyType> rulPolicyTypesTypes = policyTypeRepository.findByRulPackage(rulPackage);
        List<RulPolicyType> rulPolicyTypesNew = new ArrayList<>();

        if (!CollectionUtils.isEmpty(policyTypes.getPolicyTypes())) {
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

                convertRulPolicyTypes(rulPackage, policyType, item, rulRuleSets);
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
     * @param rulRuleSets   seznam pravidel
     */
    private void convertRulPolicyTypes(final RulPackage rulPackage,
                                       final PolicyType policyType,
                                       final RulPolicyType rulPolicyType,
                                       final List<RulRuleSet> rulRuleSets) {
        rulPolicyType.setCode(policyType.getCode());
        rulPolicyType.setName(policyType.getName());
        rulPolicyType.setRulPackage(rulPackage);

        List<RulRuleSet> findItems = rulRuleSets.stream()
                .filter((r) -> r.getCode().equals(policyType.getRuleSet()))
                .collect(Collectors.toList());

        RulRuleSet item;

        if (findItems.size() > 0) {
            item = findItems.get(0);
        } else {
            throw new IllegalStateException("Kód " + policyType.getRuleSet() + " neexistuje v RulRuleSet");
        }

        rulPolicyType.setRuleSet(item);
    }

    /**
     * Zpracování packet.
     *
     * @param packetTypes importovaný seznam packets
     * @param rulPackage  balíček
     */
    private void processPacketTypes(final PacketTypes packetTypes, final RulPackage rulPackage) {
        List<RulPacketType> rulPacketTypes = packetTypeRepository.findByRulPackage(rulPackage);
        List<RulPacketType> rulPacketTypesNew = new ArrayList<>();

        if (!CollectionUtils.isEmpty(packetTypes.getPacketTypes())) {
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

                convertRulPacketTypes(rulPackage, packetType, item);
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
     *
     * @param rulPackage    balíček
     * @param packetType    VO packet
     * @param rulPacketType DAO packet
     */
    private void convertRulPacketTypes(final RulPackage rulPackage,
                                       final PacketType packetType,
                                       final RulPacketType rulPacketType) {
        rulPacketType.setPackage(rulPackage);
        rulPacketType.setCode(packetType.getCode());
        rulPacketType.setName(packetType.getName());
        rulPacketType.setShortcut(packetType.getShortcut());
    }

    /**
     * Převod VO na DAO packet.
     *
     * @param rulPackage    balíček
     * @param outputType    VO packet
     * @param rulOutputType DAO packet
     * @param rulRuleList   seznam souborů s pravidly
     */
    private void convertRulOutputType(final RulPackage rulPackage,
                                      final OutputType outputType,
                                      final RulOutputType rulOutputType,
                                      final List<RulRule> rulRuleList) {
        rulOutputType.setPackage(rulPackage);
        rulOutputType.setCode(outputType.getCode());
        rulOutputType.setName(outputType.getName());

        String filename = outputType.getFilename();
        if (filename != null) {
            RulRule rule = rulRuleList.stream()
                    .filter(r -> r.getFilename().equals(filename))
                    .findFirst().orElse(null);

            if (rule == null) {
                throw new IllegalStateException("Soubor '" + filename + "' neexistuje v RulRule");
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
     * @param rulRuleSets    seznam pravidel
     * @param dir            adresář pravidel
     * @return seznam pravidel
     */
    private List<RulRule> processPackageRules(final PackageRules packageRules,
                                              final RulPackage rulPackage,
                                              final Map<String, ByteArrayInputStream> mapEntry,
                                              final List<RulRuleSet> rulRuleSets,
                                              final File dir) {

        List<RulRule> rulPackageRules = packageRulesRepository.findByRulPackage(rulPackage);
        List<RulRule> rulRuleNew = new ArrayList<>();

        if (!CollectionUtils.isEmpty(packageRules.getPackageRules())) {
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

                convertRulPackageRule(rulPackage, packageRule, item, rulRuleSets);
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
                saveFile(mapEntry, dir, ZIP_DIR_RULES, rule.getFilename());
            }

            bulkActionConfigManager.load();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return rulRuleNew;

    }

    /**
     * Převod VO na DAO pravidla.
     *  @param rulPackage     balíček
     * @param packageRule    VO pravidla
     * @param rulPackageRule DAO pravidla
     * @param rulRuleSets    seznam pravidel
     */
    private void convertRulPackageRule(final RulPackage rulPackage,
                                       final PackageRule packageRule,
                                       final RulRule rulPackageRule,
                                       final List<RulRuleSet> rulRuleSets) {

        rulPackageRule.setPackage(rulPackage);
        rulPackageRule.setFilename(packageRule.getFilename());
        rulPackageRule.setPriority(packageRule.getPriority());
        rulPackageRule.setRuleType(packageRule.getRuleType());

        String ruleSetCode = packageRule.getRuleSet();
        if (ruleSetCode != null) {
            RulRuleSet ruleSet = rulRuleSets.stream()
                    .filter(rs -> rs.getCode().equals(ruleSetCode))
                    .findFirst().orElse(null);

            if (ruleSet == null) {
                throw new IllegalStateException("Kód '" + ruleSetCode + "' neexistuje v RulRuleSet");
            }
            rulPackageRule.setRuleSet(ruleSet);
        } else {
            rulPackageRule.setRuleSet(null);
        }

    }

    /**
     * Zpracování hromadných akcí.
     *
     * @param packageActions   importovaných seznam hromadných akcí
     * @param rulPackage       balíček
     * @param mapEntry         mapa streamů souborů v ZIP
     * @param dir              adresář hromadných akcí  @return seznam hromadných akcí
     */
    private List<RulAction> processPackageActions(final PackageActions packageActions,
                                                  final RulPackage rulPackage,
                                                  final Map<String, ByteArrayInputStream> mapEntry,
                                                  final File dir) {

        List<RulAction> rulPackageActions = packageActionsRepository.findByRulPackage(rulPackage);
        List<RulAction> rulPackageActionsNew = new ArrayList<>();

        if (!CollectionUtils.isEmpty(packageActions.getPackageActions())) {
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
                convertRulPackageAction(rulPackage, packageAction, item);
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
                            throw new IllegalArgumentException("Neexistující typ atributu: " + actionItemType.getItemType());
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
                            throw new IllegalArgumentException("Neexistující typ outputu: " + actionRecommended.getOutputType());
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
                saveFile(mapEntry, dir, ZIP_DIR_ACTIONS, action.getFilename());
            }

            bulkActionConfigManager.load();
        } catch (IOException e) {
            throw new IllegalStateException(e);
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

        for (File file : files) {
            file.delete();
        }
    }

    /**
     * Provedení obnovy při selhání importu.
     *
     * @param dir adresář
     */
    private void rollBackFiles(final File dir) throws IOException {

        File[] files = dir.listFiles((dir1, name) -> name.endsWith(".bck"));

        for (File file : files) {
            File fileMove = new File(StringUtils.stripEnd(file.getPath(), ".bck"));
            if (fileMove.exists()) {
                fileMove.delete();
            }
            Files.move(file.toPath(), fileMove.toPath());
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
     *
     * @param rulPackage       balíček
     * @param packageAction    VO hromadné akce
     * @param rulPackageAction DAO hromadné akce
     */
    private void convertRulPackageAction(final RulPackage rulPackage,
                                         final PackageAction packageAction,
                                         final RulAction rulPackageAction) {
        rulPackageAction.setPackage(rulPackage);
        rulPackageAction.setFilename(packageAction.getFilename());
    }

    /**
     * Zpracování typů atributů.
     *
     * @param itemTypes       seznam importovaných typů
     * @param itemSpecs       seznam importovaných specifikací
     * @param rulPackage      balíček
     * @return                výsledný seznam atributů v db
     */
    private List<RulItemType> processItemTypes(final ItemTypes itemTypes,
                                               final ItemSpecs itemSpecs,
                                               final RulPackage rulPackage) {
    	List<RulDataType> rulDataTypes = dataTypeRepository.findAll();
    	
    	ItemTypeUpdater updater = AppContext.getBean(ItemTypeUpdater.class);
    	
    	return updater.update(rulDataTypes, rulPackage, itemTypes, itemSpecs);
    }

    /**
     * Zpracování typů atributů.
     *
     * @param outputTypes  seznam importovaných typů
     * @param templates    seznam importovaných specifikací
     * @param rulPackage   balíček
     * @param dirTemplates
     * @param rulRuleList  seznam souborů s pravidly
     * @return výsledný seznam atributů v db
     */
    private List<RulOutputType> processOutputTypes(final OutputTypes outputTypes,
                                                   final Templates templates,
                                                   final RulPackage rulPackage,
                                                   final Map<String, ByteArrayInputStream> mapEntry,
                                                   final File dirTemplates,
                                                   final List<RulRule> rulRuleList) {

        List<RulOutputType> rulOutputTypes = outputTypeRepository.findByRulPackage(rulPackage);
        List<RulOutputType> rulOutputTypesNew = new ArrayList<>();

        if (!CollectionUtils.isEmpty(outputTypes.getOutputTypes())) {
            for (OutputType outputType : outputTypes.getOutputTypes()) {
                List<RulOutputType> findItems = rulOutputTypes.stream()
                        .filter((r) -> r.getCode().equals(outputType.getCode())).collect(Collectors.toList());
                RulOutputType item;
                if (findItems.size() > 0) {
                    item = findItems.get(0);
                } else {
                    item = new RulOutputType();
                }

                convertRulOutputType(rulPackage, outputType, item, rulRuleList);
                rulOutputTypesNew.add(item);
            }
        }

        rulOutputTypesNew = outputTypeRepository.save(rulOutputTypesNew);

        newRultemplates = processTemplates(templates, rulPackage, rulOutputTypesNew, mapEntry, dirTemplates);

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
     *  @param templates       seznam importovaných specifikací
     * @param rulPackage          balíček
     * @param rulOutputTypes    seznam typů atributů
     * @param dirTemplates
     */
    private List<RulTemplate> processTemplates(
            final Templates templates,
            final RulPackage rulPackage,
            final List<RulOutputType> rulOutputTypes,
            final Map<String, ByteArrayInputStream> mapEntry,
            final File dirTemplates) {
        List<RulTemplate> rulTemplate = templateRepository.findByRulPackage(rulPackage);
        List<RulTemplate> rulTemplateNew = new ArrayList<>();
        List<RulTemplate> rulTemplateActual = new ArrayList<>();

        if (!CollectionUtils.isEmpty(templates.getTemplates())) {
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

            importTemplatesFiles(mapEntry, dirTemplates, rulTemplateNew);

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

    private void importTemplatesFiles(final Map<String, ByteArrayInputStream> mapEntry, final File dirTemplates, final List<RulTemplate> rulTemplateActual) throws IOException {
        for (RulTemplate template : rulTemplateActual) {
            final String templateDir = ZIP_DIR_TEMPLATES + "/" + template.getDirectory();
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
        rulTemplate.setEngine(cz.tacr.elza.api.RulTemplate.Engine.valueOf(template.getEngine()));
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
     * @param ruleSets         seznam importovaných pravidel
     * @param rulPackage       balíček
     * @return seznam pravidel
     */
    private List<RulRuleSet> processRuleSets(final RuleSets ruleSets,
                                             final RulPackage rulPackage) {

        List<RulRuleSet> rulRuleSets = ruleSetRepository.findByRulPackage(rulPackage);
        List<RulRuleSet> rulRuleSetsNew = new ArrayList<>();

        if (!CollectionUtils.isEmpty(ruleSets.getRuleSets())) {
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

        // Smazání pravidel, které již nejsou v xml
        List<RulRuleSet> rulRuleSetsDelete = new ArrayList<>(rulRuleSets);
        rulRuleSetsDelete.removeAll(rulRuleSetsNew);
        rulRuleSetsDelete.forEach(this::deleteRuleSet);

        return rulRuleSetsNew;
    }

    /**
     * Smazání pravidla.
     * @param rulRuleSet pravidlo
     */
    private void deleteRuleSet(final RulRuleSet rulRuleSet) {
        // Smazání návazných záznamů
        defaultItemTypeRepository.deleteByRuleSet(rulRuleSet);

        // Smazání instance
        ruleSetRepository.delete(rulRuleSet);
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
            if (rulPackage.getVersion().equals(packageInfo.getVersion())) {
                throw new IllegalStateException(
                        "Balíček " + rulPackage.getCode() + " ve verzi " + rulPackage.getVersion()
                                + " byl již aplikován");
            }
        }

        rulPackage.setCode(packageInfo.getCode());
        rulPackage.setName(packageInfo.getName());
        rulPackage.setDescription(packageInfo.getDescription());
        rulPackage.setVersion(packageInfo.getVersion());

        return packageRepository.save(rulPackage);
    }

    /**
     * Převod streamu na XML soubor.
     *
     * @param classObject objekt XML
     * @param xmlStream   xml stream
     * @param <T>         typ pro převod
     */
    private <T> T convertXmlStreamToObject(final Class classObject, final ByteArrayInputStream xmlStream) {
        Assert.notNull(xmlStream, "Soubor pro třídu " + classObject.toString() + " neexistuje");
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(classObject);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            T xml = (T) unmarshaller.unmarshal(xmlStream);
            return xml;
        } catch (Exception e) {
            throw new IllegalStateException("Chyba při mapování XML souboru pro třídu " + classObject.toString(), e);
        }
    }

    /**
     * Vytviření mapy streamů souborů v zipu.
     *
     * @param zipFile soubor zip
     * @param entries záznamy
     * @return mapa streamů
     */
    private Map<String, ByteArrayInputStream> createStreamsMap(final ZipFile zipFile,
                                                               final Enumeration<? extends ZipEntry> entries)
            throws IOException {
        Map<String, ByteArrayInputStream> mapEntry = new HashMap<>();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            InputStream stream = zipFile.getInputStream(entry);

            ByteArrayOutputStream fout = new ByteArrayOutputStream();

            for (int c = stream.read(); c != -1; c = stream.read()) {
                fout.write(c);
            }
            stream.close();

            mapEntry.put(entry.getName(), new ByteArrayInputStream(fout.toByteArray()));
            fout.close();
        }
        return mapEntry;
    }

    /**
     * Smazání importovaného balíčku podle kódu.
     *
     * @param code kód balíčku
     */
    public void deletePackage(final String code) {
        RulPackage rulPackage = packageRepository.findTopByCode(code);

        if (rulPackage == null) {
            throw new IllegalArgumentException("Balíček s kódem " + code + " neexistuje");
        }

        List<RulItemSpec> rulDescItemSpecs = itemSpecRepository.findByRulPackage(rulPackage);
        for (RulItemSpec rulDescItemSpec : rulDescItemSpecs) {
            itemSpecRegisterRepository.deleteByItemSpec(rulDescItemSpec);
        }
        itemSpecRepository.delete(rulDescItemSpecs);

        File dirActions = new File(bulkActionConfigManager.getPath());
        File dirRules = new File(rulesExecutor.getRootPath());


        try {


            for (RulRule rulPackageRule : packageRulesRepository.findByRulPackage(rulPackage)) {
                deleteFile(dirRules, rulPackageRule.getFilename());
            }

            for (RulAction rulPackageAction : packageActionsRepository.findByRulPackage(rulPackage)) {
                deleteFile(dirActions, rulPackageAction.getFilename());
            }

            packageActionsRepository.findByRulPackage(rulPackage).forEach(this::deleteActionLink);

            itemTypeRepository.deleteByRulPackage(rulPackage);

            packageActionsRepository.deleteByRulPackage(rulPackage);

            packageRulesRepository.deleteByRulPackage(rulPackage);

            policyTypeRepository.deleteByRulPackage(rulPackage);

            ruleSetRepository.findByRulPackage(rulPackage).forEach(this::deleteRuleSet);

            packetTypeRepository.deleteByRulPackage(rulPackage);

            templateRepository.deleteByRulPackage(rulPackage);

            outputTypeRepository.deleteByRulPackage(rulPackage);

            packageRepository.delete(rulPackage);

            entityManager.flush();

            cleanBackupFiles(dirActions);
            cleanBackupFiles(dirRules);

            bulkActionConfigManager.load();

            eventNotificationService.publishEvent(new ActionEvent(EventType.PACKAGE));

        } catch (Exception e) {
            try {
                rollBackFiles(dirActions);
                rollBackFiles(dirRules);

                bulkActionConfigManager.load();
                throw new IllegalStateException(e);
            } catch (IOException e1) {
                throw new IllegalStateException(e);
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
     * Provede export balíčku s konfigurací.
     *
     * @param code kód balíčku
     * @return výsledný soubor
     */
    public File exportPackage(final String code) {
        RulPackage rulPackage = packageRepository.findTopByCode(code);

        if (rulPackage == null) {
            throw new IllegalArgumentException("Balíček s kódem " + code + " neexistuje");
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

            file.deleteOnExit();
        } catch (IOException e) {

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

    private void exportOutputTypes(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        OutputTypes outputTypes = new OutputTypes();
        List<RulOutputType> rulRuleSets = outputTypeRepository.findByRulPackage(rulPackage);
        List<OutputType> ruleSetList = new ArrayList<>(rulRuleSets.size());
        outputTypes.setOutputTypes(ruleSetList);

        for (RulOutputType rulOutputType : rulRuleSets) {
            OutputType outputType = new OutputType();
            convertOutputType(rulOutputType, outputType);
            ruleSetList.add(outputType);
        }

        addObjectToZipFile(outputTypes, zos, OUTPUT_TYPE_XML);
    }

    private void exportTemplates(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        Templates outputTypes = new Templates();
        List<RulTemplate> rulRuleSets = templateRepository.findByRulPackageAndNotDeleted(rulPackage);
        List<Template> ruleSetList = new ArrayList<>(rulRuleSets.size());
        outputTypes.setTemplates(ruleSetList);

        for (RulTemplate rulOutputType : rulRuleSets) {
            Template outputType = new Template();
            convertTemplate(rulOutputType, outputType);
            ruleSetList.add(outputType);
            File dir = new File(outputGeneratorService.getTemplatesDir() + File.separator + rulOutputType.getDirectory() + File.separator);
            for (File dirFile : dir.listFiles()) {
                addToZipFile(ZIP_DIR_TEMPLATES + "/" + rulOutputType.getDirectory(), dirFile, zos);
            }
        }

        addObjectToZipFile(outputTypes, zos, TEMPLATE_XML);
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

        addObjectToZipFile(packageInfo, zos, PACKAGE_XML);
    }

    /**
     * Exportování packet typů.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportPacketTypes(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        PacketTypes packetTypes = new PacketTypes();
        List<RulPacketType> rulPacketTypes = packetTypeRepository.findByRulPackage(rulPackage);
        List<PacketType> packetTypeList = new ArrayList<>(rulPacketTypes.size());
        packetTypes.setPacketTypes(packetTypeList);

        for (RulPacketType rulPacketType : rulPacketTypes) {
            PacketType packetType = new PacketType();
            covertPacketType(rulPacketType, packetType);
            packetTypeList.add(packetType);
        }

        addObjectToZipFile(packetTypes, zos, PACKET_TYPE_XML);
    }

    /**
     * Exportování pravidel.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportPackageRules(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        PackageRules packageRules = new PackageRules();
        List<RulRule> rulPackageRules = packageRulesRepository.findByRulPackage(rulPackage);
        List<PackageRule> packageRuleList = new ArrayList<>(rulPackageRules.size());
        packageRules.setPackageRules(packageRuleList);

        for (RulRule rulPackageRule : rulPackageRules) {
            PackageRule packageRule = new PackageRule();
            convertPackageRule(rulPackageRule, packageRule);
            packageRuleList.add(packageRule);

            addToZipFile(ZIP_DIR_RULES + "/" + rulPackageRule.getFilename(), new File(rulesExecutor.getRootPath()
                    + File.separator + rulPackageRule.getFilename()), zos);

        }

        addObjectToZipFile(packageRules, zos, PACKAGE_RULES_XML);
    }

    /**
     * Exportování hromadných akcí.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportPackageActions(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        PackageActions packageActions = new PackageActions();
        List<RulAction> rulPackageActions = packageActionsRepository.findByRulPackage(rulPackage);
        List<PackageAction> packageActionList = new ArrayList<>(rulPackageActions.size());
        packageActions.setPackageActions(packageActionList);

        for (RulAction rulPackageAction : rulPackageActions) {
            PackageAction packageAction = new PackageAction();
            convertPackageAction(rulPackageAction, packageAction);
            packageActionList.add(packageAction);

            addToZipFile(ZIP_DIR_ACTIONS + "/" + rulPackageAction.getFilename(),
                    new File(bulkActionConfigManager.getPath() + File.separator + rulPackageAction.getFilename()), zos);
        }

        addObjectToZipFile(packageActions, zos, PACKAGE_ACTIONS_XML);
    }

    /**
     * Exportování typů atributů.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportDescItemTypes(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        ItemTypes itemTypes = new ItemTypes();
        List<RulItemType> rulDescItemTypes = itemTypeRepository.findByRulPackageOrderByViewOrderAsc(rulPackage);
        List<ItemType> itemTypeList = new ArrayList<>(rulDescItemTypes.size());
        itemTypes.setItemTypes(itemTypeList);

        for (RulItemType rulDescItemType : rulDescItemTypes) {
            ItemType itemType = new ItemType();
            convertDescItemType(rulDescItemType, itemType);
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
    private void exportDescItemSpecs(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        ItemSpecs itemSpecs = new ItemSpecs();
        List<RulItemSpec> rulDescItemSpecs = itemSpecRepository.findByRulPackage(rulPackage);
        List<ItemSpec> itemSpecList = new ArrayList<>(rulDescItemSpecs.size());
        itemSpecs.setItemSpecs(itemSpecList);

        for (RulItemSpec rulDescItemSpec : rulDescItemSpecs) {
            ItemSpec itemSpec = new ItemSpec();
            convertDescItemSpec(rulDescItemSpec, itemSpec);
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
        itemSpec.setViewOrder(rulDescItemSpec.getViewOrder());
        itemSpec.setDescription(rulDescItemSpec.getDescription());
        itemSpec.setItemType(rulDescItemSpec.getItemType().getCode());
        itemSpec.setShortcut(rulDescItemSpec.getShortcut());

        List<RulItemSpecRegister> rulItemSpecRegisters = itemSpecRegisterRepository
                .findByDescItemSpecId(rulDescItemSpec);

        List<DescItemSpecRegister> descItemSpecRegisterList = new ArrayList<>(rulItemSpecRegisters.size());

        for (RulItemSpecRegister rulItemSpecRegister : rulItemSpecRegisters) {
            DescItemSpecRegister descItemSpecRegister = new DescItemSpecRegister();
            convertDescItemSpecRegister(rulItemSpecRegister, descItemSpecRegister);
            descItemSpecRegisterList.add(descItemSpecRegister);
        }

        itemSpec.setDescItemSpecRegisters(descItemSpecRegisterList);
    }

    /**
     * Převod DAO na VO napojení specifikací na reg.
     *
     * @param rulItemSpecRegister DAO napojení specifikací
     * @param descItemSpecRegister    VO napojení specifikací
     */
    private void convertDescItemSpecRegister(final RulItemSpecRegister rulItemSpecRegister,
                                             final DescItemSpecRegister descItemSpecRegister) {
        descItemSpecRegister.setRegisterType(rulItemSpecRegister.getRegisterType().getCode());
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
        packageRule.setRuleSet(rulPackageRule.getRuleSet() == null ? null : rulPackageRule.getRuleSet().getCode());
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
}
