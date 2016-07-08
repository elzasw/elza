package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulPackage;


/**
 * Repository imporotvaných balíčků.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@Repository
public interface PackageRepository extends JpaRepository<RulPackage, Integer> {


    RulPackage findTopByCode(String code);
}
