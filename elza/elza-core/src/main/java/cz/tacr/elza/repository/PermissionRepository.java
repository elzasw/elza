package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.WfIssueList;

/**
 * Respozitory pro {@link UsrPermission}.
 *
 * @author Martin Šlapa
 * @since 26.04.2016
 */
@Repository
public interface PermissionRepository extends JpaRepository<UsrPermission, Integer> {

    /**
     * Načtení oprávnění přímo přiřazených uživateli.
     */
    @Query("SELECT p FROM usr_permission p WHERE p.user = :user")
    List<UsrPermission> findByUser(@Param("user") UsrUser user);

    /**
     * Načtení oprávnění přiřazených přes skupiny s donačtením skupiny.
     */
    @Query("SELECT p FROM usr_permission p" +
            " JOIN FETCH p.group g" +
            " WHERE g.groupId IN (SELECT gu.group.groupId FROM usr_group_user gu WHERE gu.user = :user)")
    List<UsrPermission> findByUserGroups(@Param("user") UsrUser user);

    List<UsrPermission> findAllByUserIn(@Param("users") Collection<UsrUser> user);

    List<UsrPermission> findByUserOrderByPermissionIdAsc(UsrUser user);

    List<UsrPermission> findByGroupOrderByPermissionIdAsc(UsrGroup group);

    void deleteByGroup(UsrGroup group);

    void deleteByFund(ArrFund fund);

    void deleteByIssueList(WfIssueList issueList);

    void deleteByIssueListAndPermission(WfIssueList issueList, Permission permission);

    @Query("select p" +
            " from usr_permission p" +
            " where p.issueList.issueListId = :issueListId")
    List<UsrPermission> findByIssueListId(@Param(value = "issueListId") Integer issueListId);

    @Query("select p" +
            " from usr_permission p" +
            " where p.issueList.issueListId = :issueListId" +
            " and p.permission = :permission")
    List<UsrPermission> findByIssueListAndPermission(@Param(value = "issueListId") Integer issueListId, @Param(value = "permission") Permission permission);

    @Query("select p" +
            " from usr_permission p" +
            " where p.nodeId IN :nodeIds")
    List<UsrPermission> findByNodeIds(@Param(value = "nodeIds") List<Integer> nodeIds);
}
