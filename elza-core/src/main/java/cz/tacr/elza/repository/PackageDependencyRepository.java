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
     * @param sourcePackage zdrojový balíček
     */
    List<RulPackageDependency> findBySourcePackage(RulPackage sourcePackage);

    /**
     * Odstraní současné vazby podle zdrojového balíčku.
     *
     * @param sourcePackage zdrojový balíček
     */
    void deleteBySourcePackage(RulPackage sourcePackage);

    /**
     * Vyhledá závislé balíčky.
     *
     * @param targetPackage závislý balíček
     */
    List<RulPackageDependency> findByTargetPackage(RulPackage targetPackage);
}
