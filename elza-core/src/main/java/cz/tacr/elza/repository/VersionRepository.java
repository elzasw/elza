package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.FaVersion;


/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Repository
public interface VersionRepository extends JpaRepository<FaVersion, Integer> {

    @Query(value = "select v from arr_fa_version v join v.createChange ch join v.findingAid fa where fa.findingAidId = :findingAidId order by ch.changeDate asc")
    List<FaVersion> findVersionsByFindingAidIdOrderByCreateDateAsc(@Param(value = "findingAidId") Integer findingAidId);

    @Query(value = "select v from arr_fa_version v join v.findingAid fa where fa.findingAidId = :findingAidId and v.lockChange is null")
    FaVersion findByFindingAidIdAndLockChangeIsNull(@Param(value = "findingAidId") Integer findingAidId);
}
