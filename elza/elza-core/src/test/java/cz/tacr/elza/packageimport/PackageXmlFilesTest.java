package cz.tacr.elza.packageimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.packageimport.xml.APTypes;
import cz.tacr.elza.packageimport.xml.ActionsXml;
import cz.tacr.elza.packageimport.xml.ArrangementExtensions;
import cz.tacr.elza.packageimport.xml.ArrangementRules;
import cz.tacr.elza.packageimport.xml.EntityRules;
import cz.tacr.elza.packageimport.xml.ExportFiltersXml;
import cz.tacr.elza.packageimport.xml.ExtensionRules;
import cz.tacr.elza.packageimport.xml.ExternalIdTypes;
import cz.tacr.elza.packageimport.xml.InstitutionTypes;
import cz.tacr.elza.packageimport.xml.IssueStates;
import cz.tacr.elza.packageimport.xml.IssueTypes;
import cz.tacr.elza.packageimport.xml.ItemSpec;
import cz.tacr.elza.packageimport.xml.ItemSpecs;
import cz.tacr.elza.packageimport.xml.ItemTypes;
import cz.tacr.elza.packageimport.xml.OutputFiltersXml;
import cz.tacr.elza.packageimport.xml.OutputTypes;
import cz.tacr.elza.packageimport.xml.PackageInfo;
import cz.tacr.elza.packageimport.xml.PartTypes;
import cz.tacr.elza.packageimport.xml.PolicyTypes;
import cz.tacr.elza.packageimport.xml.RuleSetApTypes;
import cz.tacr.elza.packageimport.xml.RuleSetPartTypes;
import cz.tacr.elza.packageimport.xml.RuleSets;
import cz.tacr.elza.packageimport.xml.Settings;
import cz.tacr.elza.packageimport.xml.StructureDefinitions;
import cz.tacr.elza.packageimport.xml.StructureExtensionDefinitions;
import cz.tacr.elza.packageimport.xml.StructureExtensions;
import cz.tacr.elza.packageimport.xml.StructureTypes;
import cz.tacr.elza.packageimport.xml.TaskTypes;
import cz.tacr.elza.packageimport.xml.Templates;
import cz.tacr.elza.packageimport.xml.Translations;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

/**
 * Package files are read strictly: an element the binding does not know is an error, not a silently
 * lost value. Every XML file of the packages delivered with ELZA and of the test packages is read here with the class the
 * import uses for it.
 */
class PackageXmlFilesTest {

    /** delivered packages (their src directories) and the packages of the tests */
    private static final List<Path> PACKAGES = List.of(Paths.get("..", "package-cz-base", "src"),
            Paths.get("..", "package-isaar-cpf", "src"), Paths.get("..", "rules-cz-zp2015", "src"),
            Paths.get("..", "rules-simple-dev", "src"), Paths.get("src", "test", "resources", "entity-rules-test"),
            Paths.get("src", "test", "resources", "entity-standalone-test"),
            Paths.get("src", "test", "resources", "rules-addon-test"),
            Paths.get("src", "test", "resources", "translation-addon-test"));

    /** files in the root of a package */
    private static final Map<String, Class<?>> PACKAGE_FILES = Map.ofEntries(
            Map.entry(PackageContext.PACKAGE_XML, PackageInfo.class),
            Map.entry(APTypeUpdater.AP_TYPE_XML, APTypes.class),
            Map.entry(PackageService.RULE_SET_XML, RuleSets.class),
            Map.entry(PackageService.PART_TYPE_XML, PartTypes.class),
            Map.entry(PackageService.ITEM_TYPE_XML, ItemTypes.class),
            Map.entry(PackageService.ITEM_SPEC_XML, ItemSpecs.class),
            Map.entry(PackageService.STRUCTURE_TYPE_XML, StructureTypes.class),
            Map.entry(PackageService.STRUCTURE_DEFINITION_XML, StructureDefinitions.class),
            Map.entry(PackageService.STRUCTURE_EXTENSION_XML, StructureExtensions.class),
            Map.entry(PackageService.STRUCTURE_EXTENSION_DEFINITION_XML, StructureExtensionDefinitions.class),
            Map.entry(PackageService.SETTING_XML, Settings.class),
            Map.entry(PackageService.EXTERNAL_ID_TYPE_XML, ExternalIdTypes.class),
            Map.entry(PackageService.ISSUE_TYPE_XML, IssueTypes.class),
            Map.entry(PackageService.ISSUE_STATE_XML, IssueStates.class),
            Map.entry(PackageService.TASK_TYPE_XML, TaskTypes.class),
            Map.entry(PackageService.INSTITUTION_TYPE_XML, InstitutionTypes.class),
            Map.entry(PackageService.PACKAGE_ACTIONS_XML, ActionsXml.class),
            Map.entry(PackageService.PACKAGE_OUTPUT_FILTERS_XML, OutputFiltersXml.class),
            Map.entry(PackageService.PACKAGE_EXPORT_FILTERS_XML, ExportFiltersXml.class));

    /** files in the directory of a rule set */
    private static final Map<String, Class<?>> RULE_SET_FILES = Map.ofEntries(
            Map.entry(PackageService.SETTING_XML, Settings.class),
            Map.entry(PackageService.RULE_SET_AP_TYPE_XML, RuleSetApTypes.class),
            Map.entry(PackageService.RULE_SET_PART_TYPE_XML, RuleSetPartTypes.class),
            Map.entry(PackageService.POLICY_TYPE_XML, PolicyTypes.class),
            Map.entry(PackageService.ARRANGEMENT_RULE_XML, ArrangementRules.class),
            Map.entry(PackageService.ARRANGEMENT_EXTENSION_XML, ArrangementExtensions.class),
            Map.entry(PackageService.EXTENSION_RULE_XML, ExtensionRules.class),
            Map.entry(PackageService.ENTITY_RULE_XML, EntityRules.class),
            Map.entry(PackageService.OUTPUT_TYPE_XML, OutputTypes.class),
            Map.entry(TemplateUpdater.TEMPLATE_XML, Templates.class));

    @Test
    void packagesHaveNoUnknownElements() throws IOException {
        List<String> read = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        for (Path src : PACKAGES) {
            assertTrue(Files.isDirectory(src), src.toAbsolutePath().toString());
            for (Map.Entry<String, Class<?>> e : PACKAGE_FILES.entrySet()) {
                check(src.resolve(e.getKey()), e.getValue(), read, failures);
            }
            Path ruleSets = src.resolve("rul_rule_set");
            if (Files.isDirectory(ruleSets)) {
                try (Stream<Path> dirs = Files.list(ruleSets)) {
                    for (Path dir : dirs.filter(Files::isDirectory).toList()) {
                        for (Map.Entry<String, Class<?>> e : RULE_SET_FILES.entrySet()) {
                            check(dir.resolve(e.getKey()), e.getValue(), read, failures);
                        }
                    }
                }
            }
            Path translations = src.resolve("translations");
            if (Files.isDirectory(translations)) {
                try (Stream<Path> files = Files.list(translations)) {
                    for (Path file : files.filter(f -> f.toString().endsWith(".xml")).toList()) {
                        check(file, Translations.class, read, failures);
                    }
                }
            }
        }
        assertTrue(read.size() > 40, "files read: " + read);
        assertEquals(List.of(), failures);
    }

    @Test
    void unknownElementIsAnError() {
        String xml = "<item-specs><item-spec code=\"X\"><name>x</name><description>x</description>"
                + "<shortcut>x</shortcut><category>A</category></item-spec></item-specs>";
        SystemException e = assertThrows(SystemException.class, () -> PackageUtils.convertXmlStreamToObject(
                ItemSpecs.class, new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
        assertTrue(e.getMessage().contains("category"), e.getMessage());

        String wrapped = xml.replace("<category>A</category>", "<categories><category>A</category></categories>");
        ItemSpec spec = PackageUtils.convertXmlStreamToObject(ItemSpecs.class,
                new ByteArrayInputStream(wrapped.getBytes(StandardCharsets.UTF_8))).getItemSpecs().get(0);
        assertEquals("A", spec.getCategories().get(0).getValue());
    }

    private static void check(Path file, Class<?> cls, List<String> read, List<String> failures) throws IOException {
        if (!Files.exists(file)) {
            return;
        }
        read.add(file.toString());
        try {
            assertFalse(PackageUtils.convertXmlFileToObject(cls, file) == null);
        } catch (SystemException e) {
            // the import stops at the first problem; list every one of the file
            Set<String> problems = new TreeSet<>();
            try {
                Unmarshaller unmarshaller = JAXBContext.newInstance(cls).createUnmarshaller();
                unmarshaller.setEventHandler(event -> {
                    String message = event.getMessage();
                    int expected = message.indexOf(". Expected");
                    problems.add(expected > 0 ? message.substring(0, expected) : message);
                    return true;
                });
                unmarshaller.unmarshal(file.toFile());
            } catch (JAXBException je) {
                problems.add(je.toString());
            }
            failures.add(file + ": " + problems);
        }
    }
}
