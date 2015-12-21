package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFindingAid;

/**
 * Respozitory pro Archivní pomůcku.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Repository
public interface FindingAidRepository extends JpaRepository<ArrFindingAid, Integer> {

    @Query(value = "select fa from arr_finding_aid_version v join v.findingAid fa join v.rootLevel l join l.node n where n.uuid = :uuid and v.lockChange is null")
    ArrFindingAid findFindingAidByRootNodeUUID(@Param(value = "uuid") String uuid);

}
