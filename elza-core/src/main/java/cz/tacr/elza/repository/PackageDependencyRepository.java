package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPackageDependency;

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
     * @param rulPackage
     *            zdrojový balíček
     */
    @Query("SELECT d FROM rul_package_dependency d JOIN FETCH d.dependsOnPackage WHERE d.rulPackage = :rulPackage")
    List<RulPackageDependency> findByRulPackage(@Param("rulPackage") RulPackage rulPackage);

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
