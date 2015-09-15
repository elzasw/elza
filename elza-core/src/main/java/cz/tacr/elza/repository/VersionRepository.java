package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFindingAidVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Repository
public interface VersionRepository extends JpaRepository<ArrFindingAidVersion, Integer> {

    @Query(value = "select v from arr_finding_aid_version v join v.createChange ch join v.findingAid fa where fa.findingAidId = :findingAidId order by ch.changeDate asc")
    List<ArrFindingAidVersion> findVersionsByFindingAidIdOrderByCreateDateAsc(@Param(value = "findingAidId") Integer findingAidId);

    @Query(value = "select v from arr_finding_aid_version v join v.findingAid fa where fa.findingAidId = :findingAidId and v.lockChange is null")
    ArrFindingAidVersion findByFindingAidIdAndLockChangeIsNull(@Param(value = "findingAidId") Integer findingAidId);
}
