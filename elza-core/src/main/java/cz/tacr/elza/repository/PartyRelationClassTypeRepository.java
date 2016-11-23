package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParRelationClassType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartyRelationClassTypeRepository extends JpaRepository<ParRelationClassType, Integer>, Packaging<ParRelationClassType> {

}
