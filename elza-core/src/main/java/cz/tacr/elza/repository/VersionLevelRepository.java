package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.Version;
import cz.tacr.elza.domain.VersionLevel;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Repository
public interface VersionLevelRepository extends JpaRepository<VersionLevel, Integer> {

    List<VersionLevel> findByVersion(List<Version> versions);

}
