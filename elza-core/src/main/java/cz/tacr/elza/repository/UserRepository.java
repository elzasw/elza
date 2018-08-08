package cz.tacr.elza.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;

/**
 * Respozitory pro {@link UsrUser}.
 *
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

	/**
	 * Return user with fetched party and record
	 *
	 * @param userId
	 *            User to be fetched from DB
	 * @return
	 */
	@Query("select u from usr_user u join fetch u.party p join fetch p.accessPoint r where u.userId = :userId")
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
}
