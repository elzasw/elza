package cz.tacr.elza.packageimport;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cz.tacr.elza.core.data.PackageTranslations;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulApTypeDeclaration;
import cz.tacr.elza.domain.RulPackageDependency;

/**
 * Summary of the declarations of one entity class in {@link ApType}.
 *
 * <p>Declarations are ordered as translations are: the package deeper in dependency order first,
 * ties by package code. The first declaration gives the owner and read-only of the class; its name is
 * the name of the first declaration in the language of the installation, or of the first declaration
 * when no package declares the class in that language.
 */
public class ApTypeDeclarations {

    private final Map<Integer, Integer> depth;

    private final Integer defaultLanguageId;

    /**
     * @param dependencies
     *            all package dependencies
     * @param defaultLanguageId
     *            language of the installation ({@code elza.locale}), may be null
     */
    public ApTypeDeclarations(final Collection<RulPackageDependency> dependencies, final Integer defaultLanguageId) {
        this.depth = PackageTranslations.dependencyDepth(dependencies);
        this.defaultLanguageId = defaultLanguageId;
    }

    /**
     * Declarations by precedence, the winning one first.
     */
    public List<RulApTypeDeclaration> ordered(final Collection<RulApTypeDeclaration> declarations) {
        Comparator<RulApTypeDeclaration> precedence = Comparator
                .comparing((RulApTypeDeclaration d) -> depth.getOrDefault(d.getPackageId(), 0))
                .thenComparing(d -> d.getRulPackage().getCode());
        return declarations.stream().sorted(precedence.reversed()).toList();
    }

    /**
     * Sets owner, name and read-only of the class from its declarations.
     *
     * @param declarations
     *            all declarations of the class, not empty
     */
    public void summarize(final ApType apType, final Collection<RulApTypeDeclaration> declarations) {
        List<RulApTypeDeclaration> ordered = ordered(declarations);
        RulApTypeDeclaration first = ordered.get(0);
        RulApTypeDeclaration named = ordered.stream()
                .filter(d -> Objects.equals(d.getRulPackage().getLanguageId(), defaultLanguageId))
                .findFirst()
                .orElse(first);
        apType.setRulPackage(first.getRulPackage());
        apType.setReadOnly(first.getReadOnly());
        apType.setName(named.getName());
    }
}
