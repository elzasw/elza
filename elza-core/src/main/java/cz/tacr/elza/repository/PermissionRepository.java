package cz.tacr.elza.repository;

import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Respozitory pro {@link UsrPermission}.
 *
 * @author Martin Šlapa
 * @since 26.04.2016
 */
@Repository
public interface PermissionRepository  extends JpaRepository<UsrPermission, Integer> {

    @Query("SELECT p FROM usr_permission p LEFT JOIN FETCH p.scope s LEFT JOIN FETCH p.fund f WHERE p.user = :user OR p.group.groupId IN (SELECT gu.group.groupId FROM usr_group_user gu WHERE gu.user = :user)")
    List<UsrPermission> getPermissions(@Param("user") UsrUser user);
}
