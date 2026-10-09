package cz.tacr.elza.packageimport.autoimport;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.packageimport.xml.PackageDependency;

/**
 * Which packages of the {@code dpkg} directory the startup import considers.
 *
 * A package is considered when it is already installed, so that it keeps being upgraded from
 * the directory, or when {@code elza.packages.enabled} lists it; the dependencies of a considered
 * package are considered with it. The other packages of the directory stay untouched, also when
 * the administration deleted them. An installation without any package and without the key loads
 * every package of the directory.
 */
public final class AutoImportSelection {

    /**
     * Outcome of the selection.
     *
     * @param codes       codes the startup import considers (installed, enabled and their
     *                    dependencies; a code may have no file in the directory)
     * @param implied     codes of the directory considered only as dependencies of other
     *                    considered packages
     * @param skipped     codes of the directory the import leaves out
     * @param unavailable enabled codes that are neither installed nor in the directory
     * @param bootstrap   every package of the directory is considered because nothing is installed
     *                    and the key is not set
     */
    public record Result(Set<String> codes,
                         Set<String> implied,
                         Set<String> skipped,
                         Set<String> unavailable,
                         boolean bootstrap) {
    }

    private AutoImportSelection() {
    }

    /**
     * @param enabled   value of {@code elza.packages.enabled}, {@code null} when the key is not set
     * @param installed codes of the packages in the database
     * @param available packages of the directory by code
     */
    public static Result select(final Collection<String> enabled,
                                final Collection<String> installed,
                                final Map<String, PackageInfoWrapper> available) {
        if (enabled == null && installed.isEmpty()) {
            return new Result(Collections.unmodifiableSet(new LinkedHashSet<>(available.keySet())),
                              Collections.emptySet(),
                              Collections.emptySet(),
                              Collections.emptySet(),
                              true);
        }

        Set<String> seed = new LinkedHashSet<>(installed);
        if (enabled != null) {
            for (String code : enabled) {
                if (StringUtils.isNotBlank(code)) {
                    seed.add(code.trim());
                }
            }
        }

        // closure under the dependencies declared by the files of the directory
        Set<String> codes = new LinkedHashSet<>(seed);
        Set<String> implied = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>(seed);
        while (!queue.isEmpty()) {
            PackageInfoWrapper pkg = available.get(queue.poll());
            if (pkg == null || pkg.getDependencies() == null) {
                continue;
            }
            for (PackageDependency dependency : pkg.getDependencies()) {
                String code = dependency.getCode();
                if (code != null && codes.add(code)) {
                    implied.add(code);
                    queue.add(code);
                }
            }
        }
        implied.retainAll(available.keySet());

        Set<String> skipped = new LinkedHashSet<>(available.keySet());
        skipped.removeAll(codes);

        Set<String> unavailable = new LinkedHashSet<>(seed);
        unavailable.removeAll(installed);
        unavailable.removeAll(available.keySet());

        return new Result(Collections.unmodifiableSet(codes),
                          Collections.unmodifiableSet(implied),
                          Collections.unmodifiableSet(skipped),
                          Collections.unmodifiableSet(unavailable),
                          false);
    }
}
