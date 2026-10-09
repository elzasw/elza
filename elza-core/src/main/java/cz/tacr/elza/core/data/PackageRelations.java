package cz.tacr.elza.core.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.tacr.elza.domain.RulPackageDependency;

/**
 * Relations of packages through their dependencies.
 */
public final class PackageRelations {

    private PackageRelations() {
    }

    /**
     * Packages related to each package: the package itself, the packages it depends on and the
     * packages depending on it, both transitively. Two packages that only share a dependency, or
     * that only share a package depending on both, are not related. A package without any
     * dependency in either direction is absent from the result (related only to itself).
     *
     * @param dependencies
     *            all dependencies of the installation
     * @return package id to the ids of its related packages (including itself)
     */
    public static Map<Integer, Set<Integer>> relatedPackages(final Collection<RulPackageDependency> dependencies) {
        Map<Integer, List<Integer>> dependsOn = new HashMap<>();
        Map<Integer, List<Integer>> dependents = new HashMap<>();
        for (RulPackageDependency dependency : dependencies) {
            dependsOn.computeIfAbsent(dependency.getPackageId(), k -> new ArrayList<>())
                    .add(dependency.getDependsOnPackageId());
            dependents.computeIfAbsent(dependency.getDependsOnPackageId(), k -> new ArrayList<>())
                    .add(dependency.getPackageId());
        }
        Set<Integer> packageIds = new HashSet<>(dependsOn.keySet());
        packageIds.addAll(dependents.keySet());
        Map<Integer, Set<Integer>> result = new HashMap<>();
        for (Integer packageId : packageIds) {
            Set<Integer> related = new HashSet<>();
            related.add(packageId);
            collect(packageId, dependsOn, related);
            collect(packageId, dependents, related);
            result.put(packageId, related);
        }
        return result;
    }

    private static void collect(final Integer packageId, final Map<Integer, List<Integer>> edges,
                                final Set<Integer> into) {
        for (Integer next : edges.getOrDefault(packageId, List.of())) {
            if (into.add(next)) {
                collect(next, edges, into);
            }
        }
    }
}
