package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.UsrGroup;
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
public interface PermissionRepository extends JpaRepository<UsrPermission, Integer> {

    /**
     * Načtení všech oprávnění uživatele - přímo přiřazených a přiřazených přes skupiny.
     * Donačítává se vazba na fond a scope kvůli jejímu využizí.
     *
     * @param user uživatel
     * @return seznam všech jeho oprávnění
     */
    @Query("SELECT p FROM usr_permission p LEFT JOIN FETCH p.scope s LEFT JOIN FETCH p.fund f WHERE p.user = :user OR p.group.groupId IN (SELECT gu.group.groupId FROM usr_group_user gu WHERE gu.user = :user)")
    List<UsrPermission> getAllPermissions(@Param("user") UsrUser user);

    List<UsrPermission> findByUserOrderByPermissionIdAsc(UsrUser user);

    List<UsrPermission> findByGroupOrderByPermissionIdAsc(UsrGroup group);

    void deleteByGroup(UsrGroup group);

    void deleteByFund(ArrFund fund);
}
