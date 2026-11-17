package cz.tacr.elza.rules.zp2015;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.ElzaCoreMain;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.dataexchange.input.DEImportParams;
import cz.tacr.elza.dataexchange.input.DEImportService;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.other.HelperTestService;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeConformityErrorRepository;
import cz.tacr.elza.repository.NodeConformityRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.StartupService;
import cz.tacr.elza.service.UserService;

/**
 * The ZP2015 demo fund in {@code elza-development/demo-data/ZP2015} must stay importable with the
 * current exchange format and rules. The file is read from that directory, not from a copy, so a
 * change of the format or of ZP2015 that breaks the demo data fails here.
 *
 * <p>The fund names a real archive (215000010) from the CZ_BASE institution list; the test
 * imports it into the test institution {@code in1} instead.
 *
 * <p>Context configuration and package handling follow {@link Zp2015AccessRestrictExportTest}.
 */
@ContextConfiguration(classes = ElzaCoreMain.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Zp2015DemoDataImportTest {

    /** Relative to the elza-core module, the working directory of the test run. */
    private static final Path DEMO_FUND = Path.of("..", "elza-development", "demo-data", "ZP2015", "mnv-lhota.xml");
    private static final String FUND_NAME = "Místní národní výbor Lhota";
    private static final String DEMO_INSTITUTION = "ic=\"215000010\"";
    private static final String TEST_INSTITUTION = "ic=\"in1\"";
    private static final int LEVEL_COUNT = 49;

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
    private FundRepository fundRepository;
    @Autowired
    private FundVersionRepository fundVersionRepository;
    @Autowired
    private NodeConformityRepository nodeConformityRepository;
    @Autowired
    private NodeConformityErrorRepository nodeConformityErrorRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private ArrFundVersion fundVersion;

    @BeforeAll
    void importFund() throws IOException {
        helperTestService.deleteTables(false);
        startupService.startNow();
        helperTestService.loadPackage("CZ_BASE", "package-cz-base");
        helperTestService.loadPackage("ZP2015", "rules-cz-zp2015");

        authorizeAsAdmin();
        try (InputStream is = new FileInputStream(AbstractTest.getResourceFile("institution-import.xml"))) {
            importXml(is);
        }
        String xml = Files.readString(DEMO_FUND, StandardCharsets.UTF_8);
        assertTrue(xml.contains(DEMO_INSTITUTION), "Demo fund institution changed: " + DEMO_FUND);
        importXml(new ByteArrayInputStream(
                xml.replace(DEMO_INSTITUTION, TEST_INSTITUTION).getBytes(StandardCharsets.UTF_8)));
        // the import queues validation of the new nodes; let it finish before the tests run
        helperTestService.waitForWorkers();

        ArrFund fund = fundRepository.findAll().stream()
                .filter(f -> FUND_NAME.equals(f.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Fund not imported: " + FUND_NAME));
        fundVersion = fundVersionRepository.findByFundIdAndLockChangeIsNull(fund.getFundId());
        assertNotNull(fundVersion, "Fund has no open version");
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

    /** Every level is imported and validated by the ZP2015 rules. */
    @Test
    void allLevelsAreImportedAndValidated() {
        assertEquals(LEVEL_COUNT, nodeConformityRepository.findByFundVersion(fundVersion).size());
    }

    /**
     * Nothing in the demo fund contradicts the rules: no item or specification the rules forbid at
     * its level, no reference to an unapproved entity. Missing items are allowed - the fund is a
     * fund in processing, and reference codes and serial numbers come from bulk actions run after
     * the import.
     */
    @Test
    void dataDoesNotViolateRules() {
        List<String> errors = new TransactionTemplate(transactionManager).execute(status -> {
            List<String> found = new ArrayList<>();
            nodeConformityErrorRepository.findErrorsByFundVersion(fundVersion).forEach(e -> found.add(
                    "node " + e.getNodeConformity().getNode().getUuid() + ": " + e.getDescription()));
            return found;
        });
        assertEquals(List.of(), errors);
    }

    /* helpers */

    private void authorizeAsAdmin() {
        UserDetail userDetail = userService.createUserDetail((Integer) null);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("", "", null);
        auth.setDetails(userDetail);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void importXml(InputStream is) {
        ApScope scope = apService.getApScope(1);
        assertNotNull(scope);
        deImportService.importData(is, new DEImportParams(scope.getScopeId(), 1000, 10000, null, null));
    }
}
