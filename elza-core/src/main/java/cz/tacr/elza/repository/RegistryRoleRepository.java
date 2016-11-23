package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParRegistryRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistryRoleRepository extends JpaRepository<ParRegistryRole, Integer>, Packaging<ParRegistryRole> {

}
