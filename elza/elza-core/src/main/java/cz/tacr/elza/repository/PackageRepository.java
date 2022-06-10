package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PackageRepository extends JpaRepository<RulPackage, Integer> {

    RulPackage findByCode(String code);

    List<RulPackage> findByCodeIn(Collection<String> codes);
}
