package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import cz.tacr.elza.domain.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Respozitory pro {@link UsrUser}.
 *
 */
@Repository
public interface UserRepository extends ElzaJpaRepository<UsrUser, Integer>, UserRepositoryCustom {

    UsrUser findByUsername(String username);

    @Query("select u.userId from usr_user u where u.username in :names")
    List<Integer> findIdsByUsername(@Param("names") Collection<String> names);

    @Query("select ugu.user from usr_group_user ugu where ugu.group = :group")
    List<UsrUser> findByGroup(@Param("group") UsrGroup group);

    @Query("select distinct p.user from usr_permission p where p.fund = :fund")
    List<UsrUser> findByFund(@Param("fund") ArrFund fund);

    @Query("select distinct p.user from usr_permission p where p.permission in :permissions")
    List<UsrUser> findByPermissions(@Param("permissions") Set<UsrPermission.Permission> permissions);

    List<UsrUser> findByAccessPoint(ApAccessPoint accessPoint);

	/**
	 * Return user with fetched party and record
	 *
	 * @param userId
	 *            User to be fetched from DB
	 * @return
	 */
	@Query("select u from usr_user u join fetch u.accessPoint r where u.userId = :userId")
	UsrUser findOneWithDetail(@Param("userId") Integer userId);

	/**
	 * Return list of usr_permission which grant user rights to manage other
	 * users and groups
	 *
	 * @return
	 */
	@Query("select distinct p.permissionId from usr_permission p " +
	        "left join p.groupControl g " +
	        "left join g.users gu " +
	        "left join p.group g3 " +
	        "left join g3.users gu3 " +
	        "where  (p.userId = :userId or gu3.userId = :userId) " +
	        "       and p.permission in ('USER_CONTROL_ENTITITY' , 'GROUP_CONTROL_ENTITITY') " +
	        "       and (p.userControlId = :checkedUserId OR gu.userId = :checkedUserId)")
	List<Integer> findPermissionAllowingUserAccess(@Param("userId") int userId,
	        @Param("checkedUserId") int checkedUserId);

	/**
	 * Return list of usr_permission which grant user rights to manage (anyhow) the group
	 *
	 * @return
	 */
	@Query("select distinct p.permissionId from usr_permission p " +
	        "left join p.group g3 " +
	        "left join g3.users gu3 " +
	        "where  (p.userId = :userId or gu3.userId = :userId) " +
	        "       and p.permission in ('GROUP_CONTROL_ENTITITY') " +
	        "       and (p.groupControlId = :checkedGroupId)")
	List<Integer> findPermissionAllowingGroupAccess(@Param("userId") int userId,
	        @Param("checkedGroupId") int checkedGroupId);


	@Query("SELECT user" +
			" FROM ap_state s1" +
			" JOIN s1.createChange cc" +
			" JOIN cc.user user" +
			" WHERE s1.accessPoint = :accessPoint" +
			" AND s1.createChangeId = (SELECT min(s2.createChangeId) FROM ap_state s2 WHERE s2.accessPoint = s1.accessPoint)")
	UsrUser findAccessPointOwner(@Param("accessPoint") ApAccessPoint accessPoint);
}
