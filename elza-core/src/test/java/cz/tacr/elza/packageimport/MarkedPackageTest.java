package cz.tacr.elza.packageimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;

import cz.tacr.elza.ElzaCoreMain;
import cz.tacr.elza.controller.RuleController;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.other.HelperTestService;
import cz.tacr.elza.packageimport.AvailablePackageService.AvailablePackage;
import cz.tacr.elza.packageimport.AvailablePackageService.State;
import cz.tacr.elza.packageimport.autoimport.AutoImportSelection;
import cz.tacr.elza.packageimport.autoimport.PackageInfoWrapper;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.StartupService;
import cz.tacr.elza.service.UserService;

/**
 * A package of the dpkg directory marked for the import at the next start: the mark is a package
 * row with version 0, the startup selection treats it as installed, the import over it installs
 * the package, and the administration lists it among the available packages, not the installed ones.
 *
 * <p>Packages stay installed across test classes, so the class removes all of them first and at the
 * end; later test classes import the packages they need again.
 */
@ContextConfiguration(classes = ElzaCoreMain.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MarkedPackageTest {

    private static final String CODE = "ENTITY_STANDALONE_TEST";
    private static final String DIR = "entity-standalone-test";
    private static final String FILE = "entity-standalone-test.zip";
    /** depends on ZP2015, which is neither installed nor in the directory */
    private static final String ADDON_CODE = "ADDON_TEST";
    private static final String ADDON_DIR = "rules-addon-test";

    @TempDir
    static Path dpkgDir;

    @Autowired
    private HelperTestService helperTestService;
    @Autowired
    private StartupService startupService;
    @Autowired
    private StaticDataService staticDataService;
    @Autowired
    private PackageService packageService;
    @Autowired
    private AvailablePackageService availablePackageService;
    @Autowired
    private PackageRepository packageRepository;
    @Autowired
    private RuleController ruleController;
    @Autowired
    private UserService userService;

    @BeforeAll
    void prepareDirectory() throws Exception {
        startupService.startNow();
        authorizeAsAdmin();
        // validation workers of a preceding test class may still write; they would collide with the delete
        helperTestService.waitForWorkers();
        helperTestService.deleteAllPackages();
        copyPackage(DIR, FILE);
        copyPackage(ADDON_DIR, "rules-addon-test.zip");
    }

    @AfterAll
    void unloadPackages() {
        try {
            helperTestService.waitForWorkers();
            helperTestService.deleteAllPackages();
        } finally {
            startupService.stop();
        }
    }

    /** No package is installed and the key is not set, so the next start imports every file. */
    @Test
    @Order(1)
    void directoryListsPackagesNotInstalled() {
        List<AvailablePackage> items = availablePackageService.listAvailable(dpkgDir);
        assertEquals(List.of(ADDON_CODE, CODE), codes(items));
        items.forEach(item -> assertEquals(State.CONFIGURED, item.state()));
        assertEquals(FILE, find(CODE).pkg().getPath().getFileName().toString());
    }

    @Test
    @Order(2)
    void markRefusedWhenRequiredPackageIsMissing() {
        BusinessException e = assertThrows(BusinessException.class,
                                           () -> availablePackageService.mark(dpkgDir, ADDON_CODE));
        assertEquals(PackageCode.FOREIGN_PACKAGES_NOT_EXIST, e.getErrorCode());
        assertNull(packageRepository.findByCode(ADDON_CODE));
    }

    @Test
    @Order(3)
    void markIsPendingRowListedAsMarked() {
        availablePackageService.mark(dpkgDir, CODE);

        RulPackage row = packageRepository.findByCode(CODE);
        assertNotNull(row);
        assertTrue(row.isPending());
        assertEquals(State.MARKED, find(CODE).state());
        // a package is installed now, so the other file is left alone at the next start
        assertEquals(State.NOT_LOADED, find(ADDON_CODE).state());
        assertTrue(availablePackageService.isRestartRequired());
        // the installed packages of the administration do not show the mark
        assertTrue(ruleController.getPackages().stream().noneMatch(p -> CODE.equals(p.getCode())));
        // marking again does nothing
        availablePackageService.mark(dpkgDir, CODE);
        assertEquals(row.getPackageId(), packageRepository.findByCode(CODE).getPackageId());
    }

    @Test
    @Order(4)
    void startupSelectionTreatsMarkAsInstalled() throws IOException {
        Map<String, PackageInfoWrapper> available = PackageUtils.readPackageDirectory(dpkgDir);
        Set<String> installed = packageRepository.findAll().stream().map(RulPackage::getCode)
                .collect(Collectors.toSet());

        AutoImportSelection.Result selection = AutoImportSelection.select(null, installed, available);

        assertTrue(selection.codes().contains(CODE));
        assertEquals(Set.of(ADDON_CODE), selection.skipped());
    }

    @Test
    @Order(5)
    void unmarkDeletesPendingRow() {
        availablePackageService.unmark(CODE);

        assertNull(packageRepository.findByCode(CODE));
        // the installation is empty again: every file is imported at the next start
        assertEquals(State.CONFIGURED, find(CODE).state());
        ObjectNotFoundException e = assertThrows(ObjectNotFoundException.class,
                                                 () -> availablePackageService.unmark(CODE));
        assertEquals(PackageCode.PACKAGE_NOT_EXIST, e.getErrorCode());
    }

    @Test
    @Order(6)
    void importOverPendingRowInstallsPackage() {
        availablePackageService.mark(dpkgDir, CODE);

        // as the startup import does
        packageService.preImportPackage();
        packageService.importPackageInternal(dpkgDir.resolve(FILE).toFile(), true);
        staticDataService.refreshForCurrentThread();

        RulPackage row = packageRepository.findByCode(CODE);
        assertFalse(row.isPending());
        assertEquals(1, row.getVersion());
        assertEquals(List.of(ADDON_CODE), codes(availablePackageService.listAvailable(dpkgDir)));
        assertTrue(ruleController.getPackages().stream().anyMatch(p -> CODE.equals(p.getCode())));
        assertEquals(PackageCode.ALREADY_INSTALLED,
                     assertThrows(BusinessException.class, () -> availablePackageService.mark(dpkgDir, CODE))
                             .getErrorCode());
        assertEquals(PackageCode.ALREADY_INSTALLED,
                     assertThrows(BusinessException.class, () -> availablePackageService.unmark(CODE))
                             .getErrorCode());
    }

    private static void copyPackage(String resourceDir, String fileName) throws Exception {
        File zip = HelperTestService.buildPackageFileZip(resourceDir);
        try {
            Files.copy(zip.toPath(), dpkgDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            zip.delete();
        }
    }

    private AvailablePackage find(String code) {
        return availablePackageService.listAvailable(dpkgDir).stream()
                .filter(item -> code.equals(item.pkg().getCode()))
                .findFirst()
                .orElseThrow();
    }

    private static List<String> codes(List<AvailablePackage> items) {
        return items.stream().map(item -> item.pkg().getCode()).toList();
    }

    private void authorizeAsAdmin() {
        UserDetail userDetail = userService.createUserDetail((Integer) null);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("", "", null);
        auth.setDetails(userDetail);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
