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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.bulkaction.BulkActionConfigManager;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemConstraint;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemSpecRegister;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRule;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.drools.RulesExecutor;
import cz.tacr.elza.packageimport.xml.ArrangementType;
import cz.tacr.elza.packageimport.xml.ArrangementTypes;
import cz.tacr.elza.packageimport.xml.DescItemConstraint;
import cz.tacr.elza.packageimport.xml.DescItemConstraints;
import cz.tacr.elza.packageimport.xml.DescItemSpec;
import cz.tacr.elza.packageimport.xml.DescItemSpecRegister;
import cz.tacr.elza.packageimport.xml.DescItemSpecs;
import cz.tacr.elza.packageimport.xml.DescItemType;
import cz.tacr.elza.packageimport.xml.DescItemTypes;
import cz.tacr.elza.packageimport.xml.PackageAction;
import cz.tacr.elza.packageimport.xml.PackageActions;
import cz.tacr.elza.packageimport.xml.PackageInfo;
import cz.tacr.elza.packageimport.xml.PackageRule;
import cz.tacr.elza.packageimport.xml.PackageRules;
import cz.tacr.elza.packageimport.xml.PacketType;
import cz.tacr.elza.packageimport.xml.PacketTypes;
import cz.tacr.elza.packageimport.xml.RuleSet;
import cz.tacr.elza.packageimport.xml.RuleSets;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.DescItemConstraintRepository;
import cz.tacr.elza.repository.DescItemSpecRegisterRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.ActionRepository;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.repository.RuleRepository;
import cz.tacr.elza.repository.PacketTypeRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;


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
     * typy v zipu
     */
    public static final String ARRANGEMENT_TYPE_XML = "rul_arrangement_type.xml";

    /**
     * podmínky atributů v zipu
     */
    public static final String DESC_ITEM_CONSTRAINT_XML = "rul_desc_item_constraint.xml";

    /**
     * specifikace atributů v zipu
     */
    public static final String DESC_ITEM_SPEC_XML = "rul_desc_item_spec.xml";

    /**
     * typy atributů v zipu
     */
    public static final String DESC_ITEM_TYPE_XML = "rul_desc_item_type.xml";

    /**
     * hromadné akce v zipu
     */
    public static final String PACKAGE_ACTIONS_XML = "rul_package_actions.xml";

    /**
     * pravidla v zipu
     */
    public static final String PACKAGE_RULES_XML = "rul_package_rules.xml";

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
    private ArrangementTypeRepository arrangementTypeRepository;

    @Autowired
    private DataTypeRepository dataTypeRepository;

    @Autowired
    private DescItemTypeRepository descItemTypeRepository;

    @Autowired
    private DescItemSpecRepository descItemSpecRepository;

    @Autowired
    private DescItemSpecRegisterRepository descItemSpecRegisterRepository;

    @Autowired
    private DescItemConstraintRepository descItemConstraintRepository;

    @Autowired
    private RegisterTypeRepository registerTypeRepository;

    @Autowired
    private ActionRepository packageActionsRepository;

    @Autowired
    private RuleRepository packageRulesRepository;

    @Autowired
    private BulkActionConfigManager bulkActionConfigManager;

    @Autowired
    private PacketTypeRepository packetTypeRepository;

    /**
     * Provede import balíčku.
     *
     * @param file soubor balíčku
     */
    public void importPackage(final File file) {
        File dirActions = new File(bulkActionConfigManager.getPath());
        File dirRules = new File(RulesExecutor.ROOT_PATH);

        ZipFile zipFile = null;
        List<RulAction> rulPackageActions = null;
        List<RulRule> rulPackageRules = null;

        try {

            zipFile = new ZipFile(file);

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            Map<String, ByteArrayInputStream> mapEntry = createStreamsMap(zipFile, entries);

            // načtení info o importu
            PackageInfo packageInfo = convertXmlStreamToObject(PackageInfo.class, mapEntry.get(PACKAGE_XML));

            RulPackage rulPackage = processRulPackage(packageInfo);

            RuleSets ruleSets = convertXmlStreamToObject(RuleSets.class, mapEntry.get(RULE_SET_XML));

            ArrangementTypes arrangementTypes = convertXmlStreamToObject(ArrangementTypes.class,
                    mapEntry.get(ARRANGEMENT_TYPE_XML));
            DescItemConstraints descItemConstraints = convertXmlStreamToObject(DescItemConstraints.class,
                    mapEntry.get(DESC_ITEM_CONSTRAINT_XML));
            DescItemSpecs descItemSpecs = convertXmlStreamToObject(DescItemSpecs.class,
                    mapEntry.get(DESC_ITEM_SPEC_XML));
            DescItemTypes descItemTypes = convertXmlStreamToObject(DescItemTypes.class,
                    mapEntry.get(DESC_ITEM_TYPE_XML));
            PackageActions packageActions = convertXmlStreamToObject(PackageActions.class,
                    mapEntry.get(PACKAGE_ACTIONS_XML));
            PackageRules packageRules = convertXmlStreamToObject(PackageRules.class,
                    mapEntry.get(PACKAGE_RULES_XML));
            PacketTypes packetTypes = convertXmlStreamToObject(PacketTypes.class, mapEntry.get(PACKET_TYPE_XML));

            processPacketTypes(packetTypes, rulPackage);

            List<RulRuleSet> rulRuleSets = processRuleSets(ruleSets, arrangementTypes, rulPackage);
            processDescItemTypes(descItemTypes, descItemSpecs, descItemConstraints, rulPackage);
            rulPackageActions = processPackageActions(packageActions, rulPackage, mapEntry, dirActions);

            rulPackageRules = processPackageRules(packageRules, rulPackage, mapEntry, rulRuleSets,
                    dirRules);

            entityManager.flush();

            cleanBackupFiles(dirActions);
            cleanBackupFiles(dirRules);

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
     * Zpracování pravidel.
     *
     * @param packageRules importovaných seznam pravidel
     * @param rulPackage   balíček
     * @param mapEntry     mapa streamů souborů v ZIP
     * @param rulRuleSets  seznam pravidel
     * @param dir          adresář pravidel
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
     *
     * @param rulPackage     balíček
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

        List<RulRuleSet> findItems = rulRuleSets.stream()
                .filter((r) -> r.getCode().equals(packageRule.getRuleSet()))
                .collect(Collectors.toList());

        RulRuleSet item;

        if (findItems.size() > 0) {
            item = findItems.get(0);
        } else {
            throw new IllegalStateException("Kód " + packageRule.getRuleSet() + " neexistuje v RulRuleSet");
        }

        rulPackageRule.setRuleSet(item);

    }

    /**
     * Zpracování hromadných akcí.
     *
     * @param packageActions importovaných seznam hromadných akcí
     * @param rulPackage     balíček
     * @param mapEntry       mapa streamů souborů v ZIP
     * @param dir            adresář hromadných akcí
     * @return seznam hromadných akcí
     */
    private List<RulAction> processPackageActions(final PackageActions packageActions,
                                                          final RulPackage rulPackage,
                                                          final Map<String, ByteArrayInputStream> mapEntry,
                                                          final File dir) {

        List<RulAction> rulPackageActions = packageActionsRepository.findByRulPackage(rulPackage);
        List<RulAction> rulPackageActionsNew = new ArrayList<>();

        if (!CollectionUtils.isEmpty(packageActions.getPackageActions())) {
            for (PackageAction packageAction : packageActions.getPackageActions()) {
                List<RulAction> findItems = rulPackageActions.stream().filter(
                        (r) -> r.getFilename().equals(packageAction.getFilename())).collect(
                        Collectors.toList());
                RulAction item;
                if (findItems.size() > 0) {
                    item = findItems.get(0);
                } else {
                    item = new RulAction();
                }

                convertRulPackageAction(rulPackage, packageAction, item);
                rulPackageActionsNew.add(item);
            }
        }

        rulPackageActionsNew = packageActionsRepository.save(rulPackageActionsNew);

        List<RulAction> rulPackageActionsDelete = new ArrayList<>(rulPackageActions);
        rulPackageActionsDelete.removeAll(rulPackageActionsNew);
        packageActionsRepository.delete(rulPackageActionsDelete);


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
     * @param descItemTypes       seznam importovaných typů
     * @param descItemSpecs       seznam importovaných specifikací
     * @param descItemConstraints seznam importovaných podmínek
     * @param rulPackage          balíček
     */
    private void processDescItemTypes(final DescItemTypes descItemTypes,
                                      final DescItemSpecs descItemSpecs,
                                      final DescItemConstraints descItemConstraints,
                                      final RulPackage rulPackage) {

        List<RulDataType> rulDataTypes = dataTypeRepository.findAll();

        List<RulDescItemType> rulDescItemTypes = descItemTypeRepository.findByRulPackage(rulPackage);
        List<RulDescItemType> rulDescItemTypesNew = new ArrayList<>();

        if (!CollectionUtils.isEmpty(descItemTypes.getDescItemTypes())) {
            for (DescItemType descItemType : descItemTypes.getDescItemTypes()) {
                List<RulDescItemType> findItems = rulDescItemTypes.stream().filter(
                        (r) -> r.getCode().equals(descItemType.getCode())).collect(
                        Collectors.toList());
                RulDescItemType item;
                if (findItems.size() > 0) {
                    item = findItems.get(0);
                } else {
                    item = new RulDescItemType();
                }

                convertRulDescItemType(rulPackage, descItemType, item, rulDataTypes);
                rulDescItemTypesNew.add(item);
            }
        }

        rulDescItemTypesNew = descItemTypeRepository.save(rulDescItemTypesNew);

        processDescItemSpecs(descItemSpecs, descItemConstraints, rulPackage, rulDescItemTypesNew);

        List<RulDescItemType> rulDescItemTypesDelete = new ArrayList<>(rulDescItemTypes);
        rulDescItemTypesDelete.removeAll(rulDescItemTypesNew);
        descItemTypeRepository.delete(rulDescItemTypesDelete);

    }

    /**
     * Zpracování specifikací atributů.
     *
     * @param descItemSpecs       seznam importovaných specifikací
     * @param descItemConstraints seznam importovaných podmínek
     * @param rulPackage          balíček
     * @param rulDescItemTypes    seznam typů atributů
     */
    private void processDescItemSpecs(final DescItemSpecs descItemSpecs,
                                      final DescItemConstraints descItemConstraints,
                                      final RulPackage rulPackage, final List<RulDescItemType> rulDescItemTypes) {

        List<RulDescItemSpec> rulDescItemSpecs = descItemSpecRepository.findByRulPackage(rulPackage);
        List<RulDescItemSpec> rulDescItemSpecsNew = new ArrayList<>();

        if (!CollectionUtils.isEmpty(descItemSpecs.getDescItemSpecs())) {
            for (DescItemSpec descItemSpec : descItemSpecs.getDescItemSpecs()) {
                List<RulDescItemSpec> findItems = rulDescItemSpecs.stream()
                        .filter((r) -> r.getCode().equals(descItemSpec.getCode())).collect(
                                Collectors.toList());
                RulDescItemSpec item;
                if (findItems.size() > 0) {
                    item = findItems.get(0);
                } else {
                    item = new RulDescItemSpec();
                }

                convertRulDescItemSpec(rulPackage, descItemSpec, item, rulDescItemTypes);
                rulDescItemSpecsNew.add(item);
            }
        }

        rulDescItemSpecsNew = descItemSpecRepository.save(rulDescItemSpecsNew);

        processDescItemSpecsRegister(descItemSpecs, rulDescItemSpecsNew);
        processDescItemConstraints(descItemConstraints, rulPackage, rulDescItemSpecsNew, rulDescItemTypes);

        List<RulDescItemSpec> rulDescItemSpecsDelete = new ArrayList<>(rulDescItemSpecs);
        rulDescItemSpecsDelete.removeAll(rulDescItemSpecsNew);
        for (RulDescItemSpec descItemSpec : rulDescItemSpecsDelete) {
            descItemSpecRegisterRepository.deleteByDescItemSpec(descItemSpec);
        }
        descItemSpecRepository.delete(rulDescItemSpecsDelete);
    }

    /**
     * Zpracování podmínek atributů.
     *
     * @param descItemConstraints seznam importovaných podmínek
     * @param rulPackage          balíček
     * @param rulDescItemSpecs    seznam specifikací atributů
     * @param rulDescItemTypes    seznam typů atributů
     */
    private void processDescItemConstraints(final DescItemConstraints descItemConstraints,
                                            final RulPackage rulPackage,
                                            final List<RulDescItemSpec> rulDescItemSpecs,
                                            final List<RulDescItemType> rulDescItemTypes) {


        List<RulDescItemConstraint> rulDescItemConstraints = descItemConstraintRepository.findByRulPackage(rulPackage);
        List<RulDescItemConstraint> rulDescItemConstraintsNew = new ArrayList<>();

        if (!CollectionUtils.isEmpty(descItemConstraints.getDescItemConstraints())) {
            for (DescItemConstraint descItemConstraint : descItemConstraints.getDescItemConstraints()) {
                List<RulDescItemConstraint> findItems = rulDescItemConstraints.stream()
                        .filter((r) -> r.getCode().equals(descItemConstraint.getCode())).collect(Collectors.toList());
                RulDescItemConstraint item;
                if (findItems.size() > 0) {
                    item = findItems.get(0);
                } else {
                    item = new RulDescItemConstraint();
                }

                convertRulDescItemConstraint(rulPackage, descItemConstraint, item, rulDescItemTypes, rulDescItemSpecs);
                rulDescItemConstraintsNew.add(item);
            }
        }

        rulDescItemConstraintsNew = descItemConstraintRepository.save(rulDescItemConstraintsNew);

        List<RulDescItemConstraint> rulDescItemConstraintsDelete = new ArrayList<>(rulDescItemConstraints);
        rulDescItemConstraintsDelete.removeAll(rulDescItemConstraintsNew);
        descItemConstraintRepository.delete(rulDescItemConstraintsDelete);
    }

    /**
     * Převod VO na DAO podmínky atributu.
     *
     * @param rulPackage            balíček
     * @param descItemConstraint    VO podínky
     * @param rulDescItemConstraint DAO podmínky
     * @param rulDescItemTypes      seznam typů atributů
     * @param rulDescItemSpecs      seznam specifikací atributů
     */
    private void convertRulDescItemConstraint(final RulPackage rulPackage,
                                              final DescItemConstraint descItemConstraint,
                                              final RulDescItemConstraint rulDescItemConstraint,
                                              final List<RulDescItemType> rulDescItemTypes,
                                              final List<RulDescItemSpec> rulDescItemSpecs) {

        rulDescItemConstraint.setCode(descItemConstraint.getCode());
        rulDescItemConstraint.setRegexp(descItemConstraint.getRegexp());
        rulDescItemConstraint.setRepeatable(descItemConstraint.getRepeatable());
        rulDescItemConstraint.setTextLenghtLimit(descItemConstraint.getTextLenghtLimit());
        rulDescItemConstraint.setPackage(rulPackage);

        List<RulDescItemType> findItemsType = rulDescItemTypes.stream()
                .filter((r) -> r.getCode().equals(descItemConstraint.getDescItemType()))
                .collect(Collectors.toList());

        RulDescItemType itemType;

        if (findItemsType.size() > 0) {
            itemType = findItemsType.get(0);
        } else {
            throw new IllegalStateException(
                    "Kód " + descItemConstraint.getDescItemType() + " neexistuje v RulDescItemType");
        }

        rulDescItemConstraint.setDescItemType(itemType);

        List<RulDescItemSpec> findItemsSpec = rulDescItemSpecs.stream()
                .filter((r) -> r.getCode().equals(descItemConstraint.getDescItemSpec()))
                .collect(Collectors.toList());

        RulDescItemSpec itemSpec = null;

        if (findItemsSpec.size() > 0) {
            itemSpec = findItemsSpec.get(0);
        } else if (descItemConstraint.getDescItemSpec() != null) {
            throw new IllegalStateException(
                    "Kód " + descItemConstraint.getDescItemSpec() + " neexistuje v RulDescItemSpec");
        }

        rulDescItemConstraint.setDescItemSpec(itemSpec);

    }

    /**
     * Zpracování napojení specifikací na reg.
     *
     * @param descItemSpecs    seznam importovaných specifikací
     * @param rulDescItemSpecs seznam specifikací atributů
     */
    private void processDescItemSpecsRegister(final DescItemSpecs descItemSpecs,
                                              final List<RulDescItemSpec> rulDescItemSpecs) {

        List<RegRegisterType> regRegisterTypes = registerTypeRepository.findAll();

        for (RulDescItemSpec rulDescItemSpec : rulDescItemSpecs) {
            List<DescItemSpec> findItemsSpec = descItemSpecs.getDescItemSpecs().stream().filter(
                    (r) -> r.getCode().equals(rulDescItemSpec.getCode())).collect(Collectors.toList());
            DescItemSpec item;
            if (findItemsSpec.size() > 0) {
                item = findItemsSpec.get(0);
            } else {
                throw new IllegalStateException("Kód " + rulDescItemSpec.getCode() + " neexistuje v DescItemSpecs");
            }

            List<RulDescItemSpecRegister> rulDescItemSpecRegisters = descItemSpecRegisterRepository
                    .findByDescItemSpecId(rulDescItemSpec);
            List<RulDescItemSpecRegister> rulDescItemSpecRegistersNew = new ArrayList<>();

            if (!CollectionUtils.isEmpty(item.getDescItemSpecRegisters())) {
                for (DescItemSpecRegister descItemSpecRegister : item.getDescItemSpecRegisters()) {
                    List<RulDescItemSpecRegister> findItems = rulDescItemSpecRegisters.stream()
                            .filter((r) -> r.getRegisterType().getCode().equals(
                                    descItemSpecRegister.getRegisterType())).collect(Collectors.toList());
                    RulDescItemSpecRegister itemRegister;
                    if (findItems.size() > 0) {
                        itemRegister = findItems.get(0);
                    } else {
                        itemRegister = new RulDescItemSpecRegister();
                    }

                    convertRulDescItemSpecsRegister(rulDescItemSpec, itemRegister, regRegisterTypes,
                            descItemSpecRegister);

                    rulDescItemSpecRegistersNew.add(itemRegister);
                }
            }

            rulDescItemSpecRegistersNew = descItemSpecRegisterRepository.save(rulDescItemSpecRegistersNew);

            List<RulDescItemSpecRegister> rulDescItemSpecRegistersDelete = new ArrayList<>(rulDescItemSpecRegisters);
            rulDescItemSpecRegistersDelete.removeAll(rulDescItemSpecRegistersNew);
            descItemSpecRegisterRepository.delete(rulDescItemSpecRegistersDelete);

        }

    }

    /**
     * Převod VO na DAO napojení specifikací na reg.
     *
     * @param rulDescItemSpec         seznam specifikací
     * @param rulDescItemSpecRegister seznam DAO napojení
     * @param regRegisterTypes        seznam typů reg.
     * @param descItemSpecRegister    seznam VO napojení
     */
    private void convertRulDescItemSpecsRegister(final RulDescItemSpec rulDescItemSpec,
                                                 final RulDescItemSpecRegister rulDescItemSpecRegister,
                                                 final List<RegRegisterType> regRegisterTypes,
                                                 final DescItemSpecRegister descItemSpecRegister) {

        rulDescItemSpecRegister.setDescItemSpec(rulDescItemSpec);

        List<RegRegisterType> findItems = regRegisterTypes.stream()
                .filter((r) -> r.getCode().equals(descItemSpecRegister.getRegisterType()))
                .collect(Collectors.toList());

        RegRegisterType item;

        if (findItems.size() > 0) {
            item = findItems.get(0);
        } else {
            throw new IllegalStateException(
                    "Kód " + descItemSpecRegister.getRegisterType() + " neexistuje v RegRegisterType");
        }

        rulDescItemSpecRegister.setRegisterType(item);

    }

    /**
     * Převod VO na DAO specifikace atributu.
     *
     * @param rulPackage       balíček
     * @param descItemSpec     VO specifikace
     * @param rulDescItemSpec  DAO specifikace
     * @param rulDescItemTypes seznam typů atributů
     */
    private void convertRulDescItemSpec(final RulPackage rulPackage,
                                        final DescItemSpec descItemSpec,
                                        final RulDescItemSpec rulDescItemSpec,
                                        final List<RulDescItemType> rulDescItemTypes) {

        rulDescItemSpec.setName(descItemSpec.getName());
        rulDescItemSpec.setCode(descItemSpec.getCode());
        rulDescItemSpec.setViewOrder(descItemSpec.getViewOrder());
        rulDescItemSpec.setDescription(descItemSpec.getDescription());
        rulDescItemSpec.setShortcut(descItemSpec.getShortcut());
        rulDescItemSpec.setPackage(rulPackage);

        List<RulDescItemType> findItems = rulDescItemTypes.stream()
                .filter((r) -> r.getCode().equals(descItemSpec.getDescItemType()))
                .collect(Collectors.toList());

        RulDescItemType item;

        if (findItems.size() > 0) {
            item = findItems.get(0);
        } else {
            throw new IllegalStateException("Kód " + descItemSpec.getDescItemType() + " neexistuje v RulDescItemType");
        }

        rulDescItemSpec.setDescItemType(item);
    }

    /**
     * Převod VO na DAO typu atributu.
     *
     * @param rulPackage      balíček
     * @param descItemType    VO typu
     * @param rulDescItemType DAO typy
     * @param rulDataTypes    datové typy atributů
     */
    private void convertRulDescItemType(final RulPackage rulPackage,
                                        final DescItemType descItemType,
                                        final RulDescItemType rulDescItemType,
                                        final List<RulDataType> rulDataTypes) {

        rulDescItemType.setCode(descItemType.getCode());
        rulDescItemType.setName(descItemType.getName());

        List<RulDataType> findItems = rulDataTypes.stream()
                .filter((r) -> r.getCode().equals(descItemType.getDataType()))
                .collect(Collectors.toList());

        RulDataType item;

        if (findItems.size() > 0) {
            item = findItems.get(0);
        } else {
            throw new IllegalStateException("Kód " + descItemType.getDataType() + " neexistuje v RulDataType");
        }

        rulDescItemType.setDataType(item);
        rulDescItemType.setShortcut(descItemType.getShortcut());
        rulDescItemType.setDescription(descItemType.getDescription());
        rulDescItemType.setIsValueUnique(descItemType.getIsValueUnique());
        rulDescItemType.setCanBeOrdered(descItemType.getCanBeOrdered());
        rulDescItemType.setUseSpecification(descItemType.getUseSpecification());
        rulDescItemType.setViewOrder(descItemType.getViewOrder());
        rulDescItemType.setFaOnly(descItemType.getFaOnly());

        rulDescItemType.setPackage(rulPackage);
    }


    /**
     * Zpracování pravidel.
     *
     * @param ruleSets         seznam importovaných pravidel
     * @param arrangementTypes seznam typů pořádání
     * @param rulPackage       balíček
     * @return seznam pravidel
     */
    private List<RulRuleSet> processRuleSets(final RuleSets ruleSets,
                                             final ArrangementTypes arrangementTypes,
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

        rulRuleSetsNew = ruleSetRepository.save(rulRuleSetsNew);


        processArrangementTypes(arrangementTypes, rulPackage, rulRuleSetsNew);


        List<RulRuleSet> rulRuleSetsDelete = new ArrayList<>(rulRuleSets);
        rulRuleSetsDelete.removeAll(rulRuleSetsNew);
        ruleSetRepository.delete(rulRuleSetsDelete);

        return rulRuleSetsNew;

    }

    /**
     * Zpracování typu pořádání.
     *
     * @param arrangementTypes seznam importovaných typů pořádání
     * @param rulPackage       balíček
     * @param rulRuleSets      seznam pravidel
     */
    private void processArrangementTypes(final ArrangementTypes arrangementTypes,
                                         final RulPackage rulPackage,
                                         final List<RulRuleSet> rulRuleSets) {

        List<RulArrangementType> rulArrangementTypes = arrangementTypeRepository.findByRulPackage(rulPackage);

        List<RulArrangementType> rulArrangementTypesNew = new ArrayList<>();

        if (!CollectionUtils.isEmpty(arrangementTypes.getArrangementTypes())) {
            for (ArrangementType arrangementType : arrangementTypes.getArrangementTypes()) {
                List<RulArrangementType> findItems = rulArrangementTypes.stream()
                        .filter((r) -> r.getCode().equals(arrangementType.getCode())).collect(
                                Collectors.toList());
                RulArrangementType item;
                if (findItems.size() > 0) {
                    item = findItems.get(0);
                } else {
                    item = new RulArrangementType();
                }

                convertRulArrangementType(rulPackage, arrangementType, item, rulRuleSets);
                rulArrangementTypesNew.add(item);
            }
        }

        arrangementTypeRepository.save(rulArrangementTypesNew);

        List<RulArrangementType> rulArrangementTypesDelete = new ArrayList<>(rulArrangementTypes);
        rulArrangementTypesDelete.removeAll(rulArrangementTypesNew);
        arrangementTypeRepository.delete(rulArrangementTypesDelete);
    }

    /**
     * Převod VO na DAO typu pořádání.
     *
     * @param rulPackage         balíček
     * @param arrangementType    VO typu pořádání
     * @param rulArrangementType DAO typu pořádání
     * @param rulRuleSets        seznam pravidel
     */
    private void convertRulArrangementType(final RulPackage rulPackage,
                                           final ArrangementType arrangementType,
                                           final RulArrangementType rulArrangementType,
                                           final List<RulRuleSet> rulRuleSets) {
        rulArrangementType.setCode(arrangementType.getCode());
        rulArrangementType.setName(arrangementType.getName());
        List<RulRuleSet> findItems = rulRuleSets.stream()
                .filter((r) -> r.getCode().equals(arrangementType.getRuleSet()))
                .collect(Collectors.toList());

        RulRuleSet item;

        if (findItems.size() > 0) {
            item = findItems.get(0);
        } else {
            throw new IllegalStateException("Kód " + arrangementType.getRuleSet() + " neexistuje v RulRuleSet");
        }

        rulArrangementType.setRuleSet(item);
        rulArrangementType.setPackage(rulPackage);
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
    private <T> T convertXmlStreamToObject(Class classObject, ByteArrayInputStream xmlStream) {
        Assert.notNull(xmlStream, "Soubor pro třídu " + classObject.toString() + " neexistuje");
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(classObject);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            T xml = (T) unmarshaller.unmarshal(xmlStream);
            return xml;
        } catch (Exception e) {
            throw new IllegalStateException("Chyba při mapování XML souboru", e);
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

        descItemConstraintRepository.deleteByRulPackage(rulPackage);

        List<RulDescItemSpec> rulDescItemSpecs = descItemSpecRepository.findByRulPackage(rulPackage);
        for (RulDescItemSpec rulDescItemSpec : rulDescItemSpecs) {
            descItemSpecRegisterRepository.deleteByDescItemSpec(rulDescItemSpec);
        }
        descItemSpecRepository.delete(rulDescItemSpecs);

        descItemTypeRepository.deleteByRulPackage(rulPackage);

        arrangementTypeRepository.deleteByRulPackage(rulPackage);

        File dirActions = new File(bulkActionConfigManager.getPath());
        File dirRules = new File(RulesExecutor.ROOT_PATH);

        try {


            for (RulRule rulPackageRule : packageRulesRepository.findByRulPackage(rulPackage)) {
                deleteFile(dirRules, rulPackageRule.getFilename());
            }

            for (RulAction rulPackageAction : packageActionsRepository.findByRulPackage(rulPackage)) {
                deleteFile(dirActions, rulPackageAction.getFilename());
            }

            packageActionsRepository.deleteByRulPackage(rulPackage);

            packageRulesRepository.deleteByRulPackage(rulPackage);

            ruleSetRepository.deleteByRulPackage(rulPackage);

            packetTypeRepository.deleteByRulPackage(rulPackage);

            packageRepository.delete(rulPackage);

            entityManager.flush();

            cleanBackupFiles(dirActions);
            cleanBackupFiles(dirRules);

            bulkActionConfigManager.load();

        } catch (IOException e) {
            try {
                rollBackFiles(dirActions);
                rollBackFiles(dirRules);

                bulkActionConfigManager.load();
            } catch (IOException e1) {
                throw new IllegalStateException(e);
            }
        }

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
            exportArrangementTypes(rulPackage, zos);
            exportDescItemConstraints(rulPackage, zos);
            exportDescItemSpecs(rulPackage, zos);
            exportDescItemTypes(rulPackage, zos);
            exportPackageActions(rulPackage, zos);
            exportPackageRules(rulPackage, zos);
            exportPacketTypes(rulPackage, zos);

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

    /**
     * Exportování typů pořádání.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportArrangementTypes(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        ArrangementTypes arrangementTypes = new ArrangementTypes();
        List<RulArrangementType> rulArrangementTypes = arrangementTypeRepository.findByRulPackage(rulPackage);
        List<ArrangementType> arrangementTypeList = new ArrayList<>(rulArrangementTypes.size());
        arrangementTypes.setArrangementTypes(arrangementTypeList);

        for (RulArrangementType rulArrangementType : rulArrangementTypes) {
            ArrangementType arrangementType = new ArrangementType();
            covertArrangementType(rulArrangementType, arrangementType);
            arrangementTypeList.add(arrangementType);
        }

        addObjectToZipFile(arrangementTypes, zos, ARRANGEMENT_TYPE_XML);
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

            addToZipFile(ZIP_DIR_RULES + "/" + rulPackageRule.getFilename(), new File(RulesExecutor.ROOT_PATH
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
        DescItemTypes descItemTypes = new DescItemTypes();
        List<RulDescItemType> rulDescItemTypes = descItemTypeRepository.findByRulPackage(rulPackage);
        List<DescItemType> descItemTypeList = new ArrayList<>(rulDescItemTypes.size());
        descItemTypes.setDescItemTypes(descItemTypeList);

        for (RulDescItemType rulDescItemType : rulDescItemTypes) {
            DescItemType descItemType = new DescItemType();
            convertDescItemType(rulDescItemType, descItemType);
            descItemTypeList.add(descItemType);
        }

        addObjectToZipFile(descItemTypes, zos, DESC_ITEM_TYPE_XML);
    }

    /**
     * Exportování specifikací atributů.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportDescItemSpecs(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        DescItemSpecs descItemSpecs = new DescItemSpecs();
        List<RulDescItemSpec> rulDescItemSpecs = descItemSpecRepository.findByRulPackage(rulPackage);
        List<DescItemSpec> descItemSpecList = new ArrayList<>(rulDescItemSpecs.size());
        descItemSpecs.setDescItemSpecs(descItemSpecList);

        for (RulDescItemSpec rulDescItemSpec : rulDescItemSpecs) {
            DescItemSpec descItemSpec = new DescItemSpec();
            convertDescItemSpec(rulDescItemSpec, descItemSpec);
            descItemSpecList.add(descItemSpec);
        }

        addObjectToZipFile(descItemSpecs, zos, DESC_ITEM_SPEC_XML);
    }

    /**
     * Exportování podmínek atributů.
     *
     * @param rulPackage balíček
     * @param zos        stream zip souboru
     */
    private void exportDescItemConstraints(final RulPackage rulPackage, final ZipOutputStream zos) throws IOException {
        DescItemConstraints descItemConstraints = new DescItemConstraints();
        List<RulDescItemConstraint> rulDescItemConstraints = descItemConstraintRepository.findByRulPackage(rulPackage);
        List<DescItemConstraint> descItemConstraintList = new ArrayList<>(rulDescItemConstraints.size());
        descItemConstraints.setDescItemConstraints(descItemConstraintList);

        for (RulDescItemConstraint rulDescItemConstraint : rulDescItemConstraints) {
            DescItemConstraint descItemConstraint = new DescItemConstraint();
            convertDescItemConstraint(rulDescItemConstraint, descItemConstraint);
            descItemConstraintList.add(descItemConstraint);
        }

        addObjectToZipFile(descItemConstraints, zos, DESC_ITEM_CONSTRAINT_XML);
    }

    /**
     * Převod DAO na VO podmínky atributu.
     *
     * @param rulDescItemConstraint DAO podmínky
     * @param descItemConstraint    VO podínky
     */
    private void convertDescItemConstraint(final RulDescItemConstraint rulDescItemConstraint,
                                           final DescItemConstraint descItemConstraint) {
        descItemConstraint.setCode(rulDescItemConstraint.getCode());
        descItemConstraint.setRegexp(rulDescItemConstraint.getRegexp());
        descItemConstraint.setRepeatable(rulDescItemConstraint.getRepeatable());
        descItemConstraint.setTextLenghtLimit(rulDescItemConstraint.getTextLenghtLimit());
        descItemConstraint.setDescItemType(rulDescItemConstraint.getDescItemType().getCode());
        descItemConstraint.setDescItemSpec(rulDescItemConstraint.getDescItemSpec() == null ? null :
                                           rulDescItemConstraint.getDescItemSpec().getCode());
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
     * Převod DAO na VO typu pořádání.
     *
     * @param rulArrangementType DAO typu pořádání
     * @param arrangementType    VO typu pořádání
     */
    private void covertArrangementType(final RulArrangementType rulArrangementType,
                                       final ArrangementType arrangementType) {
        arrangementType.setCode(rulArrangementType.getCode());
        arrangementType.setName(rulArrangementType.getName());
        arrangementType.setRuleSet(rulArrangementType.getRuleSet().getCode());
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
     * Převod DAO na VO typů atributu.
     *
     * @param rulDescItemType DAO typy
     * @param descItemType    VO typu
     */
    private void convertDescItemType(final RulDescItemType rulDescItemType, final DescItemType descItemType) {
        descItemType.setCode(rulDescItemType.getCode());
        descItemType.setName(rulDescItemType.getName());
        descItemType.setShortcut(rulDescItemType.getShortcut());
        descItemType.setCanBeOrdered(rulDescItemType.getCanBeOrdered());
        descItemType.setDataType(rulDescItemType.getDataType().getCode());
        descItemType.setDescription(rulDescItemType.getDescription());
        descItemType.setFaOnly(rulDescItemType.getFaOnly());
        descItemType.setIsValueUnique(rulDescItemType.getIsValueUnique());
        descItemType.setUseSpecification(rulDescItemType.getUseSpecification());
        descItemType.setViewOrder(rulDescItemType.getViewOrder());
    }

    /**
     * Převod DAO na VO specifikace.
     *
     * @param rulDescItemSpec DAO specifikace
     * @param descItemSpec    VO specifikace
     */
    private void convertDescItemSpec(final RulDescItemSpec rulDescItemSpec, final DescItemSpec descItemSpec) {
        descItemSpec.setCode(rulDescItemSpec.getCode());
        descItemSpec.setName(rulDescItemSpec.getName());
        descItemSpec.setViewOrder(rulDescItemSpec.getViewOrder());
        descItemSpec.setDescription(rulDescItemSpec.getDescription());
        descItemSpec.setDescItemType(rulDescItemSpec.getDescItemType().getCode());
        descItemSpec.setShortcut(rulDescItemSpec.getShortcut());

        List<RulDescItemSpecRegister> rulDescItemSpecRegisters = descItemSpecRegisterRepository
                .findByDescItemSpecId(rulDescItemSpec);

        List<DescItemSpecRegister> descItemSpecRegisterList = new ArrayList<>(rulDescItemSpecRegisters.size());

        for (RulDescItemSpecRegister rulDescItemSpecRegister : rulDescItemSpecRegisters) {
            DescItemSpecRegister descItemSpecRegister = new DescItemSpecRegister();
            convertDescItemSpecRegister(rulDescItemSpecRegister, descItemSpecRegister);
            descItemSpecRegisterList.add(descItemSpecRegister);
        }

        descItemSpec.setDescItemSpecRegisters(descItemSpecRegisterList);
    }

    /**
     * Převod DAO na VO napojení specifikací na reg.
     *
     * @param rulDescItemSpecRegister DAO napojení specifikací
     * @param descItemSpecRegister    VO napojení specifikací
     */
    private void convertDescItemSpecRegister(final RulDescItemSpecRegister rulDescItemSpecRegister,
                                             final DescItemSpecRegister descItemSpecRegister) {
        descItemSpecRegister.setRegisterType(rulDescItemSpecRegister.getRegisterType().getCode());
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
        packageRule.setRuleSet(rulPackageRule.getRuleSet().getCode());
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
    }

    /**
     * Převod objekt souboru (XML) do XML souboru.
     *
     * @param data objekt souboru (XML)
     * @return převedený dočasný soubor
     */
    private <T> File convertObjectToXmlFile(T data) {
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
    private void addToZipFile(String fileName, File file, ZipOutputStream zos) throws IOException {
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
