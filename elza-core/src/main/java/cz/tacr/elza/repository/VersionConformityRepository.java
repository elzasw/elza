package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrVersionConformity;


/**
 * @author Martin Šlapa
 * @since 2.12.2015
 */
@Repository
public interface VersionConformityRepository
        extends JpaRepository<ArrVersionConformity, Integer> {

    ArrVersionConformity findByFundVersion(ArrFundVersion fundVersion);

}
