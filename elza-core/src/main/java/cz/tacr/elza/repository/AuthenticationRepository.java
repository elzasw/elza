package cz.tacr.elza.repository;

import cz.tacr.elza.domain.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Respozitory pro {@link UsrAuthentication}.
 *
 */
@Repository
public interface AuthenticationRepository extends ElzaJpaRepository<UsrAuthentication, Integer> {

	List<UsrAuthentication> findByUser(UsrUser user);

	UsrAuthentication findByUserAndAuthType(UsrUser user, UsrAuthentication.AuthType authType);

	List<UsrAuthentication> findByAuthValueAndAuthType(String authValue, UsrAuthentication.AuthType authType);

}
