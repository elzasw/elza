package cz.tacr.elza.repository;

import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrUser;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
