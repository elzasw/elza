package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPackageDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pro {@link RulPackageDependency}
 *
 * @since 15.09.2017
 */
@Repository
public interface PackageDependencyRepository extends JpaRepository<RulPackageDependency, Integer> {

    /**
     * Vyhledá současné vazby podle zdrojového balíčku.
     *
     * @param rulPackage zdrojový balíček
     */
    List<RulPackageDependency> findByRulPackage(RulPackage rulPackage);

    /**
     * Odstraní současné vazby podle zdrojového balíčku.
     *
     * @param rulPackage zdrojový balíček
     */
    void deleteByRulPackage(RulPackage rulPackage);

    /**
     * Odstraní současné vazby podle cílového balíčku.
     *
     * @param dependsOnPackage cílový balíček
     */
    void deleteByDependsOnPackage(RulPackage dependsOnPackage);

    /**
     * Vyhledá závislé balíčky.
     *
     * @param dependsOnPackage závislý balíček
     */
    List<RulPackageDependency> findByDependsOnPackage(RulPackage dependsOnPackage);
}
