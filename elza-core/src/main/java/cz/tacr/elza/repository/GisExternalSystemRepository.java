package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.GisExternalSystem;

/**
 * Repository GIS systémů
 */
@Repository
public interface GisExternalSystemRepository extends JpaRepository<GisExternalSystem, Integer> {

}
