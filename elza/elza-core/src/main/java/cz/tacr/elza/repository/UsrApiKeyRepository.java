package cz.tacr.elza.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.UsrApiKey;
import cz.tacr.elza.domain.UsrUser;

@Repository
public interface UsrApiKeyRepository extends JpaRepository<UsrApiKey, Integer> {

    /** Looks the key row up by its public identifier for authentication. */
    Optional<UsrApiKey> findByKeyId(String keyId);

    /** Lists keys of one user for UI, newest first. */
    List<UsrApiKey> findByUserOrderByCreateDateDesc(UsrUser user);

    /** Same, addressed by user id — for the admin overview where {@link UsrUser} is not loaded. */
    List<UsrApiKey> findByUserUserIdOrderByCreateDateDesc(Integer userId);
}
