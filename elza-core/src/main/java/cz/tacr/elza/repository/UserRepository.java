package cz.tacr.elza.repository;

import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrUser;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
