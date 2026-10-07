package cz.tacr.elza.packageimport;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import cz.tacr.elza.core.data.PackageTranslations;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulApTypeDeclaration;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeDeclaration;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPackageDependency;
import cz.tacr.elza.domain.RulPartTypeDeclaration;
import cz.tacr.elza.domain.RulPartType;

/**
 * Precedence of declarations of one definition (entity class, part type, item type) by several packages, and
 * the summary of the declarations in the defining row.
 *
 * <p>Declarations are ordered as translations are: the package deeper in dependency order first,
 * ties by package code. The first declaration gives the owner and the structural values; the name is
 * the name of the first declaration in the language of the installation, or of the first declaration
 * when no package declares the definition in that language.
 */
public class PackageDeclarations {

    private final Map<Integer, Integer> depth;

    private final Integer defaultLanguageId;

    /**
     * @param dependencies
     *            all package dependencies
     * @param defaultLanguageId
     *            language of the installation ({@code elza.locale}), may be null
     */
    public PackageDeclarations(final Collection<RulPackageDependency> dependencies, final Integer defaultLanguageId) {
        this.depth = PackageTranslations.dependencyDepth(dependencies);
        this.defaultLanguageId = defaultLanguageId;
    }

    /**
     * Declarations by precedence, the winning one first.
     */
    public <T> List<T> ordered(final Collection<T> declarations, final Function<T, RulPackage> rulPackage) {
        Comparator<T> precedence = Comparator
                .comparing((T d) -> depth.getOrDefault(rulPackage.apply(d).getPackageId(), 0))
                .thenComparing(d -> rulPackage.apply(d).getCode());
        return declarations.stream().sorted(precedence.reversed()).toList();
    }

    /**
     * The declaration giving the name: the first one in the language of the installation, else the
     * first one.
     */
    private <T> T named(final List<T> ordered, final Function<T, RulPackage> rulPackage) {
        return ordered.stream()
                .filter(d -> Objects.equals(rulPackage.apply(d).getLanguageId(), defaultLanguageId))
                .findFirst()
                .orElse(ordered.get(0));
    }

    /**
     * Sets owner, name and read-only of the class from its declarations.
     *
     * @param declarations
     *            all declarations of the class, not empty
     */
    public void summarize(final ApType apType, final Collection<RulApTypeDeclaration> declarations) {
        List<RulApTypeDeclaration> ordered = ordered(declarations, RulApTypeDeclaration::getRulPackage);
        RulApTypeDeclaration first = ordered.get(0);
        apType.setRulPackage(first.getRulPackage());
        apType.setReadOnly(first.getReadOnly());
        apType.setName(named(ordered, RulApTypeDeclaration::getRulPackage).getName());
    }

    /**
     * Sets owner, name, child part and repeatable of the part type from its declarations; the child
     * part and repeatable are not checked between declarations - the first declaration decides.
     *
     * @param declarations
     *            all declarations of the part type, not empty
     */
    public void summarize(final RulPartType partType, final Collection<RulPartTypeDeclaration> declarations) {
        List<RulPartTypeDeclaration> ordered = ordered(declarations, RulPartTypeDeclaration::getRulPackage);
        RulPartTypeDeclaration first = ordered.get(0);
        partType.setRulPackage(first.getRulPackage());
        partType.setChildPart(first.getChildPart());
        partType.setRepeatable(first.getRepeatable());
        partType.setName(named(ordered, RulPartTypeDeclaration::getRulPackage).getName());
    }

    /**
     * Sets owner, texts and the remaining values of the item type from its declarations. Unlike
     * classes and part types, the owner stays the package that created the item type while it declares
     * it (the owner decides the position in {@code view_order}); the values come from the owner's
     * declaration. When the owner no longer declares it, the winning declaration takes over. Texts are
     * those of the first declaration in the language of the installation.
     *
     * @param declarations
     *            all declarations of the item type, not empty
     */
    public void summarize(final RulItemType itemType, final Collection<RulItemTypeDeclaration> declarations) {
        List<RulItemTypeDeclaration> ordered = ordered(declarations, RulItemTypeDeclaration::getRulPackage);
        Integer ownerId = itemType.getRulPackage() != null ? itemType.getRulPackage().getPackageId() : null;
        RulItemTypeDeclaration owner = ordered.stream()
                .filter(d -> d.getPackageId().equals(ownerId))
                .findFirst()
                .orElse(ordered.get(0));
        itemType.setRulPackage(owner.getRulPackage());
        itemType.setCanBeOrdered(owner.getCanBeOrdered());
        itemType.setStringLengthLimit(owner.getStringLengthLimit());
        itemType.setViewDefinitionJson(owner.getViewDefinition());
        RulItemTypeDeclaration named = named(ordered, RulItemTypeDeclaration::getRulPackage);
        itemType.setName(named.getName());
        itemType.setShortcut(named.getShortcut());
        itemType.setDescription(named.getDescription());
    }
}
