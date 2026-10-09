package cz.tacr.elza.packageimport.autoimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.packageimport.xml.PackageDependency;
import cz.tacr.elza.packageimport.xml.PackageInfo;

/**
 * Selection of the packages of the dpkg directory at startup: the directory holds CZ_BASE,
 * ZP2015 depending on it, and the independent ISAAR_CPF.
 */
public class AutoImportSelectionTest {

    private static final String CZ_BASE = "CZ_BASE";
    private static final String ZP2015 = "ZP2015";
    private static final String ISAAR_CPF = "ISAAR_CPF";

    private static PackageInfoWrapper pkg(String code, String... dependencies) {
        PackageInfo info = new PackageInfo();
        info.setCode(code);
        info.setVersion(1);
        info.setDependencies(Arrays.stream(dependencies).map(d -> {
            PackageDependency dependency = new PackageDependency();
            dependency.setCode(d);
            dependency.setMinVersion(1);
            return dependency;
        }).toList());
        return new PackageInfoWrapper(info, null);
    }

    private static Map<String, PackageInfoWrapper> directory() {
        Map<String, PackageInfoWrapper> available = new LinkedHashMap<>();
        available.put(CZ_BASE, pkg(CZ_BASE));
        available.put(ZP2015, pkg(ZP2015, CZ_BASE));
        available.put(ISAAR_CPF, pkg(ISAAR_CPF));
        return available;
    }

    @Test
    void emptyInstallationWithoutKeyLoadsEverything() {
        AutoImportSelection.Result result = AutoImportSelection.select(null, List.of(), directory());

        assertTrue(result.bootstrap());
        assertEquals(Set.of(CZ_BASE, ZP2015, ISAAR_CPF), result.codes());
        assertTrue(result.skipped().isEmpty());
    }

    @Test
    void emptyInstallationWithEmptyKeyLoadsNothing() {
        AutoImportSelection.Result result = AutoImportSelection.select(List.of(), List.of(), directory());

        assertFalse(result.bootstrap());
        assertTrue(result.codes().isEmpty());
        assertEquals(Set.of(CZ_BASE, ZP2015, ISAAR_CPF), result.skipped());
    }

    @Test
    void installedPackagesKeepUpgradingNewOnesAreSkipped() {
        AutoImportSelection.Result result = AutoImportSelection.select(null, List.of(CZ_BASE, ZP2015), directory());

        assertFalse(result.bootstrap());
        assertEquals(Set.of(CZ_BASE, ZP2015), result.codes());
        assertEquals(Set.of(ISAAR_CPF), result.skipped());
        assertTrue(result.implied().isEmpty());
    }

    @Test
    void enabledPackageIsAddedToInstalledOnes() {
        AutoImportSelection.Result result = AutoImportSelection.select(List.of(ISAAR_CPF), List.of(CZ_BASE, ZP2015),
                                                                       directory());

        assertEquals(Set.of(CZ_BASE, ZP2015, ISAAR_CPF), result.codes());
        assertTrue(result.skipped().isEmpty());
        assertTrue(result.implied().isEmpty());
    }

    @Test
    void dependenciesOfEnabledPackagesAreImplied() {
        AutoImportSelection.Result result = AutoImportSelection.select(List.of(ZP2015), List.of(), directory());

        assertEquals(Set.of(ZP2015, CZ_BASE), result.codes());
        assertEquals(Set.of(CZ_BASE), result.implied());
        assertEquals(Set.of(ISAAR_CPF), result.skipped());
    }

    @Test
    void enabledPackageMissingFromDirectoryIsReported() {
        AutoImportSelection.Result result = AutoImportSelection.select(List.of(" ISAAR_CPF ", "", "OTHER"),
                                                                       List.of(CZ_BASE), directory());

        assertEquals(Set.of(CZ_BASE, ISAAR_CPF, "OTHER"), result.codes());
        assertEquals(Set.of("OTHER"), result.unavailable());
        assertEquals(Set.of(ZP2015), result.skipped());
    }

    @Test
    void dependencyOnPackageOutsideDirectoryIsNotImplied() {
        Map<String, PackageInfoWrapper> available = new LinkedHashMap<>();
        available.put(ZP2015, pkg(ZP2015, CZ_BASE));

        AutoImportSelection.Result result = AutoImportSelection.select(List.of(ZP2015), List.of(), available);

        assertEquals(Set.of(ZP2015, CZ_BASE), result.codes());
        assertTrue(result.implied().isEmpty());
        assertTrue(result.unavailable().isEmpty());
    }
}
