package cz.tacr.elza.rules.zp2015;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.ElzaCoreMain;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.dataexchange.input.DEImportParams;
import cz.tacr.elza.dataexchange.input.DEImportService;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.other.HelperTestService;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.OutputFileRepository;
import cz.tacr.elza.repository.OutputRepository;
import cz.tacr.elza.repository.OutputTypeRepository;
import cz.tacr.elza.repository.TemplateRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.OutputService;
import cz.tacr.elza.service.StartupService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.dms.DmsService;
import cz.tacr.elza.service.output.OutputRequestStatus;

/**
 * Generates the ZP2015 EAD output ({@code INVENTAR_EAD/index.ftl}) for an imported fund and checks
 * the result against the Czech EAD3 profile. The generator also validates the file against
 * {@code ead3.xsd}, so a generated output is schema-valid.
 *
 * <p>Context configuration and package handling follow {@link Zp2015AccessRestrictExportTest}.
 */
@ContextConfiguration(classes = ElzaCoreMain.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Zp2015EadExportTest {

    private static final String FUND_XML = "zp2015/ead-export-fund.xml";
    private static final String FUND_NAME = "LC-TEST-ead_export";

    private static final String EAD_NS = "http://ead3.archivists.org/schema/";

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
    private AccessPointService apService;
    @Autowired
    private UserService userService;
    @Autowired
    private OutputService outputService;
    @Autowired
    private DmsService dmsService;
    @Autowired
    private FundRepository fundRepository;
    @Autowired
    private FundVersionRepository fundVersionRepository;
    @Autowired
    private OutputRepository outputRepository;
    @Autowired
    private OutputTypeRepository outputTypeRepository;
    @Autowired
    private TemplateRepository templateRepository;
    @Autowired
    private OutputFileRepository outputFileRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private Document ead;
    private XPath xpath;

    @BeforeAll
    void generateEad() throws Exception {
        helperTestService.deleteTables(false);
        startupService.startNow();
        helperTestService.loadPackage("CZ_BASE", "package-cz-base");
        helperTestService.loadPackage("ZP2015", "rules-cz-zp2015");

        authorizeAsAdmin();
        importXml("institution-import.xml");
        importXml(FUND_XML);
        // the import queues validation of the new nodes; let it finish before the output is generated
        helperTestService.waitForWorkers();

        ArrFund fund = fundRepository.findAll().stream()
                .filter(f -> FUND_NAME.equals(f.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Fund not imported: " + FUND_NAME));

        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        int outputId = tt.execute(status -> {
            ArrFundVersion version = fundVersionRepository.findByFundIdAndLockChangeIsNull(fund.getFundId());
            assertNotNull(version, "Fund has no open version");
            Integer typeId = outputTypeRepository.findByCode("ZP2015_INVENTAR").getOutputTypeId();
            Integer templateId = templateRepository.findByCode("ZP2015_INVENTAR_EAD").getTemplateId();
            ArrOutput output = outputService.createOutput(version, "EAD test", "EAD_TEST", typeId,
                    List.of(templateId), null).getOutput();
            outputService.addNodesNamedOutput(version, output, List.of(version.getRootNode().getNodeId()));
            OutputRequestStatus requestStatus = outputService.addRequest(output.getOutputId(), version, false, null);
            assertEquals(OutputRequestStatus.OK, requestStatus);
            return output.getOutputId();
        });
        helperTestService.waitForWorkers();

        ead = tt.execute(status -> {
            ArrOutput output = outputRepository.findById(outputId).orElseThrow();
            assertEquals(null, output.getError(), "Output generation failed");
            List<ArrOutputFile> files = outputFileRepository.findByOutputResultOutput(output);
            assertEquals(1, files.size(), "Generated files");
            Path path = dmsService.getFilePath(files.get(0));
            try (InputStream is = Files.newInputStream(path)) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                return dbf.newDocumentBuilder().parse(is);
            } catch (Exception e) {
                throw new AssertionError("Cannot parse generated EAD", e);
            }
        });

        xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                return "ead".equals(prefix) ? EAD_NS : null;
            }

            @Override
            public String getPrefix(String namespaceURI) {
                return null;
            }

            @Override
            public Iterator<String> getPrefixes(String namespaceURI) {
                return null;
            }
        });
    }

    /** See {@link Zp2015AccessRestrictExportTest#unloadRules()}. */
    @AfterAll
    void unloadRules() {
        try {
            helperTestService.waitForWorkers();
            helperTestService.deleteTables(false);
            packageService.deletePackage("ZP2015");
            staticDataService.refreshForCurrentThread();
        } finally {
            startupService.stop();
        }
    }

    @Test
    void profileVersion() {
        assertEquals(List.of("CZ_EAD3_PROFILE_20260501"),
                values("//ead:localcontrol[@localtype='CZ_FINDING_AID_EAD_PROFILE']/ead:term/@identifier"));
    }

    @Test
    void rules() {
        assertEquals(List.of("CZ_ZP2013"),
                values("//ead:localcontrol[@localtype='RULES']/ead:term/@identifier"));
    }

    @Test
    void otherDatesHaveType() {
        assertEquals(List.of("DISPATCH", "DELIVERY", "PROCESSING", "CLOSING"),
                values(item() + "/ead:did/ead:unitdatestructured/ead:daterange/@localtype"));
    }

    @Test
    void entityRoles() {
        assertEquals(List.of("SCRIPTWRITER", "CAPTURED_ENTITY"),
                values(item() + "/ead:relations/ead:relation[ead:descriptivenote]/@linkrole"));
    }

    /** Roles stored as text (anonymised persons) are weak entities: no reference to an entity. */
    @Test
    void textRolesAreWeakEntities() {
        List<Element> relations = elements(item() + "/ead:relations/ead:relation[not(ead:descriptivenote)]");
        assertEquals(2, relations.size());
        assertRelation(relations.get(0), "cpfrelation", "AUTHOR", "autor", "Jan Novák (prokurista)");
        assertRelation(relations.get(1), "cpfrelation", "ENTITY", "související entita", "Josef Svoboda");
    }

    @Test
    void physicalFacets() {
        String facets = item() + "/ead:did/ead:physdescstructured[@physdescstructuredtype='materialtype']";
        assertEquals(List.of("ele"), values(facets + "/ead:unittype"));
        assertEquals(List.of("modrá"), values(facets + "/ead:physfacet[@localtype='COLOR']"));
        assertEquals(List.of("praenomen et nomen"), values(facets + "/ead:physfacet[@localtype='LEGEND']"));
        assertEquals(List.of("těsnopis"), values(facets + "/ead:physfacet[@localtype='WRITING']"));
    }

    @Test
    void numericScale() {
        assertEquals(List.of("1:200"), values(item() + "/ead:did/ead:materialspec[@localtype='SCALE_RATIO']"));
    }

    /** An identifier type without its own profile value is exported as JINE with a readable label. */
    @Test
    void otherIdentifiers() {
        assertEquals(List.of("CISLO_JEDNACI|číslo jednací|MV-2022/34-2", "JINE|osobní číslo|OC-12"),
                elements(item() + "/ead:did/ead:unitid").stream()
                        .map(e -> e.getAttribute("localtype") + "|" + e.getAttribute("label") + "|" + e.getTextContent())
                        .sorted().toList());
    }

    /** Every level carries the meaningless identifier of the profile. */
    @Test
    void levelsHaveUuidId() {
        for (Element level : elements("//ead:archdesc | //ead:c")) {
            String id = level.getAttribute("id");
            assertEquals(true, id.matches("uuid-[0-9a-f-]{36}"), "Level id: " + id);
            assertEquals("https://archdesc.nacr.cz/dids/" + id.substring(5), level.getAttribute("base"));
        }
    }

    /** A level without any exported item gets the meaningless unitid, an empty did is invalid. */
    @Test
    void levelWithoutItemsHasIdentifier() {
        List<Element> folders = elements("//ead:c[@level='file']");
        assertEquals(1, folders.size());
        String id = folders.get(0).getAttribute("id");
        assertEquals(List.of("ID|identifikátor jednotky popisu|" + id),
                elements("//ead:c[@level='file']/ead:did/*").stream()
                        .map(e -> e.getAttribute("localtype") + "|" + e.getAttribute("label") + "|" + e.getTextContent())
                        .toList());
    }

    /* helpers */

    private static String item() {
        return "//ead:c[@level='item']";
    }

    private static void assertRelation(Element relation, String type, String role, String title, String entry) {
        assertEquals(type, relation.getAttribute("relationtype"));
        assertEquals(role, relation.getAttribute("linkrole"));
        assertEquals(title, relation.getAttribute("linktitle"));
        assertEquals(entry, relation.getElementsByTagNameNS(EAD_NS, "relationentry").item(0).getTextContent());
    }

    private List<String> values(String expression) {
        try {
            NodeList nodes = (NodeList) xpath.evaluate(expression, ead, XPathConstants.NODESET);
            List<String> result = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                result.add(nodes.item(i).getTextContent().trim());
            }
            return result;
        } catch (Exception e) {
            throw new AssertionError("Invalid XPath: " + expression, e);
        }
    }

    private List<Element> elements(String expression) {
        try {
            NodeList nodes = (NodeList) xpath.evaluate(expression, ead, XPathConstants.NODESET);
            List<Element> result = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                result.add((Element) nodes.item(i));
            }
            return result;
        } catch (Exception e) {
            throw new AssertionError("Invalid XPath: " + expression, e);
        }
    }

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
}
