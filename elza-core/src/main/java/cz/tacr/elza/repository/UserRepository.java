package cz.tacr.elza.repository;

import cz.tacr.elza.domain.UsrUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Respozitory pro {@link UsrUser}.
 *
 * @author Martin Šlapa
 * @since 11.04.2016
 */
@Repository
public interface UserRepository extends JpaRepository<UsrUser, Integer> {

    UsrUser findByUsername(String username);

}
