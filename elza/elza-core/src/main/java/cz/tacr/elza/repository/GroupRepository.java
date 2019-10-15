package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Respozitory pro {@link UsrGroup}.
 *
 * @author Martin Šlapa
 * @since 11.04.2016
 */
@Repository
public interface GroupRepository extends ElzaJpaRepository<UsrGroup, Integer>, GroupRepositoryCustom {

    @Query("select ugu.group from usr_group_user ugu where ugu.user = :user")
    List<UsrGroup> findByUser(@Param("user") UsrUser user);

    @Query("select distinct p.group from usr_permission p where p.fund = :fund")
    List<UsrGroup> findByFund(@Param("fund") ArrFund fund);

    UsrGroup findOneByCode(String code);

    @Query("select distinct p.group from usr_permission p where p.permission in :permissions")
    List<UsrGroup> findByPermissions(@Param("permissions") Set<UsrPermission.Permission> permissions);
}
