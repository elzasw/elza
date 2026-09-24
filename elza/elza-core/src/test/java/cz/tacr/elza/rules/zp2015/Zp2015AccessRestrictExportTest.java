package cz.tacr.elza.rules.zp2015;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.ElzaCoreMain;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.dataexchange.input.DEImportParams;
import cz.tacr.elza.dataexchange.input.DEImportService;
import cz.tacr.elza.dataexchange.output.DEExportParams;
import cz.tacr.elza.dataexchange.output.DEExportParams.FundSections;
import cz.tacr.elza.dataexchange.output.DEExportService;
import cz.tacr.elza.dataexchange.output.writer.xml.XmlExportBuilder;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.other.HelperTestService;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.StartupService;
import cz.tacr.elza.service.UserService;

/**
 * Tests the ZP2015 access restriction export filter ({@code ACCESS_RESTRICT.yaml}) end to end:
 * a fund is imported from XML, exported through the filter and the exported levels are checked.
 *
 * <p>The fund has one folder per restriction type, each referring to a shared restriction whose
 * name says what it restricts. The exported levels are recognized by that name, because node ids
 * change on import and the filter may hide the level's own name.
 *
 * <p>Context configuration and package handling follow {@link Zp2015EjCountTest}.
 */
@ContextConfiguration(classes = ElzaCoreMain.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Zp2015AccessRestrictExportTest {

    private static final String FUND_XML = "zp2015/access-restrict-fund.xml";
    private static final String FUND_NAME = "LC-TEST-omezeni_pristupnosti";
    private static final String EXPORT_FILTER = "ZP_ACCESS_RESTRICT";

    private static final String RESTRICTION = "ZP2015_RESTRICTION_ACCESS_SHARED";
    private static final String RESTRICTION_NAME = "ZP2015_RESTRICTION_ACCESS_NAME";
    private static final String APPLIED = "ZP2015_APPLIED_RESTRICTION";
    private static final String APPLIED_TEXT = "ZP2015_APPLIED_RESTRICTION_TEXT";
    private static final String APPLIED_CHANGE = "ZP2015_APPLIED_RESTRICTION_CHANGE";
    private static final String NAME = "ZP2015_NAME";
    private static final String CONTENT = "ZP2015_CONTENT";

    @Autowired
    private HelperTestService helperTestService;
    @Autowired
    private StaticDataService staticDataService;
    @Autowired
    private StartupService startupService;
    @Autowired
    private PackageService packageService;
    @Autowired
    private DEImportService deImportService;
    @Autowired
    private DEExportService deExportService;
    @Autowired
    private AccessPointService apService;
    @Autowired
    private UserService userService;
    @Autowired
    private FundRepository fundRepository;
    @Autowired
    private FundVersionRepository fundVersionRepository;

    private int fundVersionId;

    @BeforeAll
    void importFund() throws IOException {
        helperTestService.deleteTables(false);
        startupService.startNow();
        helperTestService.loadPackage("CZ_BASE", "package-cz-base");
        helperTestService.loadPackage("ZP2015", "rules-cz-zp2015");

        authorizeAsAdmin();
        importXml("institution-import.xml");
        importXml(FUND_XML);

        ArrFund fund = fundRepository.findAll().stream()
                .filter(f -> FUND_NAME.equals(f.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Fund not imported: " + FUND_NAME));
        ArrFundVersion version = fundVersionRepository.findByFundIdAndLockChangeIsNull(fund.getFundId());
        assertNotNull(version, "Fund has no open version");
        fundVersionId = version.getFundVersionId();
    }

    /**
     * Removes the imported fund and ZP2015 again, see {@link Zp2015EjCountTest#unloadRules()}.
     */
    @AfterAll
    void unloadRules() {
        helperTestService.deleteTables(false);
        packageService.deletePackage("ZP2015");
        staticDataService.refreshForCurrentThread();
        startupService.stop();
    }

    static Stream<Arguments> appliedRestrictions() {
        return Stream.of(
                Arguments.of("nepřístupný DAO",
                        "ZP2015_APPLIED_RESTRICTION_DAO",
                        "Digitální archivní objekt není přístupný k nahlížení."),
                Arguments.of("DAO přístupný jen v badatelně",
                        "ZP2015_APPLIED_RESTRICTION_DAO_INPERS_ONLY",
                        "Digitální archivní objekt je přístupný k nahlížení pouze v prostorách badatelny."),
                Arguments.of("Nepřístupný originál analogové archiválie",
                        "ZP2015_APPLIED_RESTRICTION_ARCHMAT",
                        "Originál archiválie není přístupný k nahlížení. K nahlížení je přístupná pouze kopie archiválie."),
                Arguments.of("Nepřístupná analogová archiválie a její analogová kopie",
                        "ZP2015_APPLIED_RESTRICTION_ARCHMAT2",
                        "Archiválie a její kopie v analogové podobě nejsou přístupné k nahlížení."),
                Arguments.of("Nepřístupné prvky název a obsah",
                        "ZP2015_APPLIED_RESTRICTION_ABSTRACT",
                        "Popis nezveřejněn nebo nahrazen z důvodu omezení přístupnosti."),
                Arguments.of("Použití prvků s omezením",
                        "ZP2015_APPLIED_RESTRICTION_LIMITED",
                        "Uplatněno omezení přístupnosti – zobrazený archivní popis není úplný."));
    }

    /**
     * Each restriction type adds the applied restriction and its text to the restricted level.
     * No restriction has a publish event, so no date of a future change is computed.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("appliedRestrictions")
    void restrictionIsAppliedToLevel(String restriction, String appliedSpec, String appliedText) {
        List<Item> items = export(EXPORT_FILTER).restrictedBy(restriction);

        assertEquals(List.of(appliedSpec), values(items, APPLIED));
        assertEquals(List.of(appliedText), values(items, APPLIED_TEXT));
        assertEquals(List.of(), values(items, APPLIED_CHANGE));
    }

    /**
     * Restrictions of the name and content hide both items; the levels have no public variant
     * which could replace them.
     */
    @Test
    void restrictedDescriptionIsHidden() {
        ExportedFund fund = export(EXPORT_FILTER);

        for (String restriction : List.of("Nepřístupné prvky název a obsah", "Použití prvků s omezením")) {
            List<Item> items = fund.restrictedBy(restriction);
            assertEquals(List.of(), values(items, NAME), restriction);
            assertEquals(List.of(), values(items, CONTENT), restriction);
        }
    }

    /**
     * Restrictions of the digital object or of the analogue material leave the description as is.
     */
    @Test
    void unrestrictedDescriptionIsKept() {
        ExportedFund fund = export(EXPORT_FILTER);

        assertEquals(List.of("nepřístupný DAO"), values(fund.restrictedBy("nepřístupný DAO"), NAME));
        assertEquals(List.of("Nepřístupný originál analogové archiválie"),
                     values(fund.restrictedBy("Nepřístupný originál analogové archiválie"), NAME));
    }

    /**
     * Only the restricted folders are touched; root and series carry no applied restriction.
     */
    @Test
    void onlyRestrictedLevelsAreMarked() {
        ExportedFund fund = export(EXPORT_FILTER);

        long marked = fund.levels.stream().filter(l -> !values(l, APPLIED).isEmpty()).count();
        assertEquals(6, marked);
        assertEquals(8, fund.levels.size());
    }

    /**
     * Without the filter the fund is exported as stored - also a guard that the input carries no
     * applied restriction of its own.
     */
    @Test
    void exportWithoutFilterIsUnchanged() {
        ExportedFund fund = export(null);

        for (List<Item> level : fund.levels) {
            assertEquals(List.of(), values(level, APPLIED));
            assertEquals(List.of(), values(level, APPLIED_TEXT));
        }
        List<Item> limited = fund.restrictedBy("Použití prvků s omezením");
        assertEquals(List.of("Použití prvků s omezením - S OMEZENÍM"), values(limited, NAME));
        assertEquals(List.of("Použití prvků s omezením - S OMEZENÍM"), values(limited, CONTENT));
    }

    /* helpers */

    private void authorizeAsAdmin() {
        UserDetail userDetail = userService.createUserDetail((Integer) null);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("", "", null);
        auth.setDetails(userDetail);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void importXml(String resource) throws IOException {
        ApScope scope = apService.getApScope(1);
        assertNotNull(scope);
        File file = AbstractTest.getResourceFile(resource);
        try (FileInputStream fis = new FileInputStream(file)) {
            deImportService.importData(fis, new DEImportParams(scope.getScopeId(), 1000, 10000, null, null));
        }
    }

    private ExportedFund export(String exportFilter) {
        FundSections sections = new FundSections();
        sections.setFundVersionId(fundVersionId);
        DEExportParams params = new DEExportParams();
        params.addFundsSection(sections);
        params.setExportFilter(exportFilter);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        deExportService.exportXmlData(os, new XmlExportBuilder(), params);
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(os.toByteArray()));
            return new ExportedFund(doc);
        } catch (Exception e) {
            throw new AssertionError("Cannot parse export", e);
        }
    }

    private static List<String> values(List<Item> items, String type) {
        return items.stream().filter(i -> i.type.equals(type)).map(i -> i.value).toList();
    }

    /**
     * Description item as exported: type code and its value - specification code for enums,
     * text for strings, referenced object id for structured items.
     */
    private record Item(String type, String value) {
    }

    /** Exported levels in tree order, and the names of the exported restrictions by object id. */
    private static class ExportedFund {

        final List<List<Item>> levels = new ArrayList<>();
        final Map<String, String> restrictionNames = new HashMap<>();

        ExportedFund(Document doc) {
            NodeList sos = doc.getElementsByTagName("so");
            for (int i = 0; i < sos.getLength(); i++) {
                Element so = (Element) sos.item(i);
                values(itemsOf(so), RESTRICTION_NAME)
                        .forEach(name -> restrictionNames.put(so.getAttribute("id"), name));
            }
            NodeList lvls = doc.getElementsByTagName("lvl");
            for (int i = 0; i < lvls.getLength(); i++) {
                levels.add(itemsOf((Element) lvls.item(i)));
            }
        }

        List<Item> restrictedBy(String restrictionName) {
            List<List<Item>> found = levels.stream()
                    .filter(l -> values(l, RESTRICTION).stream()
                            .anyMatch(soId -> restrictionName.equals(restrictionNames.get(soId))))
                    .toList();
            assertEquals(1, found.size(), "Levels restricted by: " + restrictionName);
            return found.get(0);
        }

        private static List<Item> itemsOf(Element parent) {
            List<Item> items = new ArrayList<>();
            for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element e && e.hasAttribute("t")) {
                    items.add(new Item(e.getAttribute("t"), valueOf(e)));
                }
            }
            return items;
        }

        private static String valueOf(Element e) {
            if (e.hasAttribute("s")) {
                return e.getAttribute("s");
            }
            if (e.hasAttribute("soid")) {
                return e.getAttribute("soid");
            }
            if (e.hasAttribute("v")) {
                return e.getAttribute("v");
            }
            NodeList v = e.getElementsByTagName("v");
            assertTrue(v.getLength() > 0, "Item without value: " + e.getAttribute("t"));
            return v.item(0).getTextContent();
        }
    }
}
