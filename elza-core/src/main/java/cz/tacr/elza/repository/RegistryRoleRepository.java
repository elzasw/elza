package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ParRegistryRole;
import cz.tacr.elza.domain.ParRelationRoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistryRoleRepository extends JpaRepository<ParRegistryRole, Integer>, Packaging<ParRegistryRole> {

    void deleteByRoleType(ParRelationRoleType parRelationRoleType);

    void deleteByApType(ApType apType);
}
