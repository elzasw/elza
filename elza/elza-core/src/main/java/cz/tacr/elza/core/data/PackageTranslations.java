package cz.tacr.elza.core.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPackageDependency;
import cz.tacr.elza.domain.RulTranslation;
import cz.tacr.elza.domain.TranslationEntityType;

/**
 * Immutable lookup of package-provided translations, part of the static data.
 *
 * When several packages translate the same text into the same language, the package latest in
 * dependency order wins: a package that depends (directly or not) on another one overrides its
 * translations. Packages without a dependency path between them are ordered by code.
 */
public class PackageTranslations {

    private record Key(String entityType, String entityCode, String field) {
    }

    /** Text key -> (language id -> value). */
    private final Map<Key, Map<Integer, String>> values;

    private PackageTranslations(Map<Key, Map<Integer, String>> values) {
        this.values = values;
    }

    /**
     * @return translation of the text into the language, null when there is none
     */
    public String get(TranslationEntityType entityType, String entityCode, String field, Integer languageId) {
        Map<Integer, String> byLanguage = values.get(new Key(entityType.name(), entityCode, field));
        return byLanguage != null ? byLanguage.get(languageId) : null;
    }

    public static PackageTranslations empty() {
        return new PackageTranslations(Map.of());
    }

    static PackageTranslations build(Collection<RulTranslation> rows,
                                     Collection<RulPackage> packages,
                                     Collection<RulPackageDependency> dependencies) {
        Map<Integer, Integer> depth = dependencyDepth(dependencies);
        Map<Integer, String> packageCodes = new HashMap<>();
        packages.forEach(p -> packageCodes.put(p.getPackageId(), p.getCode()));

        Comparator<RulTranslation> precedence = Comparator
                .comparing((RulTranslation t) -> depth.getOrDefault(t.getPackageId(), 0))
                .thenComparing(t -> packageCodes.getOrDefault(t.getPackageId(), ""));
        List<RulTranslation> ordered = rows.stream().sorted(precedence).toList();

        Map<Key, Map<Integer, String>> values = new HashMap<>();
        for (RulTranslation row : ordered) {
            Key key = new Key(row.getEntityType(), row.getEntityCode(), row.getField());
            values.computeIfAbsent(key, k -> new HashMap<>()).put(row.getLanguageId(), row.getTextValue());
        }
        return new PackageTranslations(values);
    }

    /**
     * Length of the longest dependency chain below each package: 0 for a package without
     * dependencies, 1 for a package depending only on such packages, and so on.
     */
    static Map<Integer, Integer> dependencyDepth(Collection<RulPackageDependency> dependencies) {
        Map<Integer, List<Integer>> dependsOn = new HashMap<>();
        for (RulPackageDependency d : dependencies) {
            dependsOn.computeIfAbsent(d.getPackageId(), k -> new ArrayList<>()).add(d.getDependsOnPackageId());
        }
        Map<Integer, Integer> depth = new HashMap<>();
        for (Integer packageId : dependsOn.keySet()) {
            computeDepth(packageId, dependsOn, depth);
        }
        return depth;
    }

    private static int computeDepth(Integer packageId, Map<Integer, List<Integer>> dependsOn,
                                    Map<Integer, Integer> depth) {
        Integer known = depth.get(packageId);
        if (known != null) {
            return known;
        }
        int result = 0;
        for (Integer dependency : dependsOn.getOrDefault(packageId, List.of())) {
            result = Math.max(result, computeDepth(dependency, dependsOn, depth) + 1);
        }
        depth.put(packageId, result);
        return result;
    }
}
