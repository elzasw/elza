package cz.tacr.elza.core.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPackageDependency;

/**
 * Related packages: an installation with CZ_BASE (1), ZP2015 (2) depending on it, an addon (3)
 * depending on ZP2015, ISAAR_CPF (4) independent, and a local package (5) depending on both CZ_BASE
 * and ISAAR_CPF.
 */
public class PackageRelationsTest {

    private static final int CZ_BASE = 1;
    private static final int ZP2015 = 2;
    private static final int ADDON = 3;
    private static final int ISAAR_CPF = 4;
    private static final int LOCAL = 5;

    private static RulPackageDependency dependency(int packageId, int dependsOnPackageId) {
        RulPackage rulPackage = new RulPackage();
        rulPackage.setPackageId(packageId);
        RulPackage dependsOn = new RulPackage();
        dependsOn.setPackageId(dependsOnPackageId);
        RulPackageDependency dependency = new RulPackageDependency();
        dependency.setRulPackage(rulPackage);
        dependency.setDependsOnPackage(dependsOn);
        return dependency;
    }

    @Test
    void dependenciesAndDependentsAreRelatedSiblingsAreNot() {
        Map<Integer, Set<Integer>> related = PackageRelations.relatedPackages(List.of(
                dependency(ZP2015, CZ_BASE),
                dependency(ADDON, ZP2015),
                dependency(LOCAL, CZ_BASE),
                dependency(LOCAL, ISAAR_CPF)));

        // the owner of the shared codes sees everything built on it
        assertEquals(Set.of(CZ_BASE, ZP2015, ADDON, LOCAL), related.get(CZ_BASE));
        // a rule set sees its dependencies and its addons
        assertEquals(Set.of(ZP2015, CZ_BASE, ADDON), related.get(ZP2015));
        assertEquals(Set.of(ADDON, ZP2015, CZ_BASE), related.get(ADDON));
        // the independent framework sees the local package depending on it, not CZ_BASE
        assertEquals(Set.of(ISAAR_CPF, LOCAL), related.get(ISAAR_CPF));
        // the local package sees both frameworks it depends on, not their other dependents
        assertEquals(Set.of(LOCAL, CZ_BASE, ISAAR_CPF), related.get(LOCAL));
    }

    @Test
    void packageWithoutDependenciesIsAbsent() {
        Map<Integer, Set<Integer>> related = PackageRelations.relatedPackages(List.of(dependency(ZP2015, CZ_BASE)));
        assertNull(related.get(ISAAR_CPF));
        assertEquals(Set.of(CZ_BASE, ZP2015), related.get(CZ_BASE));
    }
}
