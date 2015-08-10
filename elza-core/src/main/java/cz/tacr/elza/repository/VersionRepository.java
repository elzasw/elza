package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.FaChange;
import cz.tacr.elza.domain.FaVersion;
import cz.tacr.elza.domain.FindingAid;


/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Repository
public interface VersionRepository extends JpaRepository<FaVersion, Integer> {

    @Query(value = "select v from arr_fa_version v join v.createChange ch join v.findingAid fa where fa.findingAidId = :findingAidId order by ch.changeDate desc")
    List<FaVersion> findVersionsByFindingAidIdOrderByCreateDateAsc(@Param(value = "findingAidId") Integer findingAidId);

    FaVersion findTopByFindingAid(FindingAid findingAid);

    FaVersion findByFindingAidAndLockChange(FindingAid findingAid, FaChange lockChange);

    FaVersion findByFindingAidAndLockChangeIsNull(FindingAid findingAid);

    @Query(value = "select v from arr_fa_version v join v.findingAid fa where fa.findingAidId = :findingAidId and v.lockChange is null")
    List<FaVersion> findByFindingAidIdAndLockChangeIsNull(@Param(value = "findingAidId") Integer findingAidId);
}
