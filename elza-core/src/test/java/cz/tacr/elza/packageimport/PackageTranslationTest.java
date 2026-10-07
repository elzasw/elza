package cz.tacr.elza.packageimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import cz.tacr.elza.ElzaCoreMain;
import cz.tacr.elza.controller.ApController;
import cz.tacr.elza.controller.LanguagesController;
import cz.tacr.elza.controller.RuleController;
import cz.tacr.elza.controller.RulesController;
import cz.tacr.elza.controller.vo.ItemType;
import cz.tacr.elza.controller.vo.ItemTypeSpec;
import cz.tacr.elza.controller.vo.Language;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.core.data.PackageTexts;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.SysLanguage;
import cz.tacr.elza.domain.TranslationEntityType;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.other.HelperTestService;
import cz.tacr.elza.packageimport.PackageTranslationService.IssueKind;
import cz.tacr.elza.packageimport.PackageTranslationService.TranslationIssue;
import cz.tacr.elza.packageimport.xml.Translation;
import cz.tacr.elza.packageimport.xml.Translations;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.repository.RulTranslationRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.StartupService;
import cz.tacr.elza.service.UserService;
import jakarta.servlet.http.Cookie;

/**
 * Translations of package-provided texts, in the two ways they reach an installation.
 *
 * <p>SIMPLE-DEV translates its own item types and specifications ({@code translations/en.xml}).
 * The test package {@code translation-addon-test} (code {@code TRANSLATION_TEST}) depends on it
 * and contributes translations of another package - the case of a translation package such as
 * {@code CZ_BASE_EN}: it overrides one SIMPLE-DEV translation, adds one SIMPLE-DEV lacks,
 * translates an item type that does not exist, and defines a message with its English
 * translation. Invalid translation files are variants of that package built in the test.
 *
 * <p>Packages are imported once for the class and {@code TRANSLATION_TEST} is removed afterwards;
 * context configuration matches {@code AbstractTest} so the cached Spring context is reused.
 */
@ContextConfiguration(classes = ElzaCoreMain.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PackageTranslationTest {

    private static final String BASE_CODE = "SIMPLE-DEV";
    private static final String BASE_DIR = "rules-simple-dev";
    private static final String ADDON_CODE = "TRANSLATION_TEST";
    private static final String ADDON_DIR = "translation-addon-test";
    private static final String GREETING = ADDON_CODE + "/GREETING";

    @Autowired
    private HelperTestService helperTestService;
    @Autowired
    private StartupService startupService;
    @Autowired
    private StaticDataService staticDataService;
    @Autowired
    private PackageService packageService;
    @Autowired
    private PackageTranslationService packageTranslationService;
    @Autowired
    private PackageTexts packageTexts;
    @Autowired
    private PackageRepository packageRepository;
    @Autowired
    private RulTranslationRepository translationRepository;
    @Autowired
    private ItemTypeRepository itemTypeRepository;
    @Autowired
    private RulesController rulesController;
    @Autowired
    private LanguagesController languagesController;
    @Autowired
    private RuleController ruleController;
    @Autowired
    private ApController apController;
    @Autowired
    private UserService userService;
    @Autowired
    @Qualifier("transactionManager")
    private PlatformTransactionManager txManager;
    @Value("${local.server.port}")
    private int port;

    @BeforeAll
    void loadPackagesOnce() {
        helperTestService.deleteTables(false);
        startupService.startNow();
        authorizeAsAdmin();
        helperTestService.loadPackage("CZ_BASE", "package-cz-base");
        helperTestService.loadPackage(BASE_CODE, BASE_DIR);
        helperTestService.loadPackage(ADDON_CODE, ADDON_DIR);
    }

    @AfterAll
    void unloadPackages() {
        if (packageRepository.findByCode(ADDON_CODE) != null) {
            packageService.deletePackage(ADDON_CODE);
        }
        staticDataService.refreshForCurrentThread();
        startupService.stop();
    }

    @Test
    @Order(1)
    void packagesStoreTheirOwnRows() {
        tx(() -> {
            RulPackage base = packageRepository.findByCode(BASE_CODE);
            RulPackage addon = packageRepository.findByCode(ADDON_CODE);
            assertEquals("cs", base.getLanguage().getTag());
            assertEquals(13, translationRepository.findByRulPackage(base).size());
            // both message rows, three English item-type rows, one Czech override of SIMPLE-DEV
            assertEquals(6,translationRepository.findByRulPackage(addon).size());
        });
    }

    @Test
    @Order(2)
    void dependentPackageOverridesAndCompletesTranslations() {
        SysLanguage en = language("en");
        assertEquals("Content summary", text(TranslationEntityType.ITEM_TYPE, "SRD_TITLE", "name", en));
        assertEquals("Content", text(TranslationEntityType.ITEM_TYPE, "SRD_TITLE", "shortcut", en));
        assertEquals("Arrangement method", text(TranslationEntityType.ITEM_TYPE, "SRD_ARRANGEMENT_TYPE", "name", en));
        assertEquals("Reference code (sequence number)", text(TranslationEntityType.ITEM_TYPE, "SRD_UNIT_ID", "name", en));
        // no translation: source text
        assertEquals("source", text(TranslationEntityType.ITEM_TYPE, "SRD_UNIT_DATE", "name", en));
        // no language: source text
        assertEquals("source", text(TranslationEntityType.ITEM_TYPE, "SRD_TITLE", "name", null));
        // a dependent package overrides a SIMPLE-DEV text in its source language
        assertEquals("Datace (doplněk)", text(TranslationEntityType.ITEM_TYPE, "SRD_UNIT_DATE", "name", language("cs")));
    }

    @Test
    @Order(3)
    void requestLanguageFallsBackToLanguageWithoutRegion() {
        assertEquals("en", packageTexts.resolveRequestLanguage("en-GB").getTag());
        assertEquals("en", packageTexts.resolveRequestLanguage("de-DE,en-GB;q=0.8,cs;q=0.5").getTag());
        assertEquals("cs", packageTexts.resolveRequestLanguage("cs-CZ,cs;q=0.9").getTag());
        // German is known but not a UI language
        assertNull(packageTexts.resolveRequestLanguage("de"));
        assertNull(packageTexts.resolveRequestLanguage(null));
        assertNull(packageTexts.resolveRequestLanguage("not a header"));
    }

    @Test
    @Order(4)
    void messagesAreFormattedInTheRequestedOrSourceLanguage() {
        assertEquals("Hello, Jana. You have 3 messages.", packageTexts.message(GREETING, language("en"), "Jana", 3));
        assertEquals("Dobrý den, Jana. Máte 3 zprávy.", packageTexts.message(GREETING, language("cs"), "Jana", 3));
        assertEquals("Dobrý den, Jana. Máte 3 zprávy.", packageTexts.message(GREETING, null, "Jana", 3));
        // no German text: source language of the package
        assertEquals("Dobrý den, Jana. Máte 3 zprávy.", packageTexts.message(GREETING, language("de"), "Jana", 3));
        assertEquals(ADDON_CODE + "/MISSING", packageTexts.message(ADDON_CODE + "/MISSING", language("en")));
    }

    @Test
    @Order(5)
    void itemTypesEndpointsUseTheRequestLanguage() {
        try {
            bindRequest(null, "en-GB,en;q=0.9");
            ItemType titleEn = itemType(rulesController.rulesListItemTypes(null, null).getBody().getItemTypes(),
                                        "SRD_TITLE");
            assertEquals("Content summary", titleEn.getName());
            assertEquals("Content", titleEn.getShortcut());

            // the cookie of the client wins over the browser header
            bindRequest("en", "cs");
            ItemType levelEn = itemType(rulesController.rulesListItemTypes(null, null).getBody().getItemTypes(),
                                        "SRD_LEVEL_TYPE");
            assertEquals("Level of description", levelEn.getName());
            assertEquals("ZP4.2.7 Level of description", levelEn.getDescription());
            ItemTypeSpec series = levelEn.getSpecs().stream()
                    .filter(s -> s.getCode().equals("SRD_LEVEL_SERIES")).findFirst().orElseThrow();
            assertEquals("Series", series.getName());

            // legacy endpoint of the node form
            RulDescItemTypeExtVO levelVo = txGet(() -> ruleController.getDescItemTypes()).stream()
                    .filter(t -> t.getCode().equals("SRD_LEVEL_TYPE")).findFirst().orElseThrow();
            assertEquals("Level of description", levelVo.getName());
            assertEquals("Series", levelVo.getDescItemSpecs().stream()
                    .filter(s -> s.getCode().equals("SRD_LEVEL_SERIES")).findFirst().orElseThrow().getName());

            // no cookie and no header: the language of elza.locale
            bindRequest(null, null);
            ItemType titleCs = itemType(rulesController.rulesListItemTypes(null, null).getBody().getItemTypes(),
                                        "SRD_TITLE");
            assertEquals("Obsah, regest", titleCs.getName());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    @Order(6)
    void languagesEndpointListsAllLanguagesWithUsages() {
        List<Language> languages = languagesController.languagesListLanguages().getBody();
        assertEquals(11, languages.size());
        Set<String> uiLanguages = languages.stream().filter(Language::getUiEnabled).map(Language::getTag)
                .collect(Collectors.toSet());
        assertEquals(Set.of("cs", "en"), uiLanguages);
        assertTrue(languages.stream().allMatch(Language::getScopeEnabled));
        List<Language> defaults = languages.stream().filter(Language::getDefaultLanguage).toList();
        assertEquals(1, defaults.size());
        assertEquals("cs", defaults.get(0).getTag());

        // the scope language list offers the scope languages - all of them
        assertEquals(11, apController.getAllLanguages().size());
    }

    @Test
    @Order(7)
    void orphanIsKeptAndReported() {
        tx(() -> {
            List<TranslationIssue> issues = packageTranslationService
                    .checkTranslations(packageRepository.findByCode(ADDON_CODE));
            assertEquals(List.of(new TranslationIssue(IssueKind.ORPHAN, "ITEM_TYPE", "SRD_NOT_EXISTING", "name", "en")),
                         issues);
            assertTrue(packageTranslationService.checkTranslations(packageRepository.findByCode(BASE_CODE)).isEmpty());
        });
        assertEquals("Ghost", packageTexts.text(TranslationEntityType.ITEM_TYPE, "SRD_NOT_EXISTING", "name", null,
                                                language("en")));
    }

    @Test
    @Order(8)
    void changedSourceTextMarksTranslationOutdated() {
        String original = txGet(() -> itemTypeRepository.findOneByCode("SRD_LEVEL_TYPE").getName());
        try {
            tx(() -> {
                RulItemType levelType = itemTypeRepository.findOneByCode("SRD_LEVEL_TYPE");
                levelType.setName("Úroveň popisu (změněno)");
                itemTypeRepository.save(levelType);
            });
            tx(() -> {
                List<TranslationIssue> issues = packageTranslationService
                        .checkTranslations(packageRepository.findByCode(BASE_CODE));
                assertEquals(List.of(new TranslationIssue(IssueKind.OUTDATED, "ITEM_TYPE", "SRD_LEVEL_TYPE", "name", "en")),
                             issues);
            });
            // an outdated translation is still used
            assertEquals("Level of description", text(TranslationEntityType.ITEM_TYPE, "SRD_LEVEL_TYPE", "name",
                                                      language("en")));
        } finally {
            tx(() -> {
                RulItemType levelType = itemTypeRepository.findOneByCode("SRD_LEVEL_TYPE");
                levelType.setName(original);
                itemTypeRepository.save(levelType);
            });
        }
    }

    @Test
    @Order(9)
    void exportWritesTheTranslationFilesBack() throws IOException, URISyntaxException {
        Path zip = txGet(() -> {
            try {
                return packageService.exportPackage(ADDON_CODE);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            for (String file : List.of("translations/cs.xml", "translations/en.xml")) {
                ZipEntry entry = zipFile.getEntry(file);
                assertNotNull(entry, "missing in export: " + file);
                Translations exported;
                try (var is = zipFile.getInputStream(entry)) {
                    exported = PackageUtils.convertXmlStreamToObject(Translations.class,
                                                                     new ByteArrayInputStream(is.readAllBytes()));
                }
                Translations source = PackageUtils.convertXmlStreamToObject(Translations.class,
                        new ByteArrayInputStream(Files.readAllBytes(resourceDir(ADDON_DIR).resolve(file))));
                assertEquals(source.getLang(), exported.getLang());
                assertEquals(rows(source), rows(exported), file);
                // a translation of an existing text carries the hash of its source; the own
                // messages (source texts themselves) and the orphan have none
                for (Translation t : exported.getTranslations()) {
                    boolean noSource = t.getCode().equals("SRD_NOT_EXISTING")
                            || (t.getType().equals("MESSAGE") && exported.getLang().equals("cs"));
                    assertEquals(noSource, t.getSrcHash() == null, t.getCode() + " in " + file);
                }
            }
            String packageXml = new String(zipFile.getInputStream(zipFile.getEntry(PackageContext.PACKAGE_XML))
                    .readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(packageXml.contains("<language>cs</language>"), packageXml);
        } finally {
            Files.deleteIfExists(zip);
        }
    }

    @Test
    @Order(10)
    void invalidTranslationFilesRefuseTheImport() throws Exception {
        String en = "translations/en.xml";
        String cs = "translations/cs.xml";
        assertRefused(PackageCode.INVALID_TRANSLATION, Map.of(en, file("en", "<t type=\"FOO\" code=\"SRD_TITLE\" field=\"name\">x</t>")));
        assertRefused(PackageCode.INVALID_TRANSLATION, Map.of(en, file("en", "<t type=\"ITEM_TYPE\" code=\"SRD_TITLE\" field=\"text\">x</t>")));
        assertRefused(PackageCode.INVALID_TRANSLATION, Map.of(en, file("en",
                "<t type=\"ITEM_TYPE\" code=\"SRD_TITLE\" field=\"name\">x</t>",
                "<t type=\"ITEM_TYPE\" code=\"SRD_TITLE\" field=\"name\">y</t>")));
        assertRefused(PackageCode.INVALID_TRANSLATION, Map.of(cs, file("cs", "<t type=\"MESSAGE\" code=\"GREETING\" field=\"text\">x</t>")));
        // missing field, empty text, broken message pattern, too long code
        assertRefused(PackageCode.INVALID_TRANSLATION, Map.of(en, file("en", "<t type=\"ITEM_TYPE\" code=\"SRD_TITLE\">x</t>")));
        assertRefused(PackageCode.INVALID_TRANSLATION, Map.of(en, file("en", "<t type=\"ITEM_TYPE\" code=\"SRD_TITLE\" field=\"name\"/>")));
        assertRefused(PackageCode.INVALID_TRANSLATION, Map.of(en, file("en", "<t type=\"MESSAGE\" code=\"" + GREETING + "\" field=\"text\">Hello {0</t>")));
        assertRefused(PackageCode.INVALID_TRANSLATION, Map.of(en, file("en", "<t type=\"ITEM_TYPE\" code=\"" + "X".repeat(101) + "\" field=\"name\">x</t>")));
        // a second file of the same language
        assertRefused(PackageCode.INVALID_TRANSLATION, Map.of("translations/EN.xml", file("EN", "<t type=\"ITEM_TYPE\" code=\"SRD_TITLE\" field=\"name\">x</t>")));
        assertRefused(PackageCode.INVALID_TRANSLATION, Map.of(en, file("de", "<t type=\"ITEM_TYPE\" code=\"SRD_TITLE\" field=\"name\">x</t>")));
        assertRefused(PackageCode.CODE_NOT_FOUND, Map.of("translations/xx.xml", file("xx", "<t type=\"ITEM_TYPE\" code=\"SRD_TITLE\" field=\"name\">x</t>")));

        // the installed package is untouched
        tx(() -> assertEquals(6, translationRepository.findByRulPackage(packageRepository.findByCode(ADDON_CODE)).size()));
        assertEquals("Content summary", text(TranslationEntityType.ITEM_TYPE, "SRD_TITLE", "name", language("en")));
    }

    @Test
    @Order(11)
    void reimportOfTheTranslatedPackageKeepsAllTranslations() {
        reimport(() -> helperTestService.loadPackage(BASE_CODE, BASE_DIR));
        SysLanguage en = language("en");
        assertEquals("Content summary", text(TranslationEntityType.ITEM_TYPE, "SRD_TITLE", "name", en));
        assertEquals("Level of description", text(TranslationEntityType.ITEM_TYPE, "SRD_LEVEL_TYPE", "name", en));
        tx(() -> assertEquals(6, translationRepository.findByRulPackage(packageRepository.findByCode(ADDON_CODE)).size()));
    }

    /**
     * A new version of SIMPLE-DEV changes two source texts and keeps its English file: the
     * translations of both packages become outdated and stay so after a re-import of the
     * translating package (the hash is kept while the translation is unchanged). Restoring the
     * texts makes them current again. The own Czech row in the variant is skipped.
     */
    @Test
    @Order(12)
    void changedSourceTextInANewVersionMarksTranslationsOutdated() throws Exception {
        String itemTypes = Files.readString(resourceDir(BASE_DIR).resolve("rul_item_type.xml"), StandardCharsets.UTF_8);
        assertTrue(itemTypes.contains("<name>Úroveň popisu</name>") && itemTypes.contains("<name>Obsah, regest</name>"));
        Map<String, String> variant = new HashMap<>();
        variant.put("rul_item_type.xml", itemTypes
                .replace("<name>Úroveň popisu</name>", "<name>Úroveň popisu (nová)</name>")
                .replace("<name>Obsah, regest</name>", "<name>Obsah a regest</name>"));
        variant.put("translations/cs.xml", file("cs", "<t type=\"ITEM_TYPE\" code=\"SRD_TITLE\" field=\"name\">Vlastní</t>"));
        File zip = buildVariant(BASE_DIR, variant);
        try {
            reimport(() -> importZip(zip));
        } finally {
            Files.deleteIfExists(zip.toPath());
        }
        tx(() -> {
            RulPackage base = packageRepository.findByCode(BASE_CODE);
            assertEquals(13, translationRepository.findByRulPackage(base).size());
            assertEquals(Set.of("SRD_LEVEL_TYPE", "SRD_TITLE"), outdated(base));
            assertEquals(Set.of("SRD_TITLE"), outdated(packageRepository.findByCode(ADDON_CODE)));
        });

        reimport(() -> helperTestService.loadPackage(ADDON_CODE, ADDON_DIR));
        tx(() -> assertEquals(Set.of("SRD_TITLE"), outdated(packageRepository.findByCode(ADDON_CODE))));

        reimport(() -> helperTestService.loadPackage(BASE_CODE, BASE_DIR));
        tx(() -> {
            assertEquals(Set.of(), outdated(packageRepository.findByCode(BASE_CODE)));
            assertEquals(Set.of(), outdated(packageRepository.findByCode(ADDON_CODE)));
        });
    }

    /** Tags are case-insensitive: {@code EN.xml} with {@code lang="EN"} is the English file. */
    @Test
    @Order(13)
    void languageTagsAreCaseInsensitive() throws Exception {
        String en = Files.readString(resourceDir(ADDON_DIR).resolve("translations/en.xml"), StandardCharsets.UTF_8)
                .replace("lang=\"en\"", "lang=\"EN\"");
        Map<String, String> variant = new HashMap<>();
        variant.put("translations/en.xml", null);
        variant.put("translations/EN.xml", en);
        File zip = buildVariant(ADDON_DIR, variant);
        try {
            reimport(() -> importZip(zip));
            assertEquals("Content summary", text(TranslationEntityType.ITEM_TYPE, "SRD_TITLE", "name", language("en")));
        } finally {
            Files.deleteIfExists(zip.toPath());
            reimport(() -> helperTestService.loadPackage(ADDON_CODE, ADDON_DIR));
        }
    }

    /** The language list is public: the login page needs it before the user signs in. */
    @Test
    @Order(14)
    void languagesEndpointIsPublic() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/languages"))
                        .header("Accept", "application/json").GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("\"tag\":\"en\""), response.body());
    }

    @Test
    @Order(20)
    void deletingTheDependentPackageRemovesOnlyItsRows() {
        packageService.deletePackage(ADDON_CODE);
        staticDataService.refreshForCurrentThread();

        SysLanguage en = language("en");
        assertEquals("Content, abstract", text(TranslationEntityType.ITEM_TYPE, "SRD_TITLE", "name", en));
        assertEquals("source", text(TranslationEntityType.ITEM_TYPE, "SRD_ARRANGEMENT_TYPE", "name", en));
        assertEquals(GREETING, packageTexts.message(GREETING, en));
        tx(() -> assertEquals(13, translationRepository.findByRulPackage(packageRepository.findByCode(BASE_CODE)).size()));
    }

    /** Binds a request with the language cookie and the {@code Accept-Language} header (both optional). */
    private static void bindRequest(String cookie, String acceptLanguage) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (cookie != null) {
            request.setCookies(new Cookie(PackageTexts.LANGUAGE_COOKIE, cookie));
        }
        if (acceptLanguage != null) {
            request.addHeader("Accept-Language", acceptLanguage);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private String text(TranslationEntityType type, String code, String field, SysLanguage language) {
        return packageTexts.text(type, code, field, "source", language);
    }

    private SysLanguage language(String tag) {
        return staticDataService.getData().getSysLanguageByTag(tag);
    }

    private static ItemType itemType(List<ItemType> itemTypes, String code) {
        return itemTypes.stream().filter(t -> t.getCode().equals(code)).findFirst().orElseThrow();
    }

    private static Set<String> rows(Translations translations) {
        Set<String> rows = new HashSet<>();
        for (Translation t : translations.getTranslations()) {
            rows.add(t.getType() + "|" + t.getCode() + "|" + t.getField() + "|" + t.getValue());
        }
        return rows;
    }

    private static String file(String lang, String... rows) {
        return "<translations lang=\"" + lang + "\">" + String.join("", rows) + "</translations>";
    }

    /** Codes of the entities whose translations by the package are outdated. */
    private Set<String> outdated(RulPackage rulPackage) {
        return packageTranslationService.checkTranslations(rulPackage).stream()
                .filter(i -> i.kind() == IssueKind.OUTDATED)
                .map(TranslationIssue::entityCode)
                .collect(Collectors.toSet());
    }

    private void importZip(File zip) {
        packageService.preImportPackage();
        packageService.importPackageInternal(zip, true);
    }

    private void assertRefused(PackageCode expected, Map<String, String> replacedFiles) throws Exception {
        File zip = buildVariant(ADDON_DIR, replacedFiles);
        try {
            AbstractException e = assertThrows(AbstractException.class, () -> reimport(() -> importZip(zip)));
            assertEquals(expected, e.getErrorCode(), e.getMessage());
        } finally {
            Files.deleteIfExists(zip.toPath());
        }
    }

    /** A test package with some files replaced (null removes the file), as a ZIP. */
    private static File buildVariant(String packageDir, Map<String, String> replacedFiles)
            throws IOException, URISyntaxException {
        Path dir = resourceDir(packageDir);
        Map<String, byte[]> files = new HashMap<>();
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path p : paths.filter(Files::isRegularFile).toList()) {
                files.put(dir.relativize(p).toString().replace('\\', '/'), Files.readAllBytes(p));
            }
        }
        replacedFiles.forEach((name, content) -> {
            if (content == null) {
                files.remove(name);
            } else {
                files.put(name, content.getBytes(StandardCharsets.UTF_8));
            }
        });

        File zip = File.createTempFile("translation-variant_", ".zip");
        try (OutputStream os = Files.newOutputStream(zip.toPath()); ZipOutputStream zos = new ZipOutputStream(os)) {
            for (Map.Entry<String, byte[]> f : files.entrySet()) {
                zos.putNextEntry(new ZipEntry(f.getKey()));
                zos.write(f.getValue());
                zos.closeEntry();
            }
        }
        return zip;
    }

    private static Path resourceDir(String dir) throws URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(dir);
        assertNotNull(url, dir);
        return Path.of(url.toURI());
    }

    /** Runs an import of an already installed package version. */
    private void reimport(Runnable importer) {
        Boolean testing = packageService.getTesting();
        packageService.setTesting(true);
        try {
            importer.run();
        } finally {
            packageService.setTesting(testing);
            staticDataService.refreshForCurrentThread();
        }
    }

    private void tx(Runnable body) {
        new TransactionTemplate(txManager).executeWithoutResult(status -> body.run());
    }

    private <T> T txGet(Supplier<T> body) {
        return new TransactionTemplate(txManager).execute(status -> body.get());
    }

    private void authorizeAsAdmin() {
        UserDetail userDetail = userService.createUserDetail((Integer) null);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("", "", null);
        auth.setDetails(userDetail);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
