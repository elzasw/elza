package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPackageActions;


/**
 * Repository pro nainportované hromadné akce.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@Repository
public interface PackageActionsRepository extends JpaRepository<RulPackageActions, Integer> {


    List<RulPackageActions> findByRulPackage(RulPackage rulPackage);


    void deleteByRulPackage(RulPackage rulPackage);

}
