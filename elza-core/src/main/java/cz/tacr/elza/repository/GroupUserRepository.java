package cz.tacr.elza.repository;

import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrGroupUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Respozitory pro {@link UsrGroupUser}.
 *
 * @author Martin Šlapa
 * @since 11.04.2016
 */
@Repository
public interface GroupUserRepository extends JpaRepository<UsrGroupUser, Integer> {

    void deleteByGroup(UsrGroup group);

    List<UsrGroupUser> findByGroup(UsrGroup group);
}
