package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Respozitory pro {@link UsrUser}.
 *
 * @author Martin Šlapa
 * @since 11.04.2016
 */
@Repository
public interface UserRepository extends ElzaJpaRepository<UsrUser, Integer>, UserRepositoryCustom {

    UsrUser findByUsername(String username);

    @Query("select ugu.user from usr_group_user ugu where ugu.group = :group")
    List<UsrUser> findByGroup(@Param("group") UsrGroup group);

    @Query("select distinct p.user from usr_permission p where p.fund = :fund")
    List<UsrUser> findByFund(@Param("fund") ArrFund fund);

    @Query("select distinct p.user from usr_permission p where p.permission in :permissions")
    List<UsrUser> findByPermissions(@Param("permissions") Set<UsrPermission.Permission> permissions);

    List<UsrUser> findByParty(ParParty party);
}
