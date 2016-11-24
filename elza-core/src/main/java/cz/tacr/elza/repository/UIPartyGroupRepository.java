package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.UIPartyGroup;

/**
 * Repository pro {@link UIPartyGroup}.
 */
@Repository
public interface UIPartyGroupRepository extends JpaRepository<UIPartyGroup, Integer>, Packaging<UIPartyGroup> {

}
