package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrFindingAidVersionConformityInfo;


/**
 * @author Martin Šlapa
 * @since 2.12.2015
 */
@Repository
public interface FindingAidVersionConformityInfoRepository
        extends JpaRepository<ArrFindingAidVersionConformityInfo, Integer> {

    ArrFindingAidVersionConformityInfo findByFaVersion(ArrFindingAidVersion faVersion);

}
